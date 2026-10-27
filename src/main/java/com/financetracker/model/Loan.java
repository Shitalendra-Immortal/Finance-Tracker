package com.financetracker.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money lent to a friend: the lent amount, plus repayments tracked
 * separately and summed into returnedAmount by the DAO.
 * getRemaining() and getStatus() are pure derivations from this loan's
 * own fields (no database access), so the table can bind to them directly.
 * A loan is deliberately NOT a transaction: lent money is expected back,
 * so counting it as an expense would distort income/expense reports.
 */
public class Loan {
    private int id;
    private String personName;
    private BigDecimal amount;
    private BigDecimal returnedAmount;
    private LocalDate lentDate;
    private String note;

    public Loan() {
    }

    public Loan(String personName, BigDecimal amount, LocalDate lentDate, String note) {
        this.personName = personName;
        this.amount = amount;
        this.lentDate = lentDate;
        this.note = note;
    }

    public Loan(int id, String personName, BigDecimal amount, BigDecimal returnedAmount,
                LocalDate lentDate, String note) {
        this.id = id;
        this.personName = personName;
        this.amount = amount;
        this.returnedAmount = returnedAmount;
        this.lentDate = lentDate;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getReturnedAmount() { return returnedAmount; }
    public void setReturnedAmount(BigDecimal returnedAmount) { this.returnedAmount = returnedAmount; }
    public LocalDate getLentDate() { return lentDate; }
    public void setLentDate(LocalDate lentDate) { this.lentDate = lentDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    /** Money the friend still owes. Never negative: repayments are capped at this. */
    public BigDecimal getRemaining() {
        BigDecimal remaining = amount.subtract(returnedAmount);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    /** OPEN while anything is still owed, SETTLED once fully repaid. */
    public String getStatus() {
        return getRemaining().compareTo(BigDecimal.ZERO) == 0 ? "SETTLED" : "OPEN";
    }
}
