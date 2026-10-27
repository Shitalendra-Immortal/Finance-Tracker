package com.financetracker.service;

import com.financetracker.model.Loan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoanServiceTest {

    private static Loan loan(String amount, String returned) {
        return new Loan(1, "Rahul", new BigDecimal(amount), new BigDecimal(returned),
                LocalDate.now().minusDays(5), "");
    }

    @Test
    void remainingSubtractsRepayments() {
        assertEquals(new BigDecimal("700"), loan("1000", "300").getRemaining());
    }

    @Test
    void statusIsOpenUntilFullyRepaid() {
        assertEquals("OPEN", loan("1000", "300").getStatus());
        assertEquals("SETTLED", loan("1000", "1000").getStatus());
    }

    @Test
    void validateRejectsBadLoans() {
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateLoan("", new BigDecimal("100"), LocalDate.now(), ""));
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateLoan("Rahul", BigDecimal.ZERO, LocalDate.now(), ""));
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateLoan("Rahul", new BigDecimal("-50"), LocalDate.now(), ""));
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateLoan("Rahul", new BigDecimal("100"), null, ""));
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateLoan("Rahul", new BigDecimal("100"), LocalDate.now().plusDays(1), ""));
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateLoan("Rahul", new BigDecimal("100"), LocalDate.now(), "x".repeat(256)));
    }

    @Test
    void validateAcceptsPastLentDate() {
        LoanService.validateLoan("Rahul", new BigDecimal("100"), LocalDate.now().minusDays(30), "");
    }

    @Test
    void validateRepaymentRejectsOverpayment() {
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateRepayment(new BigDecimal("800"), new BigDecimal("700")));
        assertThrows(IllegalArgumentException.class, () ->
                LoanService.validateRepayment(BigDecimal.ZERO, new BigDecimal("700")));
    }

    @Test
    void validateRepaymentAcceptsExactRemaining() {
        LoanService.validateRepayment(new BigDecimal("700"), new BigDecimal("700"));
    }
}
