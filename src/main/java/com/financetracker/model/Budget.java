package com.financetracker.model;

import java.math.BigDecimal;

/** A monthly spending limit for one expense category. Month is "YYYY-MM". */
public class Budget {
    private int id;
    private int categoryId;
    private String categoryName;
    private String month;
    private BigDecimal amount;

    public Budget() {
    }

    public Budget(int categoryId, String month, BigDecimal amount) {
        this.categoryId = categoryId;
        this.month = month;
        this.amount = amount;
    }

    public Budget(int id, int categoryId, String categoryName, String month, BigDecimal amount) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.month = month;
        this.amount = amount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
