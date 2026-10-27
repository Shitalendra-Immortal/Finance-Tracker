package com.financetracker.dao;

import com.financetracker.db.Database;
import com.financetracker.model.SavingsGoal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** All savings-goal SQL. */
public class SavingsGoalDao {

    public void create(SavingsGoal goal) throws SQLException {
        String sql = "INSERT INTO savings_goals (name, target_amount, deadline) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.getName());
            ps.setBigDecimal(2, goal.getTargetAmount());
            ps.setDate(3, Date.valueOf(goal.getDeadline()));
            ps.executeUpdate();
        }
    }

    /** All goals with their saved totals (sum of contributions), nearest deadline first. */
    public List<SavingsGoal> findAllWithSaved() throws SQLException {
        List<SavingsGoal> out = new ArrayList<>();
        String sql = "SELECT g.id, g.name, g.target_amount, g.deadline, "
                + "COALESCE(SUM(c.amount), 0) AS saved "
                + "FROM savings_goals g LEFT JOIN goal_contributions c ON c.goal_id = g.id "
                + "GROUP BY g.id, g.name, g.target_amount, g.deadline ORDER BY g.deadline";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BigDecimal saved = rs.getBigDecimal("saved");
                out.add(new SavingsGoal(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("target_amount"),
                        saved == null ? BigDecimal.ZERO : saved,
                        rs.getDate("deadline").toLocalDate()));
            }
        }
        return out;
    }

    public void addContribution(int goalId, BigDecimal amount, LocalDate date, String note) throws SQLException {
        String sql = "INSERT INTO goal_contributions (goal_id, amount, contribution_date, note)"
                + " VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.setBigDecimal(2, amount);
            ps.setDate(3, Date.valueOf(date));
            ps.setString(4, note);
            ps.executeUpdate();
        }
    }

    /** Contributions are deleted automatically (ON DELETE CASCADE). */
    public void deleteGoal(int goalId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM savings_goals WHERE id = ?")) {
            ps.setInt(1, goalId);
            ps.executeUpdate();
        }
    }
}
