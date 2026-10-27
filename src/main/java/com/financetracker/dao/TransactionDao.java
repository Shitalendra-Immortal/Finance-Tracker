package com.financetracker.dao;

import com.financetracker.db.Database;
import com.financetracker.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All transaction SQL. No UI code, no business rules — those live in the service. */
public class TransactionDao {

    private static final String BASE_SELECT =
            "SELECT t.id, t.type, t.category_id, c.name AS category_name, "
            + "t.amount, t.txn_date, t.note, t.payment_method "
            + "FROM transactions t JOIN categories c ON t.category_id = c.id ";

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getString("type"),
                rs.getInt("category_id"),
                rs.getString("category_name"),
                rs.getBigDecimal("amount"),
                rs.getDate("txn_date").toLocalDate(),
                rs.getString("note"),
                rs.getString("payment_method"));
    }

    public void insert(Transaction txn) throws SQLException {
        String sql = "INSERT INTO transactions (type, category_id, amount, txn_date, note, payment_method)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txn.getType());
            ps.setInt(2, txn.getCategoryId());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setDate(4, Date.valueOf(txn.getDate()));
            ps.setString(5, txn.getNote());
            ps.setString(6, txn.getPaymentMethod());
            ps.executeUpdate();
        }
    }

    public void update(Transaction txn) throws SQLException {
        String sql = "UPDATE transactions SET type = ?, category_id = ?, amount = ?, "
                + "txn_date = ?, note = ?, payment_method = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txn.getType());
            ps.setInt(2, txn.getCategoryId());
            ps.setBigDecimal(3, txn.getAmount());
            ps.setDate(4, Date.valueOf(txn.getDate()));
            ps.setString(5, txn.getNote());
            ps.setString(6, txn.getPaymentMethod());
            ps.setInt(7, txn.getId());
            ps.executeUpdate();
        }
    }

    public List<Transaction> findAll() throws SQLException {
        List<Transaction> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(BASE_SELECT + "ORDER BY t.txn_date DESC, t.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(mapRow(rs));
            }
        }
        return out;
    }

    /** Searchable history: matches the note text or the category name. */
    public List<Transaction> search(String keyword) throws SQLException {
        List<Transaction> out = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE t.note LIKE ? OR c.name LIKE ? "
                + "ORDER BY t.txn_date DESC, t.id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    /**
     * Filtered view. Pass null for type, -1 for categoryId, or null for month
     * to leave that filter out ("All").
     */
    public List<Transaction> findFiltered(String type, int categoryId, String month) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT + "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (type != null) {
            sql.append("AND t.type = ? ");
            params.add(type);
        }
        if (categoryId != -1) {
            sql.append("AND t.category_id = ? ");
            params.add(categoryId);
        }
        if (month != null) {
            sql.append("AND DATE_FORMAT(t.txn_date, '%Y-%m') = ? ");
            params.add(month);
        }
        sql.append("ORDER BY t.txn_date DESC, t.id DESC");
        List<Transaction> out = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    public List<Transaction> findByMonth(String month) throws SQLException {
        List<Transaction> out = new ArrayList<>();
        String sql = BASE_SELECT + "WHERE DATE_FORMAT(t.txn_date, '%Y-%m') = ? "
                + "ORDER BY t.txn_date DESC, t.id DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapRow(rs));
                }
            }
        }
        return out;
    }

    /** Total income or expense for one month ("YYYY-MM"). Returns 0 when empty. */
    public BigDecimal sumByTypeAndMonth(String type, String month) throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM transactions "
                + "WHERE type = ? AND DATE_FORMAT(txn_date, '%Y-%m') = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
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

    public void deleteById(int id) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM transactions WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Insert many rows in one database transaction: all commit together,
     * or all roll back. Used by CSV import so a failed import never leaves
     * half its rows behind.
     */
    public void insertAll(List<Transaction> txns) throws SQLException {
        if (txns.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO transactions (type, category_id, amount, txn_date, note, payment_method)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                for (Transaction txn : txns) {
                    ps.setString(1, txn.getType());
                    ps.setInt(2, txn.getCategoryId());
                    ps.setBigDecimal(3, txn.getAmount());
                    ps.setDate(4, Date.valueOf(txn.getDate()));
                    ps.setString(5, txn.getNote());
                    ps.setString(6, txn.getPaymentMethod());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
