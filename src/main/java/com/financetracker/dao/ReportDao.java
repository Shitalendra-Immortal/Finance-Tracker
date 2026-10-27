package com.financetracker.dao;

import com.financetracker.db.Database;
import com.financetracker.model.CategorySummary;
import com.financetracker.model.MonthTotal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** All report SQL. Aggregation only — presentation lives in the service/controller. */
public class ReportDao {

    /** Category-wise expense totals for one month, biggest spend first. */
    public List<CategorySummary> expenseByCategory(String month) throws SQLException {
        List<CategorySummary> out = new ArrayList<>();
        String sql = "SELECT c.name AS category_name, SUM(t.amount) AS total "
                + "FROM transactions t JOIN categories c ON t.category_id = c.id "
                + "WHERE t.type = 'EXPENSE' AND DATE_FORMAT(t.txn_date, '%Y-%m') = ? "
                + "GROUP BY c.id, c.name ORDER BY total DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new CategorySummary(rs.getString("category_name"), rs.getBigDecimal("total")));
                }
            }
        }
        return out;
    }

    /** Category-wise expense totals for one week (inclusive dates), biggest first. */
    public List<CategorySummary> expenseByCategoryForWeek(LocalDate weekStart, LocalDate weekEnd)
            throws SQLException {
        List<CategorySummary> out = new ArrayList<>();
        String sql = "SELECT c.name AS category_name, SUM(t.amount) AS total "
                + "FROM transactions t JOIN categories c ON t.category_id = c.id "
                + "WHERE t.type = 'EXPENSE' AND t.txn_date BETWEEN ? AND ? "
                + "GROUP BY c.id, c.name ORDER BY total DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(weekStart));
            ps.setDate(2, Date.valueOf(weekEnd));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new CategorySummary(rs.getString("category_name"), rs.getBigDecimal("total")));
                }
            }
        }
        return out;
    }

    /** Category-wise expense totals for one year ("YYYY"), biggest first. */
    public List<CategorySummary> expenseByCategoryForYear(String year) throws SQLException {
        List<CategorySummary> out = new ArrayList<>();
        String sql = "SELECT c.name AS category_name, SUM(t.amount) AS total "
                + "FROM transactions t JOIN categories c ON t.category_id = c.id "
                + "WHERE t.type = 'EXPENSE' AND DATE_FORMAT(t.txn_date, '%Y') = ? "
                + "GROUP BY c.id, c.name ORDER BY total DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new CategorySummary(rs.getString("category_name"), rs.getBigDecimal("total")));
                }
            }
        }
        return out;
    }

    /** Income vs expense per month, for the comparison chart. Months in given order. */
    public List<MonthTotal> monthlyTotals(List<String> months) throws SQLException {
        List<MonthTotal> out = new ArrayList<>();
        String sql = "SELECT type, COALESCE(SUM(amount), 0) AS total FROM transactions "
                + "WHERE DATE_FORMAT(txn_date, '%Y-%m') = ? GROUP BY type";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String month : months) {
                ps.setString(1, month);
                BigDecimal income = BigDecimal.ZERO;
                BigDecimal expense = BigDecimal.ZERO;
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal total = rs.getBigDecimal("total");
                        if (total == null) {
                            total = BigDecimal.ZERO;
                        }
                        if ("INCOME".equals(rs.getString("type"))) {
                            income = total;
                        } else {
                            expense = total;
                        }
                    }
                }
                out.add(new MonthTotal(month, income, expense));
            }
        }
        return out;
    }
}
