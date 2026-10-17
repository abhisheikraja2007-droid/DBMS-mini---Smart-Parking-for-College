package com.college.smartparking.dao;

import com.college.smartparking.config.DatabaseConnection;
import com.college.smartparking.model.ParkingSlot;
import com.college.smartparking.model.SlotStatus;
import com.college.smartparking.model.SlotType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for PARKING_SLOTS table operations.
 * Implements prepared statements and try-with-resources to prevent SQL injection and resource leaks.
 */
public class ParkingSlotDAO {

    /**
     * Inserts a new parking slot into PARKING_SLOTS table.
     *
     * @param slot the ParkingSlot to add
     * @return true if inserted successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean addParkingSlot(ParkingSlot slot) throws SQLException {
        String sql = "INSERT INTO PARKING_SLOTS (slot_number, parking_area, slot_type, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, slot.getSlotNumber());
            pstmt.setString(2, slot.getParkingArea());
            pstmt.setString(3, slot.getSlotType().name());
            pstmt.setString(4, slot.getStatus() != null ? slot.getStatus().name() : SlotStatus.AVAILABLE.name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        slot.setSlotId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Queries available slots (status = 'AVAILABLE') filtered by parking area and slot type.
     *
     * @param parkingArea the area to filter by (e.g. 'Main Block', 'Hostel Block')
     * @param slotType    the vehicle type (TWO_WHEELER or FOUR_WHEELER)
     * @return list of matching available parking slots
     * @throws SQLException if a database error occurs
     */
    public List<ParkingSlot> getAvailableSlots(String parkingArea, SlotType slotType) throws SQLException {
        String sql = "SELECT slot_id, slot_number, parking_area, slot_type, status " +
                     "FROM PARKING_SLOTS " +
                     "WHERE status = 'AVAILABLE' AND parking_area = ? AND slot_type = ? " +
                     "ORDER BY slot_id";
        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, parkingArea);
            pstmt.setString(2, slotType.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    slots.add(mapResultSetToSlot(rs));
                }
            }
        }
        return slots;
    }

    /**
     * Returns all AVAILABLE slots across campus regardless of area or type.
     *
     * @return list of all currently available parking slots
     * @throws SQLException if a database error occurs
     */
    public List<ParkingSlot> getAllAvailableSlots() throws SQLException {
        String sql = "SELECT slot_id, slot_number, parking_area, slot_type, status " +
                     "FROM PARKING_SLOTS " +
                     "WHERE status = 'AVAILABLE' " +
                     "ORDER BY parking_area, slot_number";
        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                slots.add(mapResultSetToSlot(rs));
            }
        }
        return slots;
    }

    /**
     * Updates the status column of a specific parking slot.
     *
     * @param slotId    the ID of the slot to update
     * @param newStatus the new SlotStatus
     * @return true if updated successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean updateSlotStatus(int slotId, SlotStatus newStatus) throws SQLException {
        String sql = "UPDATE PARKING_SLOTS SET status = ? WHERE slot_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, slotId);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Returns all slots to display overall campus availability.
     *
     * @return list of all parking slots
     * @throws SQLException if a database error occurs
     */
    public List<ParkingSlot> getAllSlots() throws SQLException {
        String sql = "SELECT slot_id, slot_number, parking_area, slot_type, status " +
                     "FROM PARKING_SLOTS " +
                     "ORDER BY parking_area, slot_number";
        List<ParkingSlot> slots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                slots.add(mapResultSetToSlot(rs));
            }
        }
        return slots;
    }

    /**
     * Fetches a single parking slot by its primary key (slot_id).
     *
     * @param slotId the ID of the slot
     * @return ParkingSlot if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public ParkingSlot getSlotById(int slotId) throws SQLException {
        String sql = "SELECT slot_id, slot_number, parking_area, slot_type, status " +
                     "FROM PARKING_SLOTS " +
                     "WHERE slot_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, slotId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSlot(rs);
                }
            }
        }
        return null;
    }

    /**
     * Fetches a single parking slot by its slot_number (e.g., 'MB-2W-01').
     *
     * @param slotNumber the slot number to search
     * @return ParkingSlot if found, null otherwise
     * @throws SQLException if a database error occurs
     */
    public ParkingSlot getSlotByNumber(String slotNumber) throws SQLException {
        String sql = "SELECT slot_id, slot_number, parking_area, slot_type, status " +
                     "FROM PARKING_SLOTS " +
                     "WHERE slot_number = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, slotNumber);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSlot(rs);
                }
            }
        }
        return null;
    }

    /**
     * Helper to map a ResultSet row into a ParkingSlot object.
     */
    private ParkingSlot mapResultSetToSlot(ResultSet rs) throws SQLException {
        return new ParkingSlot(
                rs.getInt("slot_id"),
                rs.getString("slot_number"),
                rs.getString("parking_area"),
                SlotType.valueOf(rs.getString("slot_type")),
                SlotStatus.valueOf(rs.getString("status"))
        );
    }
}
