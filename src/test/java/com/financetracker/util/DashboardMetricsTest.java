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
    void deltaLineEmptyCurrentMonthShowsNoActivityYet() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                BigDecimal.ZERO, new BigDecimal("1000"), true, DashboardMetrics.DeltaKind.NET);
        assertEquals("No activity yet", line.text());
        assertEquals(DashboardMetrics.DeltaTone.NEUTRAL, line.tone());
    }

    @Test
    void deltaLineEmptyMonthWordedPerCard() {
        assertEquals("No income recorded", DashboardMetrics.deltaLine(
                BigDecimal.ZERO, new BigDecimal("1000"), true, DashboardMetrics.DeltaKind.INCOME).text());
        assertEquals("No expenses recorded", DashboardMetrics.deltaLine(
                BigDecimal.ZERO, new BigDecimal("1000"), true, DashboardMetrics.DeltaKind.EXPENSE).text());
    }

    @Test
    void deltaLineEmptyCurrentMonthBeatsZeroPreviousMonth() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                BigDecimal.ZERO, BigDecimal.ZERO, true, DashboardMetrics.DeltaKind.EXPENSE);
        assertEquals("No expenses recorded", line.text());
    }

    @Test
    void deltaLineZeroPreviousMonthShowsNewActivity() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                new BigDecimal("500"), BigDecimal.ZERO, false, DashboardMetrics.DeltaKind.INCOME);
        assertEquals("New activity", line.text());
        assertEquals(DashboardMetrics.DeltaTone.NEUTRAL, line.tone());
    }

    @Test
    void deltaLineIncomeRiseIsGood() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                new BigDecimal("1125"), new BigDecimal("1000"), false, DashboardMetrics.DeltaKind.INCOME);
        assertEquals("▲ +12.5% vs last month", line.text());
        assertEquals(DashboardMetrics.DeltaTone.GOOD, line.tone());
    }

    @Test
    void deltaLineIncomeFallIsBad() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                new BigDecimal("900"), new BigDecimal("1000"), false, DashboardMetrics.DeltaKind.INCOME);
        assertEquals("▼ -10.0% vs last month", line.text());
        assertEquals(DashboardMetrics.DeltaTone.BAD, line.tone());
    }

    @Test
    void deltaLineExpenseFallIsGoodAndSaysSo() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                new BigDecimal("800"), new BigDecimal("1000"), false, DashboardMetrics.DeltaKind.EXPENSE);
        assertEquals("▼ -20.0% vs last month (lower spending)", line.text());
        assertEquals(DashboardMetrics.DeltaTone.GOOD, line.tone());
    }

    @Test
    void deltaLineExpenseRiseIsBadAndSaysSo() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                new BigDecimal("1200"), new BigDecimal("1000"), false, DashboardMetrics.DeltaKind.EXPENSE);
        assertEquals("▲ +20.0% vs last month (higher spending)", line.text());
        assertEquals(DashboardMetrics.DeltaTone.BAD, line.tone());
    }

    @Test
    void deltaLineFlatIsNeutral() {
        DashboardMetrics.DeltaLine line = DashboardMetrics.deltaLine(
                new BigDecimal("1000"), new BigDecimal("1000"), false, DashboardMetrics.DeltaKind.NET);
        assertEquals("• 0.0% vs last month", line.text());
        assertEquals(DashboardMetrics.DeltaTone.NEUTRAL, line.tone());
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
