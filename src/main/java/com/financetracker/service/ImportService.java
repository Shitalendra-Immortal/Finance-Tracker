package com.financetracker.service;

import com.financetracker.dao.TransactionDao;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CSV import: turn raw bank-style rows into transactions, with duplicate
 * detection and validation. The pure resolution logic is static so tests
 * need no database; only the final save touches the DAO.
 */
public class ImportService {

    /** Which CSV column feeds each field. -1 means "not mapped". */
    public static class ColumnMapping {
        public int dateIdx = -1;
        public int amountIdx = -1;
        public int categoryIdx = -1;
        public int noteIdx = -1;
        public int typeIdx = -1;
        public int paymentIdx = -1;
    }

    /** Outcome of resolving rows: what to import, what was skipped, and why. */
    public static class ImportResult {
        public final List<Transaction> toImport = new ArrayList<>();
        public final List<String> duplicates = new ArrayList<>();
        public final List<String> invalid = new ArrayList<>();
    }

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private static final Set<String> HEADER_WORDS = Set.of(
            "date", "txn_date", "transaction date", "value date",
            "amount", "debit", "credit",
            "category", "note", "narration", "description", "remarks",
            "type", "payment", "payment_method", "mode", "balance");

    private final TransactionDao transactionDao;

    public ImportService(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    /** True when a row looks like a header row (known column names). */
    public static boolean isHeaderRow(String[] row) {
        for (String cell : row) {
            if (cell != null && HEADER_WORDS.contains(cell.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /** Guess the column mapping from a header row; unrecognized columns stay -1. */
    public static ColumnMapping autoMapping(String[] headerRow) {
        ColumnMapping mapping = new ColumnMapping();
        for (int i = 0; i < headerRow.length; i++) {
            String header = headerRow[i] == null ? "" : headerRow[i].trim().toLowerCase();
            switch (header) {
                case "date", "txn_date", "transaction date", "value date" -> mapping.dateIdx = i;
                case "amount" -> mapping.amountIdx = i;
                case "category" -> mapping.categoryIdx = i;
                case "note", "narration", "description", "remarks" -> mapping.noteIdx = i;
                case "type" -> mapping.typeIdx = i;
                case "payment", "payment_method", "mode" -> mapping.paymentIdx = i;
                default -> {
                    // debit/credit columns are intentionally left unmapped:
                    // use a signed amount column instead.
                }
            }
        }
        return mapping;
    }

    /** Try the common date formats; null when none fits. */
    public static LocalDate parseDate(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, format);
            } catch (DateTimeParseException ignored) {
                // Try the next format.
            }
        }
        return null;
    }

    /** Strip currency symbols, spaces and thousand separators; null when not a number. */
    public static BigDecimal parseAmount(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim()
                .replace("₹", "").replace("$", "").replace("£", "").replace("€", "")
                .replace(",", "").replace(" ", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Duplicate key: same date, amount, category and note (note normalized). */
    public static String duplicateKey(LocalDate date, BigDecimal amount, int categoryId, String note) {
        String normalizedNote = note == null ? "" : note.trim().toLowerCase();
        return date + "|" + amount.toPlainString() + "|" + categoryId + "|" + normalizedNote;
    }

    public static String duplicateKey(Transaction txn) {
        return duplicateKey(txn.getDate(), txn.getAmount(), txn.getCategoryId(), txn.getNote());
    }

    /**
     * Resolve raw rows against the mapping. Rows are skipped (never silently
     * changed) when the date or amount is unrecognized; rows matching an
     * existing transaction, or an earlier row in the same file, are reported
     * as duplicates. A missing or unknown category falls back to the
     * per-type default category. Negative amounts mean EXPENSE, positive
     * amounts mean INCOME, unless a mapped type column says otherwise.
     */
    public static ImportResult resolveRows(List<String[]> rows, ColumnMapping mapping,
                                           List<Category> categories,
                                           Category defaultExpense, Category defaultIncome,
                                           Set<String> existingKeys) {
        ImportResult result = new ImportResult();
        Set<String> seen = new HashSet<>(existingKeys);
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String label = "Row " + (i + 1);
            LocalDate date = mapping.dateIdx >= 0 && mapping.dateIdx < row.length
                    ? parseDate(row[mapping.dateIdx]) : null;
            if (date == null) {
                result.invalid.add(label + ": date not recognized.");
                continue;
            }
            BigDecimal amount = mapping.amountIdx >= 0 && mapping.amountIdx < row.length
                    ? parseAmount(row[mapping.amountIdx]) : null;
            if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                result.invalid.add(label + ": amount not recognized.");
                continue;
            }
            String type = null;
            if (mapping.typeIdx >= 0 && mapping.typeIdx < row.length) {
                String typeText = row[mapping.typeIdx].trim().toUpperCase();
                if (typeText.contains("INCOME") || typeText.equals("CR") || typeText.equals("CREDIT")) {
                    type = "INCOME";
                } else if (typeText.contains("EXPENSE") || typeText.equals("DR") || typeText.equals("DEBIT")) {
                    type = "EXPENSE";
                }
            }
            if (type == null) {
                type = amount.compareTo(BigDecimal.ZERO) < 0 ? "EXPENSE" : "INCOME";
            }
            BigDecimal absAmount = amount.abs();
            Category category = null;
            if (mapping.categoryIdx >= 0 && mapping.categoryIdx < row.length) {
                String wanted = row[mapping.categoryIdx].trim();
                for (Category candidate : categories) {
                    if (candidate.getType().equals(type)
                            && candidate.getName().equalsIgnoreCase(wanted)) {
                        category = candidate;
                        break;
                    }
                }
            }
            if (category == null) {
                category = "INCOME".equals(type) ? defaultIncome : defaultExpense;
            }
            if (category == null) {
                result.invalid.add(label + ": no category available for " + type + ".");
                continue;
            }
            String note = mapping.noteIdx >= 0 && mapping.noteIdx < row.length
                    ? row[mapping.noteIdx].trim() : "";
            if (note.length() > 255) {
                note = note.substring(0, 255);
            }
            String payment = mapping.paymentIdx >= 0 && mapping.paymentIdx < row.length
                    ? row[mapping.paymentIdx].trim() : "";
            if (payment.isEmpty()) {
                payment = "Cash";
            }
            Transaction txn = new Transaction(type, category.getId(), absAmount, date, note, payment);
            txn.setCategoryName(category.getName());
            String key = duplicateKey(txn);
            if (seen.contains(key)) {
                result.duplicates.add(label + ": same date, amount, category and note already exists.");
                continue;
            }
            seen.add(key);
            result.toImport.add(txn);
        }
        return result;
    }

    /** Save resolved rows in one database transaction: all or nothing. */
    public void importRows(List<Transaction> rows) throws SQLException {
        transactionDao.insertAll(rows);
    }
}
