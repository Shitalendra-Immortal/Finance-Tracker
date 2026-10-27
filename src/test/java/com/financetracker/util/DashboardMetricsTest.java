package com.financetracker.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardMetricsTest {

    @Test
    void monthOverMonthRise() {
        assertEquals(Optional.of(new BigDecimal("12.5")),
                DashboardMetrics.monthOverMonthChange(new BigDecimal("1125"), new BigDecimal("1000")));
    }

    @Test
    void monthOverMonthFall() {
        assertEquals(Optional.of(new BigDecimal("-10.0")),
                DashboardMetrics.monthOverMonthChange(new BigDecimal("900"), new BigDecimal("1000")));
    }

    @Test
    void monthOverMonthUndefinedWhenPreviousIsZero() {
        assertTrue(DashboardMetrics.monthOverMonthChange(new BigDecimal("500"), BigDecimal.ZERO).isEmpty());
    }

    @Test
    void savingsRateIsNetOverIncome() {
        assertEquals(Optional.of(new BigDecimal("10.0")),
                DashboardMetrics.savingsRate(new BigDecimal("1000"), new BigDecimal("100")));
    }

    @Test
    void savingsRateUndefinedWhenIncomeIsZero() {
        assertTrue(DashboardMetrics.savingsRate(BigDecimal.ZERO, BigDecimal.ZERO).isEmpty());
    }

    @Test
    void greetingFollowsTimeOfDay() {
        assertEquals("Good morning", DashboardMetrics.greetingForHour(8));
        assertEquals("Good afternoon", DashboardMetrics.greetingForHour(13));
        assertEquals("Good evening", DashboardMetrics.greetingForHour(19));
        assertEquals("Good night", DashboardMetrics.greetingForHour(23));
        assertEquals("Good night", DashboardMetrics.greetingForHour(3));
    }
}
