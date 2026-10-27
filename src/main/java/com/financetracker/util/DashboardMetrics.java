package com.financetracker.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Pure dashboard math: month-over-month change, savings rate, and the
 * time-of-day greeting. No database access, so all of it is unit-testable.
 * A percentage is undefined (empty) whenever its denominator is zero —
 * callers show "—" instead of inventing a number.
 */
public final class DashboardMetrics {

    private DashboardMetrics() {
    }

    /** Percent change from previous to current, one decimal. Empty when previous is zero. */
    public static Optional<BigDecimal> monthOverMonthChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal change = current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
        return Optional.of(change);
    }

    /** Savings rate = net cash flow as a percent of income, one decimal. Empty when income is zero. */
    public static Optional<BigDecimal> savingsRate(BigDecimal income, BigDecimal net) {
        if (income == null || income.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        return Optional.of(net.multiply(BigDecimal.valueOf(100))
                .divide(income, 1, RoundingMode.HALF_UP));
    }

    /** Time-of-day greeting for a 24-hour clock value (0-23). No name is ever invented. */
    public static String greetingForHour(int hour) {
        if (hour >= 5 && hour < 12) {
            return "Good morning";
        }
        if (hour >= 12 && hour < 17) {
            return "Good afternoon";
        }
        if (hour >= 17 && hour < 22) {
            return "Good evening";
        }
        return "Good night";
    }
}
