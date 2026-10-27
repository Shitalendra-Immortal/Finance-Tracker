package com.financetracker.service;

import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportServiceTest {

    private static final Category FOOD = new Category(1, "Food", "EXPENSE");
    private static final Category SALARY = new Category(2, "Salary", "INCOME");
    private static final Category OTHER = new Category(3, "Other", "EXPENSE");
    private static final Category OTHER_INCOME = new Category(4, "Other Income", "INCOME");
    private static final List<Category> CATEGORIES = List.of(FOOD, SALARY, OTHER, OTHER_INCOME);

    private static ImportService.ColumnMapping mapping() {
        ImportService.ColumnMapping mapping = new ImportService.ColumnMapping();
        mapping.dateIdx = 0;
        mapping.amountIdx = 1;
        mapping.categoryIdx = 2;
        mapping.noteIdx = 3;
        return mapping;
    }

    @Test
    void negativeAmountBecomesExpenseAndPositiveBecomesIncome() {
        ImportService.ImportResult result = ImportService.resolveRows(
                List.<String[]>of(new String[]{"2026-10-05", "-250.50", "Food", "Lunch"},
                        new String[]{"2026-10-06", "5000", "Salary", "Pay"}),
                mapping(), CATEGORIES, OTHER, OTHER_INCOME, Set.of());
        assertEquals(2, result.toImport.size());
        assertEquals("EXPENSE", result.toImport.get(0).getType());
        assertEquals(new BigDecimal("250.50"), result.toImport.get(0).getAmount());
        assertEquals("INCOME", result.toImport.get(1).getType());
    }

    @Test
    void duplicateAgainstExistingDatabaseIsSkipped() {
        Transaction existing = new Transaction(9, "EXPENSE", 1, "Food",
                new BigDecimal("250.50"), LocalDate.of(2026, 10, 5), "Lunch", "Cash");
        ImportService.ImportResult result = ImportService.resolveRows(
                List.<String[]>of(new String[]{"2026-10-05", "-250.50", "Food", "Lunch"}),
                mapping(), CATEGORIES, OTHER, OTHER_INCOME,
                Set.of(ImportService.duplicateKey(existing)));
        assertTrue(result.toImport.isEmpty());
        assertEquals(1, result.duplicates.size());
    }

    @Test
    void duplicateWithinTheSameFileIsSkipped() {
        ImportService.ImportResult result = ImportService.resolveRows(
                List.<String[]>of(new String[]{"2026-10-05", "-250.50", "Food", "Lunch"},
                        new String[]{"2026-10-05", "-250.50", "Food", "Lunch"}),
                mapping(), CATEGORIES, OTHER, OTHER_INCOME, Set.of());
        assertEquals(1, result.toImport.size());
        assertEquals(1, result.duplicates.size());
    }

    @Test
    void invalidRowsAreListedNotImported() {
        ImportService.ImportResult result = ImportService.resolveRows(
                List.<String[]>of(new String[]{"not-a-date", "-250.50", "Food", "Lunch"},
                        new String[]{"2026-10-05", "abc", "Food", "Lunch"},
                        new String[]{"2026-10-05", "0", "Food", "Free sample"}),
                mapping(), CATEGORIES, OTHER, OTHER_INCOME, Set.of());
        assertTrue(result.toImport.isEmpty());
        assertEquals(3, result.invalid.size());
    }

    @Test
    void unknownCategoryFallsBackToPerTypeDefault() {
        ImportService.ImportResult result = ImportService.resolveRows(
                List.<String[]>of(new String[]{"2026-10-05", "-99", "SomethingNew", "Misc"}),
                mapping(), CATEGORIES, OTHER, OTHER_INCOME, Set.of());
        assertEquals(1, result.toImport.size());
        assertEquals(OTHER.getId(), result.toImport.get(0).getCategoryId());
    }

    @Test
    void categoryMatchIsCaseInsensitive() {
        ImportService.ImportResult result = ImportService.resolveRows(
                List.<String[]>of(new String[]{"2026-10-05", "-99", "food", "Lunch"}),
                mapping(), CATEGORIES, OTHER, OTHER_INCOME, Set.of());
        assertEquals(FOOD.getId(), result.toImport.get(0).getCategoryId());
    }

    @Test
    void typeColumnBeatsTheSign() {
        ImportService.ColumnMapping withType = mapping();
        withType.typeIdx = 4;
        ImportService.ImportResult result = ImportService.resolveRows(
                List.<String[]>of(new String[]{"2026-10-05", "-500", "Salary", "Refund", "CR"}),
                withType, CATEGORIES, OTHER, OTHER_INCOME, Set.of());
        assertEquals("INCOME", result.toImport.get(0).getType());
        assertEquals(new BigDecimal("500.00"), result.toImport.get(0).getAmount());
    }

    @Test
    void amountsWithCurrencySymbolsAndCommasParse() {
        assertEquals(new BigDecimal("1250.50"), ImportService.parseAmount("₹1,250.50"));
        assertEquals(new BigDecimal("1250.50"), ImportService.parseAmount("$1,250.50"));
    }

    @Test
    void datesInCommonFormatsParse() {
        assertEquals(LocalDate.of(2026, 10, 5), ImportService.parseDate("2026-10-05"));
        assertEquals(LocalDate.of(2026, 10, 5), ImportService.parseDate("05/10/2026"));
        assertEquals(null, ImportService.parseDate("yesterday"));
    }

    @Test
    void headerRowIsDetectedAndMapped() {
        String[] header = {"Date", "Amount", "Category", "Narration"};
        assertTrue(ImportService.isHeaderRow(header));
        ImportService.ColumnMapping mapping = ImportService.autoMapping(header);
        assertEquals(0, mapping.dateIdx);
        assertEquals(1, mapping.amountIdx);
        assertEquals(2, mapping.categoryIdx);
        assertEquals(3, mapping.noteIdx);
    }
}
