package com.financetracker.service;

import com.financetracker.dao.BudgetDao;
import com.financetracker.model.Budget;
import com.financetracker.model.BudgetStatus;
import com.financetracker.model.Category;
import com.financetracker.util.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Budget rules: status thresholds, alerts, validation, carry-forward. */
public class BudgetService {

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.8");

    private final BudgetDao budgetDao;

    public BudgetService(BudgetDao budgetDao) {
        this.budgetDao = budgetDao;
    }

    /** The status rule, kept here (not in the UI) so tests can pin it down. */
    public static String statusFor(BigDecimal limit, BigDecimal spent) {
        if (spent.compareTo(limit) >= 0) {
            return "OVER BUDGET";
        }
        if (spent.compareTo(limit.multiply(WARNING_THRESHOLD)) >= 0) {
            return "WARNING";
        }
        return "OK";
    }

    public static void validateMonth(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("Enter the month as YYYY-MM.");
        }
        try {
            YearMonth.parse(month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Enter a real month as YYYY-MM.");
        }
    }

    public static void validateBudget(String month, BigDecimal amount) {
        validateMonth(month);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget amount must be greater than zero.");
        }
    }

    /** One status row per budget of the month, with real spending attached. */
    public List<BudgetStatus> getStatuses(String month) throws SQLException {
        List<BudgetStatus> rows = new ArrayList<>();
        for (Budget budget : budgetDao.findByMonth(month)) {
            BigDecimal spent = budgetDao.spentForCategoryAndMonth(budget.getCategoryId(), month);
            rows.add(new BudgetStatus(budget.getCategoryName(), budget.getAmount(), spent,
                    statusFor(budget.getAmount(), spent)));
        }
        return rows;
    }

    /** Dashboard alert lines for over-budget and nearly-over categories. */
    public List<String> getAlerts(String month) throws SQLException {
        List<String> warnings = new ArrayList<>();
        for (BudgetStatus status : getStatuses(month)) {
            if ("OVER BUDGET".equals(status.getStatus())) {
                warnings.add(status.getCategoryName() + " is OVER budget ("
                        + Money.format(status.getSpent()) + " / " + Money.format(status.getLimit()) + ")");
            } else if ("WARNING".equals(status.getStatus())) {
                warnings.add(status.getCategoryName() + " is at "
                        + percentUsed(status) + "% of its budget");
            }
        }
        return warnings;
    }

    private long percentUsed(BudgetStatus status) {
        if (status.getLimit().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return status.getSpent().multiply(BigDecimal.valueOf(100))
                .divide(status.getLimit(), 0, RoundingMode.HALF_UP).longValue();
    }

    /** Warning for one category right after an expense is added, if any applies. */
    public Optional<String> checkAfterAdd(int categoryId, String month) throws SQLException {
        for (Budget budget : budgetDao.findByMonth(month)) {
            if (budget.getCategoryId() != categoryId) {
                continue;
            }
            BigDecimal spent = budgetDao.spentForCategoryAndMonth(categoryId, month);
            String status = statusFor(budget.getAmount(), spent);
            if ("OVER BUDGET".equals(status)) {
                return Optional.of("Budget alert: " + budget.getCategoryName() + " is now OVER budget for "
                        + month + " (" + Money.format(spent) + " / " + Money.format(budget.getAmount()) + ").");
            }
            if ("WARNING".equals(status)) {
                return Optional.of("Budget alert: " + budget.getCategoryName() + " has used "
                        + spent.multiply(BigDecimal.valueOf(100))
                                .divide(budget.getAmount(), 0, RoundingMode.HALF_UP)
                        + "% of its " + month + " budget.");
            }
        }
        return Optional.empty();
    }

    public void setBudget(Category category, String month, BigDecimal amount) throws SQLException {
        if (category == null) {
            throw new IllegalArgumentException("Pick a category.");
        }
        validateBudget(month, amount);
        budgetDao.upsert(new Budget(category.getId(), month, amount));
    }

    public void deleteBudget(Category category, String month) throws SQLException {
        if (category == null) {
            throw new IllegalArgumentException("Pick a category.");
        }
        validateMonth(month);
        budgetDao.deleteByCategoryAndMonth(category.getId(), month);
    }

    /** Copies budgets to next month. Only ever called from the copy button. */
    public void copyToNextMonth(String month) throws SQLException {
        validateMonth(month);
        budgetDao.copyBudgetsToNextMonth(month);
    }
}
