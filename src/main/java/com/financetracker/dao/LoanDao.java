package com.financetracker.dao;

import com.financetracker.db.Database;
import com.financetracker.model.Loan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** All loan SQL. */
public class LoanDao {

    public void create(Loan loan) throws SQLException {
        String sql = "INSERT INTO loans (person_name, amount, lent_date, note) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loan.getPersonName());
            ps.setBigDecimal(2, loan.getAmount());
            ps.setDate(3, Date.valueOf(loan.getLentDate()));
            ps.setString(4, loan.getNote());
            ps.executeUpdate();
        }
    }

    /** All loans with their repaid totals (sum of repayments), most recent first. */
    public List<Loan> findAllWithReturned() throws SQLException {
        List<Loan> out = new ArrayList<>();
        String sql = "SELECT l.id, l.person_name, l.amount, l.lent_date, l.note, "
                + "COALESCE(SUM(r.amount), 0) AS returned "
                + "FROM loans l LEFT JOIN loan_repayments r ON r.loan_id = l.id "
                + "GROUP BY l.id, l.person_name, l.amount, l.lent_date, l.note "
                + "ORDER BY l.lent_date DESC, l.id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal returned = rs.getBigDecimal("returned");
                out.add(new Loan(
                        rs.getInt("id"),
                        rs.getString("person_name"),
                        rs.getBigDecimal("amount"),
                        returned == null ? BigDecimal.ZERO : returned,
                        rs.getDate("lent_date").toLocalDate(),
                        rs.getString("note")));
            }
        }
        return out;
    }

    public void addRepayment(int loanId, BigDecimal amount, LocalDate date, String note) throws SQLException {
        String sql = "INSERT INTO loan_repayments (loan_id, amount, repaid_date, note)"
                + " VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            ps.setBigDecimal(2, amount);
            ps.setDate(3, Date.valueOf(date));
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    /** Repayments are deleted automatically (ON DELETE CASCADE). */
    public void deleteLoan(int loanId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM loans WHERE id = ?")) {
            ps.setInt(1, loanId);
            ps.executeUpdate();
        }
    }
}
