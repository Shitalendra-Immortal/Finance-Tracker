package com.financetracker.service;

import com.financetracker.model.CategorySummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest {

    @Test
    void weekBoundariesForMonday() {
        LocalDate monday = LocalDate.of(2026, 10, 26);
        assertEquals(LocalDate.of(2026, 10, 26), ReportService.weekStart(monday));
        assertEquals(LocalDate.of(2026, 11, 1), ReportService.weekEnd(monday));
    }

    @Test
    void weekBoundariesForSunday() {
        LocalDate sunday = LocalDate.of(2026, 11, 1);
        assertEquals(LocalDate.of(2026, 10, 26), ReportService.weekStart(sunday));
        assertEquals(LocalDate.of(2026, 11, 1), ReportService.weekEnd(sunday));
    }

    @Test
    void topCategoriesTextShowsTopThreeWithShares() {
        List<CategorySummary> rows = List.of(
                new CategorySummary("Food", new BigDecimal("4200")),
                new CategorySummary("Transport", new BigDecimal("2300")),
                new CategorySummary("Rent", new BigDecimal("1800")),
                new CategorySummary("Other", new BigDecimal("1700")));
        assertEquals("Biggest spending: Food (42%), Transport (23%), Rent (18%)",
                ReportService.topCategoriesText(rows, new BigDecimal("10000")));
    }

    @Test
    void topCategoriesTextHandlesEmptyPeriod() {
        assertEquals("No expenses in this period.",
                ReportService.topCategoriesText(List.of(), BigDecimal.ZERO));
    }

    @Test
    void surgeAtExactlyFiftyPercentAndFiveHundred() {
        assertTrue(ReportService.isSurge(new BigDecimal("1500"), new BigDecimal("1000")));
    }

    @Test
    void belowRatioOrBelowMinimumRiseIsNotASurge() {
        // 1499 is under 1.5x of 1000.
        assertFalse(ReportService.isSurge(new BigDecimal("1499"), new BigDecimal("1000")));
        // 3x the average, but the rise is only 400 — too small to flag.
        assertFalse(ReportService.isSurge(new BigDecimal("600"), new BigDecimal("200")));
        // No baseline (new category) never surges.
        assertFalse(ReportService.isSurge(new BigDecimal("5000"), BigDecimal.ZERO));
    }

    @Test
    void surgeMessageShowsPercentAndAmounts() {
        assertEquals("Food spending is up 90% vs your recent average (₹1,900.00 vs ₹1,000.00 avg)",
                ReportService.surgeMessage("Food", new BigDecimal("1900"), new BigDecimal("1000")));
    }
}
