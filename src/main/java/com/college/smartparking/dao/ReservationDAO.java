package com.college.smartparking.dao;

import com.college.smartparking.config.DatabaseConnection;
import com.college.smartparking.model.ParkingReservation;
import com.college.smartparking.model.ReservationStatus;
import com.college.smartparking.model.SlotStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for PARKING_RESERVATIONS table.
 * Implements explicit JDBC transactions (setAutoCommit(false), commit(), rollback())
 * to ensure atomicity across reservation operations and slot status updates.
 */
public class ReservationDAO {

    /**
     * Creates a new parking reservation in a single atomic transaction:
     * 1. Verifies the slot is currently AVAILABLE (with FOR UPDATE lock).
     * 2. Sets reservation_time = now() and arrival_deadline = now() + deadlineMinutes.
     * 3. Inserts a new record into PARKING_RESERVATIONS with status CONFIRMED.
     * 4. Updates PARKING_SLOTS status to RESERVED.
     * 5. Commits and returns the generated reservation_id.
     *
     * @param userId          the ID of the reserving user
     * @param slotId          the ID of the slot to reserve
     * @param deadlineMinutes the minutes before the reservation expires
     * @return generated reservation_id, or -1 if slot is unavailable or failure occurs
     * @throws SQLException if a database error occurs
     */
    public int createReservation(int userId, int slotId, int deadlineMinutes) throws SQLException {
        String checkSlotSql = "SELECT status FROM PARKING_SLOTS WHERE slot_id = ? FOR UPDATE";
        String insertReservationSql = "INSERT INTO PARKING_RESERVATIONS (user_id, slot_id, reservation_time, arrival_deadline, arrival_time, status) " +
                                     "VALUES (?, ?, ?, ?, NULL, ?)";
        String updateSlotSql = "UPDATE PARKING_SLOTS SET status = ? WHERE slot_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Verify slot availability
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSlotSql)) {
                checkStmt.setInt(1, slotId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return -1; // Slot does not exist
                    }
                    String currentStatus = rs.getString("status");
                    if (!SlotStatus.AVAILABLE.name().equalsIgnoreCase(currentStatus)) {
                        conn.rollback();
                        return -1; // Slot is not AVAILABLE
                    }
                }
            }

            // 2. Prepare timestamps
            LocalDateTime reservationTime = LocalDateTime.now();
            LocalDateTime arrivalDeadline = reservationTime.plusMinutes(deadlineMinutes);

            // 3. Insert reservation record
            int generatedReservationId = -1;
            try (PreparedStatement insertStmt = conn.prepareStatement(insertReservationSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, slotId);
                insertStmt.setTimestamp(3, Timestamp.valueOf(reservationTime));
                insertStmt.setTimestamp(4, Timestamp.valueOf(arrivalDeadline));
                insertStmt.setString(5, ReservationStatus.CONFIRMED.name());

                int affected = insertStmt.executeUpdate();
                if (affected <= 0) {
                    conn.rollback();
                    return -1;
                }

                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedReservationId = rs.getInt(1);
                    }
                }
            }

            if (generatedReservationId == -1) {
                conn.rollback();
                return -1;
            }

            // 4. Update slot status to RESERVED
            try (PreparedStatement updateSlotStmt = conn.prepareStatement(updateSlotSql)) {
                updateSlotStmt.setString(1, SlotStatus.RESERVED.name());
                updateSlotStmt.setInt(2, slotId);
                updateSlotStmt.executeUpdate();
            }

            // 5. Commit transaction
            conn.commit();
            return generatedReservationId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Rollback error: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Error resetting auto-commit: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Cancels an existing reservation:
     * 1. Verifies reservation belongs to userId and is currently CONFIRMED.
     * 2. Updates reservation status to CANCELLED.
     * 3. Updates the associated slot status back to AVAILABLE.
     *
     * @param reservationId the reservation ID to cancel
     * @param userId        the user ID requesting cancellation
     * @return true if cancelled successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean cancelReservation(int reservationId, int userId) throws SQLException {
        String querySql = "SELECT slot_id, status FROM PARKING_RESERVATIONS WHERE reservation_id = ? AND user_id = ? FOR UPDATE";
        String updateReservationSql = "UPDATE PARKING_RESERVATIONS SET status = ? WHERE reservation_id = ?";
        String updateSlotSql = "UPDATE PARKING_SLOTS SET status = ? WHERE slot_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            int slotId = -1;
            try (PreparedStatement queryStmt = conn.prepareStatement(querySql)) {
                queryStmt.setInt(1, reservationId);
                queryStmt.setInt(2, userId);

                try (ResultSet rs = queryStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false; // Not found or does not belong to user
                    }
                    String currentStatus = rs.getString("status");
                    if (!ReservationStatus.CONFIRMED.name().equalsIgnoreCase(currentStatus)) {
                        conn.rollback();
                        return false; // Only CONFIRMED reservations can be cancelled
                    }
                    slotId = rs.getInt("slot_id");
                }
            }

            // Update reservation status to CANCELLED
            try (PreparedStatement updateResStmt = conn.prepareStatement(updateReservationSql)) {
                updateResStmt.setString(1, ReservationStatus.CANCELLED.name());
                updateResStmt.setInt(2, reservationId);
                updateResStmt.executeUpdate();
            }

            // Update slot status back to AVAILABLE
            try (PreparedStatement updateSlotStmt = conn.prepareStatement(updateSlotSql)) {
                updateSlotStmt.setString(1, SlotStatus.AVAILABLE.name());
                updateSlotStmt.setInt(2, slotId);
                updateSlotStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Rollback error: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Error resetting auto-commit: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Checks in a user against a confirmed reservation:
     * 1. Verifies reservation status is CONFIRMED and current time is before arrival_deadline.
     * 2. Sets arrival_time = now(), updates reservation status to ACTIVE.
     * 3. Updates associated slot status to OCCUPIED.
     *
     * @param reservationId the reservation ID to check in
     * @return true if check-in succeeded, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean checkIn(int reservationId) throws SQLException {
        String querySql = "SELECT slot_id, arrival_deadline, status FROM PARKING_RESERVATIONS WHERE reservation_id = ? FOR UPDATE";
        String updateReservationSql = "UPDATE PARKING_RESERVATIONS SET status = ?, arrival_time = ? WHERE reservation_id = ?";
        String updateSlotSql = "UPDATE PARKING_SLOTS SET status = ? WHERE slot_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            int slotId = -1;
            LocalDateTime now = LocalDateTime.now();

            try (PreparedStatement queryStmt = conn.prepareStatement(querySql)) {
                queryStmt.setInt(1, reservationId);

                try (ResultSet rs = queryStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    String currentStatus = rs.getString("status");
                    if (!ReservationStatus.CONFIRMED.name().equalsIgnoreCase(currentStatus)) {
                        conn.rollback();
                        return false; // Can only check in on CONFIRMED reservation
                    }

                    Timestamp deadlineTs = rs.getTimestamp("arrival_deadline");
                    if (deadlineTs == null || now.isAfter(deadlineTs.toLocalDateTime())) {
                        conn.rollback();
                        return false; // Arrival deadline has passed
                    }

                    slotId = rs.getInt("slot_id");
                }
            }

            // Update reservation to ACTIVE and record arrival_time
            try (PreparedStatement updateResStmt = conn.prepareStatement(updateReservationSql)) {
                updateResStmt.setString(1, ReservationStatus.ACTIVE.name());
                updateResStmt.setTimestamp(2, Timestamp.valueOf(now));
                updateResStmt.setInt(3, reservationId);
                updateResStmt.executeUpdate();
            }

            // Update slot status to OCCUPIED
            try (PreparedStatement updateSlotStmt = conn.prepareStatement(updateSlotSql)) {
                updateSlotStmt.setString(1, SlotStatus.OCCUPIED.name());
                updateSlotStmt.setInt(2, slotId);
                updateSlotStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Rollback error: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Error resetting auto-commit: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Checks out a user from an active reservation:
     * 1. Verifies reservation status is ACTIVE.
     * 2. Updates reservation status to COMPLETED.
     * 3. Updates associated slot status back to AVAILABLE.
     *
     * @param reservationId the reservation ID to check out
     * @return true if check-out succeeded, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean checkOut(int reservationId) throws SQLException {
        String querySql = "SELECT slot_id, status FROM PARKING_RESERVATIONS WHERE reservation_id = ? FOR UPDATE";
        String updateReservationSql = "UPDATE PARKING_RESERVATIONS SET status = ? WHERE reservation_id = ?";
        String updateSlotSql = "UPDATE PARKING_SLOTS SET status = ? WHERE slot_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            int slotId = -1;
            try (PreparedStatement queryStmt = conn.prepareStatement(querySql)) {
                queryStmt.setInt(1, reservationId);

                try (ResultSet rs = queryStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }

                    String currentStatus = rs.getString("status");
                    if (!ReservationStatus.ACTIVE.name().equalsIgnoreCase(currentStatus)) {
                        conn.rollback();
                        return false; // Can only check out an ACTIVE reservation
                    }

                    slotId = rs.getInt("slot_id");
                }
            }

            // Update reservation status to COMPLETED
            try (PreparedStatement updateResStmt = conn.prepareStatement(updateReservationSql)) {
                updateResStmt.setString(1, ReservationStatus.COMPLETED.name());
                updateResStmt.setInt(2, reservationId);
                updateResStmt.executeUpdate();
            }

            // Update slot status back to AVAILABLE
            try (PreparedStatement updateSlotStmt = conn.prepareStatement(updateSlotSql)) {
                updateSlotStmt.setString(1, SlotStatus.AVAILABLE.name());
                updateSlotStmt.setInt(2, slotId);
                updateSlotStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Rollback error: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Error resetting auto-commit: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Automatic Slot Release Engine:
     * Finds all reservations where status = 'CONFIRMED' and arrival_deadline < NOW().
     * Updates PARKING_RESERVATIONS.status to EXPIRED and linked PARKING_SLOTS.status to AVAILABLE.
     *
     * @return count of expired reservations released
     * @throws SQLException if a database error occurs
     */
    public int releaseExpiredReservations() throws SQLException {
        String findExpiredSql = "SELECT reservation_id, slot_id FROM PARKING_RESERVATIONS " +
                                "WHERE status = 'CONFIRMED' AND arrival_deadline < NOW() FOR UPDATE";
        String updateReservationSql = "UPDATE PARKING_RESERVATIONS SET status = ? WHERE reservation_id = ?";
        String updateSlotSql = "UPDATE PARKING_SLOTS SET status = ? WHERE slot_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            List<int[]> expiredPairs = new ArrayList<>(); // [reservationId, slotId]

            try (PreparedStatement findStmt = conn.prepareStatement(findExpiredSql);
                 ResultSet rs = findStmt.executeQuery()) {
                while (rs.next()) {
                    expiredPairs.add(new int[]{rs.getInt("reservation_id"), rs.getInt("slot_id")});
                }
            }

            if (expiredPairs.isEmpty()) {
                conn.commit();
                return 0;
            }

            try (PreparedStatement updateResStmt = conn.prepareStatement(updateReservationSql);
                 PreparedStatement updateSlotStmt = conn.prepareStatement(updateSlotSql)) {

                for (int[] pair : expiredPairs) {
                    int resId = pair[0];
                    int slotId = pair[1];

                    // Set reservation status to EXPIRED
                    updateResStmt.setString(1, ReservationStatus.EXPIRED.name());
                    updateResStmt.setInt(2, resId);
                    updateResStmt.executeUpdate();

                    // Set slot status back to AVAILABLE
                    updateSlotStmt.setString(1, SlotStatus.AVAILABLE.name());
                    updateSlotStmt.setInt(2, slotId);
                    updateSlotStmt.executeUpdate();
                }
            }

            conn.commit();
            return expiredPairs.size();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Rollback error: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    System.err.println("[ReservationDAO] Error resetting auto-commit: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Retrieves all reservations for a specific user to track status and deadlines.
     *
     * @param userId the user ID
     * @return list of reservations for the given user, ordered by reservation_time descending
     * @throws SQLException if a database error occurs
     */
    public List<ParkingReservation> getReservationsByUser(int userId) throws SQLException {
        String sql = "SELECT reservation_id, user_id, slot_id, reservation_time, arrival_deadline, arrival_time, status " +
                     "FROM PARKING_RESERVATIONS " +
                     "WHERE user_id = ? " +
                     "ORDER BY reservation_time DESC";
        List<ParkingReservation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReservation(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves a single reservation by its ID.
     *
     * @param reservationId the reservation ID
     * @return ParkingReservation if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public ParkingReservation getReservationById(int reservationId) throws SQLException {
        String sql = "SELECT reservation_id, user_id, slot_id, reservation_time, arrival_deadline, arrival_time, status " +
                     "FROM PARKING_RESERVATIONS " +
                     "WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, reservationId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all reservations across all users and statuses.
     *
     * @return list of all reservations
     * @throws SQLException if a database error occurs
     */
    public List<ParkingReservation> getAllReservations() throws SQLException {
        String sql = "SELECT reservation_id, user_id, slot_id, reservation_time, arrival_deadline, arrival_time, status " +
                     "FROM PARKING_RESERVATIONS " +
                     "ORDER BY reservation_time DESC";
        List<ParkingReservation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        }
        return list;
    }

    /**
     * Helper to map a ResultSet row into a ParkingReservation object.
     */
    private ParkingReservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Timestamp resTime = rs.getTimestamp("reservation_time");
        Timestamp deadline = rs.getTimestamp("arrival_deadline");
        Timestamp arrivalTime = rs.getTimestamp("arrival_time");

        return new ParkingReservation(
                rs.getInt("reservation_id"),
                rs.getInt("user_id"),
                rs.getInt("slot_id"),
                resTime != null ? resTime.toLocalDateTime() : null,
                deadline != null ? deadline.toLocalDateTime() : null,
                arrivalTime != null ? arrivalTime.toLocalDateTime() : null,
                ReservationStatus.valueOf(rs.getString("status"))
        );
    }
}
