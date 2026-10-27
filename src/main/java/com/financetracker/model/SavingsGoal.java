package com.financetracker.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * A savings goal: a target amount by a deadline, with contributions tracked
 * separately and summed into savedAmount by the DAO.
 * getProgress() and getMonthlyNeeded() are pure derivations from this goal's
 * own fields (no database access), so the table can bind to them directly.
 */
public class SavingsGoal {
    private int id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal savedAmount;
    private LocalDate deadline;

    public SavingsGoal() {
    }

    public SavingsGoal(String name, BigDecimal targetAmount, LocalDate deadline) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.deadline = deadline;
    }

    public SavingsGoal(int id, String name, BigDecimal targetAmount, BigDecimal savedAmount,
                       LocalDate deadline) {
        this.id = id;
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
        this.deadline = deadline;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }
    public BigDecimal getSavedAmount() { return savedAmount; }
    public void setSavedAmount(BigDecimal savedAmount) { this.savedAmount = savedAmount; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    /** Money still needed to reach the target. */
    public BigDecimal getRemaining() { return targetAmount.subtract(savedAmount); }

    /** 0.0 to 1.0, for the progress bar. */
    public double getProgress() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        double progress = savedAmount.divide(targetAmount, 4, RoundingMode.HALF_UP).doubleValue();
        return Math.min(1.0, Math.max(0.0, progress));
    }

    /**
     * Even monthly amount needed from now until the deadline.
     * If the deadline is this month or already passed, the whole remainder is due.
     */
    public BigDecimal getMonthlyNeeded() {
        BigDecimal remaining = getRemaining();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long months = ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(deadline));
        if (months <= 0) {
            return remaining.setScale(2, RoundingMode.HALF_UP);
        }
        return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }
}
