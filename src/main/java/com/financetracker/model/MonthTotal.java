package com.financetracker.model;

import java.math.BigDecimal;

/** Income vs expense for one month, for the comparison chart. */
public class MonthTotal {
    private String month;
    private BigDecimal income;
    private BigDecimal expense;

    public MonthTotal(String month, BigDecimal income, BigDecimal expense) {
        this.month = month;
        this.income = income;
        this.expense = expense;
    }

    public String getMonth() { return month; }
    public BigDecimal getIncome() { return income; }
    public BigDecimal getExpense() { return expense; }
}
