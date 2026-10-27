package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One income or expense entry. Amount is BigDecimal — never float/double. */
public class Transaction {
    private int id;
    private String type; // "INCOME" or "EXPENSE"
    private int categoryId;
    private String categoryName;
    private BigDecimal amount;
    private LocalDate date;
    private String note;
    private String paymentMethod;

    public Transaction() {
    }

    public Transaction(String type, int categoryId, BigDecimal amount, LocalDate date,
                       String note, String paymentMethod) {
        this.type = type;
        this.categoryId = categoryId;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.paymentMethod = paymentMethod;
    }

    public Transaction(int id, String type, int categoryId, String categoryName,
                       BigDecimal amount, LocalDate date, String note, String paymentMethod) {
        this.id = id;
        this.type = type;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.paymentMethod = paymentMethod;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
