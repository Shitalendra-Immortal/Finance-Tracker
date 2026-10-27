package com.financetracker.service;

import com.financetracker.dao.ReportDao;
import com.financetracker.model.CategorySummary;
import com.financetracker.model.MonthTotal;
import com.financetracker.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Report rules: period boundaries, rankings, comparison data. */
public class ReportService {

    private final ReportDao reportDao;

    public ReportService(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    /** Monday of the week containing the given date. */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    /** Sunday of the week containing the given date. */
    public static LocalDate weekEnd(LocalDate date) {
        return date.with(DayOfWeek.SUNDAY);
    }

    /** "Biggest spending: Food (42%), Transport (23%), Rent (18%)" or a no-data line. */
    public static String topCategoriesText(List<CategorySummary> rows, BigDecimal total) {
        if (rows.isEmpty() || total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return "No expenses in this period.";
        }
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < Math.min(3, rows.size()); i++) {
            CategorySummary row = rows.get(i);
            long share = row.getTotal().multiply(BigDecimal.valueOf(100))
                    .divide(total, 0, RoundingMode.HALF_UP).longValue();
            parts.add(row.getCategoryName() + " (" + share + "%)");
        }
        return "Biggest spending: " + String.join(", ", parts);
    }

    /**
     * A spending surge: this month's spend in a category is at least 50%
     * above its average over the previous 3 months, and the rise is at
     * least 500. The minimum rise keeps tiny categories from flagging on
     * small absolute changes; a category with no previous spending has no
     * baseline and never surges.
     */
    public static final BigDecimal SURGE_RATIO = new BigDecimal("1.5");
    public static final BigDecimal MIN_RISE = new BigDecimal("500");

    public static boolean isSurge(BigDecimal current, BigDecimal average) {
        if (current == null || average == null || average.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return current.compareTo(average.multiply(SURGE_RATIO)) >= 0
                && current.subtract(average).compareTo(MIN_RISE) >= 0;
    }

    public static String surgeMessage(String category, BigDecimal current, BigDecimal average) {
        long percent = current.subtract(average).multiply(BigDecimal.valueOf(100))
                .divide(average, 0, RoundingMode.HALF_UP).longValue();
        return category + " spending is up " + percent + "% vs your recent average ("
                + Money.format(current) + " vs " + Money.format(average) + " avg)";
    }

    /**
     * Surge alerts for one month. Note the comparison is month-to-date vs
     * full previous months, so alerts are conservative early in a month:
     * if spending already surges with the month unfinished, the full month
     * will only be higher.
     */
    public List<String> spendingSurgeAlerts(String month) throws java.sql.SQLException {
        YearMonth current = YearMonth.parse(month);
        Map<String, BigDecimal> currentSpend = new HashMap<>();
        for (CategorySummary row : reportDao.expenseByCategory(month)) {
            currentSpend.put(row.getCategoryName(), row.getTotal());
        }
        Map<String, BigDecimal> previousSum = new HashMap<>();
        for (int i = 1; i <= 3; i++) {
            String previousMonth = current.minusMonths(i).toString();
            for (CategorySummary row : reportDao.expenseByCategory(previousMonth)) {
                previousSum.merge(row.getCategoryName(), row.getTotal(), BigDecimal::add);
            }
        }
        List<String> alerts = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : currentSpend.entrySet()) {
            BigDecimal average = previousSum.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            if (isSurge(entry.getValue(), average)) {
                alerts.add(surgeMessage(entry.getKey(), entry.getValue(), average));
            }
        }
        return alerts;
    }

    public List<CategorySummary> monthlyReport(String month) throws java.sql.SQLException {
        return reportDao.expenseByCategory(month);
    }

    public List<CategorySummary> yearlyReport(String year) throws java.sql.SQLException {
        return reportDao.expenseByCategoryForYear(year);
    }

    public List<CategorySummary> weeklyReport(LocalDate weekStart, LocalDate weekEnd)
            throws java.sql.SQLException {
        return reportDao.expenseByCategoryForWeek(weekStart, weekEnd);
    }

    public List<MonthTotal> monthlyTotals(List<String> months) throws java.sql.SQLException {
        return reportDao.monthlyTotals(months);
    }
}
