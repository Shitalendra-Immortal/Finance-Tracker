package com.financetracker.service;

import com.financetracker.model.Category;
import com.financetracker.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionServiceTest {

    private static Transaction txn(String type, String amount) {
        return new Transaction(1, type, 1, "Category", new BigDecimal(amount),
                LocalDate.of(2026, 10, 1), "", "Cash");
    }

    @Test
    void balanceExampleFromSpec() {
        // Income 10000, expenses 2500 → balance 7500.
        List<Transaction> before = List.of(txn("INCOME", "10000"), txn("EXPENSE", "2500"));
        assertEquals(new BigDecimal("7500"), TransactionService.balance(before));
    }

    @Test
    void editingExpenseRecalculatesWithoutDoubleCounting() {
        // The same expense edited from 2500 to 3000 must give 7000, not 4500.
        // Balance is always recomputed from the rows, never adjusted incrementally.
        List<Transaction> afterEdit = List.of(txn("INCOME", "10000"), txn("EXPENSE", "3000"));
        assertEquals(new BigDecimal("7000"), TransactionService.balance(afterEdit));
    }

    @Test
    void balanceOfEmptyListIsZero() {
        assertEquals(BigDecimal.ZERO, TransactionService.balance(List.of()));
    }

    @Test
    void validateRejectsNegativeAndZeroAmounts() {
        Category food = new Category(1, "Food", "EXPENSE");
        assertThrows(IllegalArgumentException.class, () ->
                TransactionService.validate("EXPENSE", food, new BigDecimal("-5"), LocalDate.now(), ""));
        assertThrows(IllegalArgumentException.class, () ->
                TransactionService.validate("EXPENSE", food, BigDecimal.ZERO, LocalDate.now(), ""));
    }

    @Test
    void validateRejectsMissingCategoryAndDate() {
        Category food = new Category(1, "Food", "EXPENSE");
        assertThrows(IllegalArgumentException.class, () ->
                TransactionService.validate("EXPENSE", null, new BigDecimal("10"), LocalDate.now(), ""));
        assertThrows(IllegalArgumentException.class, () ->
                TransactionService.validate("EXPENSE", food, new BigDecimal("10"), null, ""));
    }

    @Test
    void validateRejectsCategoryOfWrongType() {
        Category food = new Category(1, "Food", "EXPENSE");
        assertThrows(IllegalArgumentException.class, () ->
                TransactionService.validate("INCOME", food, new BigDecimal("10"), LocalDate.now(), ""));
    }

    @Test
    void validateRejectsOverlongNote() {
        Category food = new Category(1, "Food", "EXPENSE");
        String note = "x".repeat(256);
        assertThrows(IllegalArgumentException.class, () ->
                TransactionService.validate("EXPENSE", food, new BigDecimal("10"), LocalDate.now(), note));
    }

    @Test
    void validateAcceptsGoodTransaction() {
        Category food = new Category(1, "Food", "EXPENSE");
        TransactionService.validate("EXPENSE", food, new BigDecimal("250.50"), LocalDate.now(), "Lunch");
    }
}
