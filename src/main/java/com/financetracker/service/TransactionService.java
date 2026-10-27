package com.financetracker.service;

import com.financetracker.dao.CategoryDao;
import com.financetracker.dao.TransactionDao;
import com.financetracker.model.Category;
import com.financetracker.model.Transaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Transaction business rules + validation. The controller talks to this, never
 * to the DAOs directly. Pure rules are static so unit tests need no database.
 */
public class TransactionService {

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;

    public TransactionService(TransactionDao transactionDao, CategoryDao categoryDao) {
        this.transactionDao = transactionDao;
        this.categoryDao = categoryDao;
    }

    /** Rejects bad input with a plain message. Throws IllegalArgumentException. */
    public static void validate(String type, Category category, BigDecimal amount,
                                LocalDate date, String note) {
        if (!"INCOME".equals(type) && !"EXPENSE".equals(type)) {
            throw new IllegalArgumentException("Transaction type must be INCOME or EXPENSE.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Pick a category.");
        }
        if (!category.getType().equals(type)) {
            throw new IllegalArgumentException("That category doesn't fit this transaction type.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Pick a date.");
        }
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("Note is too long (max 255 characters).");
        }
    }

    /**
     * Balance from a list of transactions, always recomputed from the rows.
     * Never adjusted incrementally — that is how an edit could double-count.
     * Example: income 10000, expense 2500 → 7500; edit the expense to 3000
     * and the same recompute gives 7000, not 4500.
     */
    public static BigDecimal balance(List<Transaction> transactions) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction txn : transactions) {
            if ("INCOME".equals(txn.getType())) {
                income = income.add(txn.getAmount());
            } else {
                expense = expense.add(txn.getAmount());
            }
        }
        return income.subtract(expense);
    }

    public void addTransaction(String type, Category category, BigDecimal amount,
                               LocalDate date, String note, String payment) throws SQLException {
        validate(type, category, amount, date, note);
        transactionDao.insert(new Transaction(type, category.getId(), amount, date, note,
                payment == null || payment.isBlank() ? "Cash" : payment));
    }

    public void updateTransaction(int id, String type, Category category, BigDecimal amount,
                                  LocalDate date, String note, String payment) throws SQLException {
        validate(type, category, amount, date, note);
        transactionDao.update(new Transaction(id, type, category.getId(), category.getName(),
                amount, date, note, payment == null || payment.isBlank() ? "Cash" : payment));
    }

    public void deleteTransaction(int id) throws SQLException {
        transactionDao.deleteById(id);
    }

    public List<Transaction> findAll() throws SQLException {
        return transactionDao.findAll();
    }

    public List<Transaction> search(String keyword) throws SQLException {
        return transactionDao.search(keyword);
    }

    public List<Transaction> findFiltered(String type, int categoryId, String month) throws SQLException {
        return transactionDao.findFiltered(type, categoryId, month);
    }

    public List<Transaction> findByMonth(String month) throws SQLException {
        return transactionDao.findByMonth(month);
    }

    public BigDecimal sumByTypeAndMonth(String type, String month) throws SQLException {
        return transactionDao.sumByTypeAndMonth(type, month);
    }

    public List<Category> findCategoriesByType(String type) throws SQLException {
        return categoryDao.findByType(type);
    }

    public List<Category> findAllCategories() throws SQLException {
        return categoryDao.findAll();
    }
}
