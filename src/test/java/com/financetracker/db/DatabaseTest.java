package com.financetracker.db;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @Test
    void schemaSplitsIntoValidStatements() throws Exception {
        String sql;
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<String> statements = Database.splitStatements(sql);
        // CREATE DATABASE, USE, 7 tables, INSERT = 10 statements.
        assertEquals(10, statements.size());
        for (String statement : statements) {
            assertTrue(statement.startsWith("CREATE") || statement.startsWith("USE")
                            || statement.startsWith("INSERT"),
                    "Unexpected statement start: "
                            + statement.substring(0, Math.min(60, statement.length())));
        }
        assertTrue(statements.stream().anyMatch(s -> s.contains("CREATE TABLE IF NOT EXISTS loans (")));
    }
}
