package com.furnitureapp.database;

import java.sql.*;
import java.io.File;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:furnituredesigner.db";

    public DatabaseManager() {
        initializeDatabase();
    }

    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.err.println("Error connecting to SQLite database: " + e.getMessage());
        }
        return conn;
    }

    private void initializeDatabase() {
        File dbFile = new File("furnituredesigner.db");
        boolean dbExists = dbFile.exists();

        String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                                  " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                  " username TEXT NOT NULL UNIQUE," +
                                  " password TEXT NOT NULL" +
                                  ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Create users table if it doesn't exist
            stmt.execute(createUserTableSql);
            System.out.println("Users table checked/created.");

            // Add a default user if the database was just created
            if (!dbExists) {
                // In a real app, use secure password hashing!
                String insertDefaultUserSql = "INSERT INTO users(username, password) VALUES(?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertDefaultUserSql)) {
                    pstmt.setString(1, "designer");
                    pstmt.setString(2, "password123"); // VERY insecure placeholder
                    pstmt.executeUpdate();
                    System.out.println("Default user 'designer' created.");
                } catch (SQLException e) {
                    if (e.getErrorCode() != 19) { // 19 is SQLITE_CONSTRAINT for UNIQUE constraint
                         System.err.println("Error inserting default user: " + e.getMessage());
                    }
                 }
            }

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    /**
     * Verifies user credentials against the database.
     * IMPORTANT: This uses plain text passwords, which is highly insecure.
     * In a real application, use salted password hashing.
     * @param username The username to check.
     * @param password The plain text password to check.
     * @return true if credentials are valid, false otherwise.
     */
    public boolean verifyUser(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            // Check if user exists and password matches
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // Direct comparison (insecure!)
                return storedPassword.equals(password);
            }
        } catch (SQLException e) {
            System.err.println("Error verifying user: " + e.getMessage());
        }
        return false;
    }
} 