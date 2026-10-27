package com.financetracker.service;

import com.financetracker.dao.BudgetDao;
import com.financetracker.model.Category;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetServiceTest {

    private static final BigDecimal LIMIT = new BigDecimal("10000");

    @Test
    void belowEightyPercentIsOk() {
        assertEquals("OK", BudgetService.statusFor(LIMIT, new BigDecimal("7999")));
    }

    @Test
    void eightyPercentIsWarning() {
        assertEquals("WARNING", BudgetService.statusFor(LIMIT, new BigDecimal("8000")));
    }

    @Test
    void justBelowLimitIsStillWarning() {
        assertEquals("WARNING", BudgetService.statusFor(LIMIT, new BigDecimal("9999")));
    }

    @Test
    void atLimitIsOverBudget() {
        assertEquals("OVER BUDGET", BudgetService.statusFor(LIMIT, new BigDecimal("10000")));
    }

    @Test
    void aboveLimitIsOverBudget() {
        assertEquals("OVER BUDGET", BudgetService.statusFor(LIMIT, new BigDecimal("15000")));
    }

    @Test
    void validateRejectsBadMonths() {
        assertThrows(IllegalArgumentException.class, () ->
                BudgetService.validateBudget("Oct-2026", new BigDecimal("100")));
        assertThrows(IllegalArgumentException.class, () ->
                BudgetService.validateBudget("2026-13", new BigDecimal("100")));
    }

    @Test
    void validateRejectsZeroBudget() {
        assertThrows(IllegalArgumentException.class, () ->
                BudgetService.validateBudget("2026-10", BigDecimal.ZERO));
    }

    @Test
    void validateAcceptsGoodBudget() {
        BudgetService.validateBudget("2026-10", new BigDecimal("5000"));
    }

    @Test
    void deleteRejectsMissingCategoryAndBadMonth() throws Exception {
        BudgetService service = new BudgetService(new BudgetDao());
        assertThrows(IllegalArgumentException.class, () ->
                service.deleteBudget(null, "2026-11"));
        Category food = new Category(1, "Food", "EXPENSE");
        assertThrows(IllegalArgumentException.class, () ->
                service.deleteBudget(food, "2026-13"));
    }
}
