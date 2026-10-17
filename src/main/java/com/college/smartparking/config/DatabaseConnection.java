package com.college.smartparking.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC Connection Manager for Smart Parking application.
 * Manages connections to the MySQL database 'smart_parking'.
 */
public class DatabaseConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/smart_parking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "root";

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        // Load configuration from System properties, Environment variables, or defaults
        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        String envPass = System.getenv("DB_PASSWORD");

        URL = (envUrl != null && !envUrl.isBlank()) ? envUrl : System.getProperty("db.url", DEFAULT_URL);
        USER = (envUser != null && !envUser.isBlank()) ? envUser : System.getProperty("db.user", DEFAULT_USER);
        PASSWORD = (envPass != null) ? envPass : System.getProperty("db.password", DEFAULT_PASSWORD);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    private DatabaseConnection() {
        // Private constructor to prevent instantiation
    }

    /**
     * Obtains a new database connection from DriverManager.
     *
     * @return an open {@link Connection} to the database
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.err.println("[DatabaseConnection] Failed to establish database connection to: " + URL);
            System.err.println("[DatabaseConnection] Error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Safely closes a database connection if open.
     *
     * @param connection the Connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[DatabaseConnection] Error closing connection: " + e.getMessage());
            }
        }
    }
}
