package com.financetracker.model;

import java.math.BigDecimal;

/** One row of the category-wise expense report: total spent per category. */
public class CategorySummary {
    private String categoryName;
    private BigDecimal total;

    public CategorySummary(String categoryName, BigDecimal total) {
        this.categoryName = categoryName;
        this.total = total;
    }

    public String getCategoryName() { return categoryName; }
    public BigDecimal getTotal() { return total; }
}
