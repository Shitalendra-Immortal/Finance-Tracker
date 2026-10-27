package com.financetracker.service;

import com.financetracker.dao.SavingsGoalDao;
import com.financetracker.model.SavingsGoal;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Savings-goal rules + validation. Progress math lives on the goal itself. */
public class GoalService {

    private final SavingsGoalDao goalDao;

    public GoalService(SavingsGoalDao goalDao) {
        this.goalDao = goalDao;
    }

    public static void validateGoal(String name, BigDecimal target, LocalDate deadline) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Give the goal a name.");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("Goal name is too long (max 100 characters).");
        }
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target amount must be greater than zero.");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("Pick a deadline.");
        }
        if (deadline.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline must be today or in the future.");
        }
    }

    public static void validateContribution(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Contribution must be greater than zero.");
        }
    }

    public void createGoal(String name, BigDecimal target, LocalDate deadline) throws SQLException {
        validateGoal(name, target, deadline);
        goalDao.create(new SavingsGoal(name, target, deadline));
    }

    /** Editing keeps contributions: the new target must still cover what is already saved. */
    public void updateGoal(int goalId, String name, BigDecimal target, LocalDate deadline,
            BigDecimal alreadySaved) throws SQLException {
        validateGoal(name, target, deadline);
        if (alreadySaved != null && target.compareTo(alreadySaved) < 0) {
            throw new IllegalArgumentException("Target can't be less than the amount already saved.");
        }
        goalDao.update(goalId, name, target, deadline);
    }

    public void addContribution(int goalId, BigDecimal amount, LocalDate date) throws SQLException {
        validateContribution(amount);
        goalDao.addContribution(goalId, amount, date, "");
    }

    public List<SavingsGoal> findAllWithSaved() throws SQLException {
        return goalDao.findAllWithSaved();
    }

    public void deleteGoal(int goalId) throws SQLException {
        goalDao.deleteGoal(goalId);
    }
}
