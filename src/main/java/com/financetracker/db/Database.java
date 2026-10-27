package com.financetracker.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * MySQL connection handling.
 * Settings come from db.properties on the classpath (never hardcoded).
 * initDatabase() runs schema.sql, so a fresh MySQL install just works.
 *
 * Dates rule: transaction dates, deadlines and contribution dates are plain
 * calendar dates (MySQL DATE, java.time.LocalDate) in the user's local
 * timezone — no time component, so no timezone conversion ever applies to
 * them. serverTimezone=UTC in the JDBC URL only pins the TIMESTAMP columns
 * (created_at) to UTC, so they stay consistent whatever the server zone is.
 */
public class Database {
    private static String host = "localhost";
    private static String port = "3306";
    private static String dbName = "finance_tracker";
    private static String user = "root";
    private static String password = "";

    private static String baseUrl() {
        return "jdbc:mysql://" + host + ":" + port
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private static String appUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    /** Load db.properties once; missing file just keeps the defaults above. */
    private static void loadProperties() {
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                return;
            }
            Properties props = new Properties();
            props.load(in);
            host = props.getProperty("db.host", host);
            port = props.getProperty("db.port", port);
            dbName = props.getProperty("db.name", dbName);
            user = props.getProperty("db.user", user);
            password = props.getProperty("db.password", password);
        } catch (Exception e) {
            System.err.println("Could not read db.properties, using defaults: " + e.getMessage());
        }
    }

    /** Normal app connection (database must already exist). */
    public static Connection getConnection() throws SQLException {
        loadProperties();
        return DriverManager.getConnection(appUrl(), user, password);
    }

    /**
     * Split schema.sql into executable statements. Full-line -- comments are
     * dropped first: splitting on ';' alone cut a statement in half when a
     * comment contained a ';' (the v6 loans comment did), producing invalid
     * SQL that failed startup behind the generic connection-error dialog.
     */
    static List<String> splitStatements(String sql) {
        StringBuilder kept = new StringBuilder();
        for (String line : sql.split("\n")) {
            if (!line.trim().startsWith("--")) {
                kept.append(line).append('\n');
            }
        }
        List<String> out = new ArrayList<>();
        for (String part : kept.toString().split(";")) {
            String statement = part.trim();
            if (!statement.isEmpty()) {
                out.add(statement);
            }
        }
        return out;
    }

    /** Create the database and tables from schema.sql. Safe to run every launch. */
    public static void initDatabase() throws SQLException {
        loadProperties();
        String sql;
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new SQLException("schema.sql not found on the classpath");
            }
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Could not read schema.sql", e);
        }
        try (Connection conn = DriverManager.getConnection(baseUrl(), user, password);
             Statement stmt = conn.createStatement()) {
            for (String statement : splitStatements(sql)) {
                stmt.execute(statement);
            }
        }
        // v2 migration: databases created by v1 lack transactions.payment_method.
        try (Connection conn = DriverManager.getConnection(appUrl(), user, password);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) AS c FROM INFORMATION_SCHEMA.COLUMNS "
                     + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'transactions' "
                     + "AND COLUMN_NAME = 'payment_method'")) {
            ps.setString(1, dbName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("c") == 0) {
                    try (Statement alter = conn.createStatement()) {
                        alter.execute("ALTER TABLE transactions "
                                + "ADD COLUMN payment_method VARCHAR(30) NOT NULL DEFAULT 'Cash'");
                    }
                }
            }
        }
    }
}
