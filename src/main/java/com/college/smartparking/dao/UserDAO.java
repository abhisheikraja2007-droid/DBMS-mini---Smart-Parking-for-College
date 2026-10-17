package com.college.smartparking.dao;

import com.college.smartparking.config.DatabaseConnection;
import com.college.smartparking.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for USERS table operations.
 * Implements prepared statements and try-with-resources to prevent SQL injection and resource leaks.
 */
public class UserDAO {

    /**
     * Inserts a new user into the USERS table and returns the generated user_id.
     *
     * @param user the User to register
     * @return generated user_id, or -1 if insertion fails
     * @throws SQLException if a database error occurs
     */
    public int registerUser(User user) throws SQLException {
        String sql = "INSERT INTO USERS (user_name, email, contact_info, vehicle_number) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUserName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getContactInfo());
            pstmt.setString(4, user.getVehicleNumber());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        user.setUserId(generatedId);
                        return generatedId;
                    }
                }
            }
            return -1;
        }
    }

    /**
     * Fetches user details by user ID.
     *
     * @param userId the user ID to search
     * @return User object if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT user_id, user_name, email, contact_info, vehicle_number FROM USERS WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Fetches user details by registered vehicle number.
     *
     * @param vehicleNumber the vehicle number to search
     * @return User object if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public User getUserByVehicleNumber(String vehicleNumber) throws SQLException {
        String sql = "SELECT user_id, user_name, email, contact_info, vehicle_number FROM USERS WHERE vehicle_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, vehicleNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Fetches user details by email.
     *
     * @param email the email to search
     * @return User object if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, user_name, email, contact_info, vehicle_number FROM USERS WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all registered users in the database.
     *
     * @return list of all users
     * @throws SQLException if a database error occurs
     */
    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT user_id, user_name, email, contact_info, vehicle_number FROM USERS ORDER BY user_id";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    /**
     * Helper to map a ResultSet row into a User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("user_name"),
                rs.getString("email"),
                rs.getString("contact_info"),
                rs.getString("vehicle_number")
        );
    }
}
