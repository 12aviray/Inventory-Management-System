package com.inventory.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Central point for obtaining SQLite connections and initializing the schema.
 * This is plain infrastructure (not one of the project's showcased design
 * patterns) — a single shared connection-string/config point is standard
 * practice for a small desktop app, not a pattern used to solve a business
 * problem.
 */
public final class DatabaseManager {

    private static final String DB_FILE = "inventory.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /** Creates tables from schema.sql if they do not already exist. */
    public static void initializeSchema() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            String schema = readSchemaFile();
            for (String rawStatement : schema.split(";")) {
                String sql = rawStatement.trim();
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }

            // Ensure unit_price column exists on existing databases
            try {
                st.execute("ALTER TABLE stock_movement ADD COLUMN unit_price REAL NOT NULL DEFAULT 0;");
            } catch (SQLException ignored) {
                // Column already exists
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    private static String readSchemaFile() throws IOException {
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql not found on classpath at /db/schema.sql");
            }
            return new String(in.readAllBytes());
        }
    }

    public static boolean databaseFileExists() {
        return Files.exists(Path.of(DB_FILE));
    }
}
