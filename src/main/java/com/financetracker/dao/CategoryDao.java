package com.financetracker.dao;

import com.financetracker.db.Database;
import com.financetracker.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

    public List<Category> findAll() throws SQLException {
        List<Category> out = new ArrayList<>();
        String sql = "SELECT id, name, type FROM categories ORDER BY type, name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new Category(rs.getInt("id"), rs.getString("name"), rs.getString("type")));
            }
        }
        return out;
    }

    public List<Category> findByType(String type) throws SQLException {
        List<Category> out = new ArrayList<>();
        String sql = "SELECT id, name, type FROM categories WHERE type = ? ORDER BY name";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Category(rs.getInt("id"), rs.getString("name"), rs.getString("type")));
                }
            }
        }
        return out;
    }
}
