package com.college.smartparking.service;

import com.college.smartparking.dao.ParkingSlotDAO;
import com.college.smartparking.dao.ReservationDAO;
import com.college.smartparking.dao.UserDAO;
import com.college.smartparking.model.ParkingReservation;
import com.college.smartparking.model.ParkingSlot;
import com.college.smartparking.model.SlotStatus;
import com.college.smartparking.model.SlotType;
import com.college.smartparking.model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Service Layer coordinating operations across UserDAO, ParkingSlotDAO, and ReservationDAO.
 */
public class ParkingService {

    private final UserDAO userDAO;
    private final ParkingSlotDAO slotDAO;
    private final ReservationDAO reservationDAO;

    public ParkingService() {
        this.userDAO = new UserDAO();
        this.slotDAO = new ParkingSlotDAO();
        this.reservationDAO = new ReservationDAO();
    }

    public ParkingService(UserDAO userDAO, ParkingSlotDAO slotDAO, ReservationDAO reservationDAO) {
        this.userDAO = userDAO;
        this.slotDAO = slotDAO;
        this.reservationDAO = reservationDAO;
    }

    // User Operations
    public int registerUser(User user) throws SQLException {
        return userDAO.registerUser(user);
    }

    public User getUserById(int userId) throws SQLException {
        return userDAO.getUserById(userId);
    }

    public User getUserByVehicleNumber(String vehicleNumber) throws SQLException {
        return userDAO.getUserByVehicleNumber(vehicleNumber);
    }

    public List<User> getAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }

    // Slot Operations
    public boolean addParkingSlot(ParkingSlot slot) throws SQLException {
        return slotDAO.addParkingSlot(slot);
    }

    public List<ParkingSlot> getAvailableSlots(String parkingArea, SlotType slotType) throws SQLException {
        reservationDAO.releaseExpiredReservations(); // Auto-release overdue slots
        return slotDAO.getAvailableSlots(parkingArea, slotType);
    }

    public List<ParkingSlot> getAllAvailableSlots() throws SQLException {
        reservationDAO.releaseExpiredReservations(); // Auto-release overdue slots
        return slotDAO.getAllAvailableSlots();
    }

    public List<ParkingSlot> getAllSlots() throws SQLException {
        return slotDAO.getAllSlots();
    }

    public ParkingSlot getSlotById(int slotId) throws SQLException {
        return slotDAO.getSlotById(slotId);
    }

    public boolean updateSlotStatus(int slotId, SlotStatus status) throws SQLException {
        return slotDAO.updateSlotStatus(slotId, status);
    }

    // Reservation Operations
    public int createReservation(int userId, int slotId, int deadlineMinutes) throws SQLException {
        reservationDAO.releaseExpiredReservations();
        return reservationDAO.createReservation(userId, slotId, deadlineMinutes);
    }

    public boolean cancelReservation(int reservationId, int userId) throws SQLException {
        return reservationDAO.cancelReservation(reservationId, userId);
    }

    public boolean checkIn(int reservationId) throws SQLException {
        reservationDAO.releaseExpiredReservations();
        return reservationDAO.checkIn(reservationId);
    }

    public boolean checkOut(int reservationId) throws SQLException {
        return reservationDAO.checkOut(reservationId);
    }

    public int releaseExpiredReservations() throws SQLException {
        return reservationDAO.releaseExpiredReservations();
    }

    public List<ParkingReservation> getReservationsByUser(int userId) throws SQLException {
        reservationDAO.releaseExpiredReservations();
        return reservationDAO.getReservationsByUser(userId);
    }

    public ParkingReservation getReservationById(int reservationId) throws SQLException {
        return reservationDAO.getReservationById(reservationId);
    }
}
