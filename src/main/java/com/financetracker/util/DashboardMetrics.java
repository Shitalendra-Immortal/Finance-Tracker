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

    /** What a delta line describes: expenses read differently from income — spending
     *  less is good news, earning less is not. */
    public enum DeltaKind { INCOME, EXPENSE, NET }

    /** How a delta line should be colored: good, bad, or neither. */
    public enum DeltaTone { GOOD, BAD, NEUTRAL }

    /** One month-over-month indicator line: its text and its tone. */
    public record DeltaLine(String text, DeltaTone tone) {
    }

    /**
     * The text and tone for one stat card's month-over-month line.
     * currentEmpty means the current month has no activity for this metric
     * (for net cash flow: neither income nor expenses) — it reads a plain
     * empty-state line instead of a misleading −100%, worded per card:
     * "No activity yet" for net, "No income recorded" for income,
     * "No expenses recorded" for expenses. A previous month of zero reads
     * "New activity" instead of dividing by zero. Only when both months have
     * data does a percentage appear, and expense lines say whether spending
     * rose or fell, since that meaning differs from an income change.
     */
    public static DeltaLine deltaLine(BigDecimal current, BigDecimal previous,
            boolean currentEmpty, DeltaKind kind) {
        if (currentEmpty) {
            String emptyText = switch (kind) {
                case INCOME -> "No income recorded";
                case EXPENSE -> "No expenses recorded";
                case NET -> "No activity yet";
            };
            return new DeltaLine(emptyText, DeltaTone.NEUTRAL);
        }
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return new DeltaLine("New activity", DeltaTone.NEUTRAL);
        }
        BigDecimal pct = monthOverMonthChange(current, previous).orElse(BigDecimal.ZERO);
        boolean up = pct.compareTo(BigDecimal.ZERO) > 0;
        boolean flat = pct.compareTo(BigDecimal.ZERO) == 0;
        String arrow = flat ? "•" : (up ? "▲" : "▼");
        String text = arrow + " " + (up ? "+" : "") + pct.toPlainString() + "% vs last month";
        if (kind == DeltaKind.EXPENSE && !flat) {
            text += up ? " (higher spending)" : " (lower spending)";
        }
        boolean upIsGood = kind != DeltaKind.EXPENSE;
        DeltaTone tone = flat ? DeltaTone.NEUTRAL : (up == upIsGood ? DeltaTone.GOOD : DeltaTone.BAD);
        return new DeltaLine(text, tone);
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
