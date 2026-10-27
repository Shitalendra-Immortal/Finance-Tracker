package com.financetracker.model;

import java.math.BigDecimal;

/**
 * One row of the budgets table: limit vs actually spent, plus a status flag.
 * The status rule (WARNING at 80%, OVER BUDGET at 100%) lives in BudgetService,
 * which builds these rows — this class only carries the result.
 */
public class BudgetStatus {
    private String categoryName;
    private BigDecimal limit;
    private BigDecimal spent;
    private BigDecimal remaining;
    private String status; // "OK", "WARNING" or "OVER BUDGET"

    public BudgetStatus(String categoryName, BigDecimal limit, BigDecimal spent, String status) {
        this.categoryName = categoryName;
        this.limit = limit;
        this.spent = spent;
        this.remaining = limit.subtract(spent);
        this.status = status;
    }

    public String getCategoryName() { return categoryName; }
    public BigDecimal getLimit() { return limit; }
    public BigDecimal getSpent() { return spent; }
    public BigDecimal getRemaining() { return remaining; }
    public String getStatus() { return status; }
}
