package com.financetracker.dao;

import com.financetracker.db.Database;
import com.financetracker.model.Budget;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All budget SQL. The status rule lives in BudgetService, not here. */
public class BudgetDao {

    /** Deletes one category's budget for one month. No other month is touched. */
    public void deleteByCategoryAndMonth(int categoryId, String month) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM budgets WHERE category_id = ? AND month = ?")) {
            ps.setInt(1, categoryId);
            ps.setString(2, month);
            ps.executeUpdate();
        }
    }

    /** Insert a new budget, or replace the amount if one already exists for that month. */
    public void upsert(Budget budget) throws SQLException {
        String sql = "INSERT INTO budgets (category_id, month, amount) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE amount = VALUES(amount)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, budget.getCategoryId());
            ps.setString(2, budget.getMonth());
            ps.setBigDecimal(3, budget.getAmount());
            ps.executeUpdate();
        }
    }

    public List<Budget> findByMonth(String month) throws SQLException {
        List<Budget> out = new ArrayList<>();
        String sql = "SELECT b.id, b.category_id, c.name AS category_name, b.month, b.amount "
                + "FROM budgets b JOIN categories c ON b.category_id = c.id "
                + "WHERE b.month = ? ORDER BY c.name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Budget(
                            rs.getInt("id"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getString("month"),
                            rs.getBigDecimal("amount")));
                }
            }
        }
        return out;
    }

    /**
     * Copy this month's budgets into next month. Never automatic — the UI
     * only calls this when the user clicks "Copy to next month".
     * Existing next-month budgets are overwritten with these amounts.
     */
    public void copyBudgetsToNextMonth(String month) throws SQLException {
        String sql = "INSERT INTO budgets (category_id, month, amount) "
                + "SELECT category_id, "
                + "DATE_FORMAT(DATE_ADD(STR_TO_DATE(CONCAT(month, '-01'), '%Y-%m-%d'), INTERVAL 1 MONTH), '%Y-%m'), "
                + "amount FROM budgets WHERE month = ? "
                + "ON DUPLICATE KEY UPDATE amount = VALUES(amount)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            ps.executeUpdate();
        }
    }

    /** How much was actually spent in one category during one month. */
    public BigDecimal spentForCategoryAndMonth(int categoryId, String month) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM transactions "
                + "WHERE category_id = ? AND type = 'EXPENSE' "
                + "AND DATE_FORMAT(txn_date, '%Y-%m') = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setString(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("total");
                    return total == null ? BigDecimal.ZERO : total;
                }
                return BigDecimal.ZERO;
            }
        }
    }
}
