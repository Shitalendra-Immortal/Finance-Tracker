package com.financetracker.service;

import com.financetracker.model.SavingsGoal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoalServiceTest {

    private static SavingsGoal goal(String target, String saved, LocalDate deadline) {
        return new SavingsGoal(1, "Laptop", new BigDecimal(target), new BigDecimal(saved), deadline);
    }

    @Test
    void monthlyNeededSpreadsRemainderEvenly() {
        // 60000 target, 10000 saved, 10 months left → 5000 per month.
        SavingsGoal g = goal("60000", "10000", LocalDate.now().plusMonths(10));
        assertEquals(new BigDecimal("5000.00"), g.getMonthlyNeeded());
    }

    @Test
    void monthlyNeededIsFullRemainderWhenDeadlineIsThisMonth() {
        SavingsGoal g = goal("60000", "10000", LocalDate.now());
        assertEquals(new BigDecimal("50000.00"), g.getMonthlyNeeded());
    }

    @Test
    void monthlyNeededIsFullRemainderWhenDeadlinePassed() {
        SavingsGoal g = goal("60000", "10000", LocalDate.now().minusMonths(1));
        assertEquals(new BigDecimal("50000.00"), g.getMonthlyNeeded());
    }

    @Test
    void monthlyNeededIsZeroWhenTargetReached() {
        SavingsGoal g = goal("60000", "60000", LocalDate.now().plusMonths(10));
        assertEquals(new BigDecimal("0.00"), g.getMonthlyNeeded());
    }

    @Test
    void progressIsCappedAtOne() {
        assertEquals(0.0, goal("60000", "0", LocalDate.now().plusYears(1)).getProgress());
        assertEquals(0.5, goal("60000", "30000", LocalDate.now().plusYears(1)).getProgress());
        assertEquals(1.0, goal("60000", "90000", LocalDate.now().plusYears(1)).getProgress());
    }

    @Test
    void validateRejectsBadGoals() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalService.validateGoal("", new BigDecimal("100"), LocalDate.now().plusMonths(1)));
        assertThrows(IllegalArgumentException.class, () ->
                GoalService.validateGoal("Laptop", BigDecimal.ZERO, LocalDate.now().plusMonths(1)));
        assertThrows(IllegalArgumentException.class, () ->
                GoalService.validateGoal("Laptop", new BigDecimal("100"), LocalDate.now().minusDays(1)));
        assertThrows(IllegalArgumentException.class, () ->
                GoalService.validateGoal("Laptop", new BigDecimal("100"), null));
    }

    @Test
    void validateRejectsBadContributions() {
        assertThrows(IllegalArgumentException.class, () ->
                GoalService.validateContribution(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                GoalService.validateContribution(new BigDecimal("-50")));
    }
}
