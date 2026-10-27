package com.financetracker.service;

import com.financetracker.dao.LoanDao;
import com.financetracker.model.Loan;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Loan rules + validation. Remaining/status math lives on the loan itself. */
public class LoanService {

    private final LoanDao loanDao;

    public LoanService(LoanDao loanDao) {
        this.loanDao = loanDao;
    }

    public static void validateLoan(String personName, BigDecimal amount, LocalDate lentDate, String note) {
        if (personName == null || personName.isBlank()) {
            throw new IllegalArgumentException("Give the friend's name.");
        }
        if (personName.length() > 100) {
            throw new IllegalArgumentException("Name is too long (max 100 characters).");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero.");
        }
        if (lentDate == null) {
            throw new IllegalArgumentException("Pick the date you lent the money.");
        }
        if (lentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Lent date can't be in the future.");
        }
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("Note is too long (max 255 characters).");
        }
    }

    /** A repayment must be positive and can never exceed what is still owed. */
    public static void validateRepayment(BigDecimal amount, BigDecimal remaining) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repayment must be greater than zero.");
        }
        if (remaining != null && amount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException("That repayment is more than the amount still owed.");
        }
    }

    public void createLoan(String personName, BigDecimal amount, LocalDate lentDate, String note) throws SQLException {
        validateLoan(personName, amount, lentDate, note);
        loanDao.create(new Loan(personName, amount, lentDate, note));
    }

    public void addRepayment(int loanId, BigDecimal amount, BigDecimal remaining, LocalDate date) throws SQLException {
        validateRepayment(amount, remaining);
        loanDao.addRepayment(loanId, amount, date, "");
    }

    public List<Loan> findAllWithReturned() throws SQLException {
        return loanDao.findAllWithReturned();
    }

    public void deleteLoan(int loanId) throws SQLException {
        loanDao.deleteLoan(loanId);
    }
}
