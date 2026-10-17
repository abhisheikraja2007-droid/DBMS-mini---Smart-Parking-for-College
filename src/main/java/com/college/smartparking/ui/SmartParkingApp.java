package com.college.smartparking.ui;

import com.college.smartparking.dao.ParkingSlotDAO;
import com.college.smartparking.dao.ReservationDAO;
import com.college.smartparking.dao.UserDAO;
import com.college.smartparking.model.ParkingReservation;
import com.college.smartparking.model.ParkingSlot;
import com.college.smartparking.model.ReservationStatus;
import com.college.smartparking.model.SlotStatus;
import com.college.smartparking.model.SlotType;
import com.college.smartparking.model.User;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Interactive Console Application for Smart Parking for College.
 * Provides a menu-driven interface integrating UserDAO, ParkingSlotDAO, and ReservationDAO.
 */
public class SmartParkingApp {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserDAO userDAO;
    private final ParkingSlotDAO slotDAO;
    private final ReservationDAO reservationDAO;
    private final Scanner scanner;
    private final ScheduledExecutorService scheduler;

    public SmartParkingApp() {
        this.userDAO = new UserDAO();
        this.slotDAO = new ParkingSlotDAO();
        this.reservationDAO = new ReservationDAO();
        this.scanner = new Scanner(System.in);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Background-Expiry-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public static void main(String[] args) {
        SmartParkingApp app = new SmartParkingApp();
        app.start();
    }

    /**
     * Initializes background scheduler and enters the main interactive loop.
     */
    public void start() {
        printBanner();
        com.college.smartparking.config.DatabaseConnection.initializeDatabaseIfMissing();
        startBackgroundExpiryEngine();

        // Perform initial check on startup
        checkAndAlertExpiry();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readIntInput("Select an option (0-8): ", -1);

            switch (choice) {
                case 1 -> handleRegisterUser();
                case 2 -> handleManageParkingSlots();
                case 3 -> handleViewAvailableSlots();
                case 4 -> handleReserveSlot();
                case 5 -> handleViewAndCancelReservations();
                case 6 -> handleCheckIn();
                case 7 -> handleCheckOut();
                case 8 -> handleManualExpiryAndDashboard();
                case 0 -> {
                    running = false;
                    shutdown();
                }
                default -> System.out.println("❌ Invalid choice. Please enter a number between 0 and 8.");
            }

            if (running) {
                System.out.println("\nPress ENTER to continue...");
                scanner.nextLine();
            }
        }
    }

    /**
     * Background scheduler executing releaseExpiredReservations() every 60 seconds.
     */
    private void startBackgroundExpiryEngine() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int released = reservationDAO.releaseExpiredReservations();
                if (released > 0) {
                    System.out.println("\n" + buildExpirationAlertBanner(released));
                    System.out.print("Select an option (0-8): ");
                }
            } catch (Exception e) {
                // Background exception suppressed to prevent console disruption
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * Helper to run expiry check synchronously before critical operations.
     */
    private synchronized int checkAndAlertExpiry() {
        try {
            int released = reservationDAO.releaseExpiredReservations();
            if (released > 0) {
                System.out.println(buildExpirationAlertBanner(released));
            }
            return released;
        } catch (SQLException e) {
            System.err.println("⚠️ Warning: Failed to run automatic expiry check: " + e.getMessage());
            return 0;
        }
    }

    private String buildExpirationAlertBanner(int count) {
        return """
               ========================================================================
               🚨 [EXPIRATION ALERT] AUTOMATIC SLOT RELEASE ENGINE
               ------------------------------------------------------------------------
               ⚠️  Detected and released %d overdue reservation(s)!
               🔄  Associated parking slot(s) have been restored to 'AVAILABLE' status.
               ========================================================================""".formatted(count);
    }

    private void printBanner() {
        System.out.println("""
               ========================================================================
               🚗  SMART PARKING MANAGEMENT SYSTEM — COLLEGE CAMPUS  🅿️
               ========================================================================
               🚀 Real-time Slot Availability, Reservation Engine & Campus Monitoring
               ========================================================================""");
    }

    private void printMainMenu() {
        System.out.println("""
               
               ------------------------------ MAIN MENU -------------------------------
               [1] 👤 Register User (Student / Staff)
               [2] 🔧 Add / Manage Parking Slots (Admin)
               [3] 🔍 View Available Parking Slots (Filtered / Campus-wide)
               [4] 📅 Reserve a Parking Slot
               [5] 📋 View & Cancel My Reservations
               [6] 🟢 Vehicle Check-In (Arrival)
               [7] 🔴 Vehicle Check-Out (Departure)
               [8] 📊 Run Manual Expiry Check & View Campus Dashboard
               [0] 🚪 Exit Application
               ------------------------------------------------------------------------""");
    }

    // ========================================================================
    // Option 1: Register User
    // ========================================================================
    private void handleRegisterUser() {
        System.out.println("\n--- [1] REGISTER NEW USER (Student / Staff) ---");

        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine().trim();
        while (name.isEmpty()) {
            System.out.print("Name cannot be empty. Enter Full Name: ");
            name = scanner.nextLine().trim();
        }

        System.out.print("Enter Email Address: ");
        String email = scanner.nextLine().trim();
        while (email.isEmpty()) {
            System.out.print("Email cannot be empty. Enter Email: ");
            email = scanner.nextLine().trim();
        }

        System.out.print("Enter Contact Number: ");
        String contact = scanner.nextLine().trim();
        while (contact.isEmpty()) {
            System.out.print("Contact cannot be empty. Enter Contact: ");
            contact = scanner.nextLine().trim();
        }

        System.out.print("Enter Vehicle Registration Number: ");
        String vehicleNumber = scanner.nextLine().trim().toUpperCase();
        while (vehicleNumber.isEmpty()) {
            System.out.print("Vehicle number cannot be empty. Enter Vehicle Number: ");
            vehicleNumber = scanner.nextLine().trim().toUpperCase();
        }

        try {
            User user = new User(name, email, contact, vehicleNumber);
            int userId = userDAO.registerUser(user);

            if (userId != -1) {
                System.out.println("""
                       
                       ========================================================================
                       ✅ [USER REGISTRATION SUCCESSFUL]
                       ------------------------------------------------------------------------
                       🆔 User ID        : %d
                       👤 Name           : %s
                       📧 Email          : %s
                       📞 Contact        : %s
                       🚘 Vehicle Number : %s
                       ========================================================================""".formatted(
                               userId, name, email, contact, vehicleNumber));
            } else {
                System.out.println("❌ Failed to register user. Please try again.");
            }
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                System.out.println("❌ Registration Failed: A user with that Email or Vehicle Number already exists!");
            } else {
                System.out.println("❌ Database Error: " + e.getMessage());
            }
        }
    }

    // ========================================================================
    // Option 2: Add / Manage Parking Slots (Admin)
    // ========================================================================
    private void handleManageParkingSlots() {
        System.out.println("\n--- [2] ADD / MANAGE PARKING SLOTS (Admin) ---");
        System.out.println("[1] Add New Parking Slot");
        System.out.println("[2] Update Slot Status Manually");
        System.out.println("[0] Back to Main Menu");

        int choice = readIntInput("Select sub-option (0-2): ", -1);

        switch (choice) {
            case 1 -> {
                System.out.print("Enter Slot Number (e.g., 'MB-2W-03', 'HB-4W-03'): ");
                String slotNumber = scanner.nextLine().trim().toUpperCase();

                System.out.print("Enter Parking Area (e.g., 'Main Block', 'Hostel Block', 'Library'): ");
                String area = scanner.nextLine().trim();

                System.out.println("Select Vehicle Slot Type:");
                System.out.println("  1. TWO_WHEELER");
                System.out.println("  2. FOUR_WHEELER");
                int typeChoice = readIntInput("Select Type (1 or 2): ", 1);
                SlotType slotType = (typeChoice == 2) ? SlotType.FOUR_WHEELER : SlotType.TWO_WHEELER;

                try {
                    ParkingSlot slot = new ParkingSlot(slotNumber, area, slotType, SlotStatus.AVAILABLE);
                    boolean added = slotDAO.addParkingSlot(slot);
                    if (added) {
                        System.out.println("✅ Parking slot " + slotNumber + " added successfully with ID: " + slot.getSlotId());
                    } else {
                        System.out.println("❌ Failed to add parking slot.");
                    }
                } catch (SQLException e) {
                    System.out.println("❌ Database Error: " + e.getMessage());
                }
            }
            case 2 -> {
                int slotId = readIntInput("Enter Slot ID to update: ", -1);
                try {
                    ParkingSlot slot = slotDAO.getSlotById(slotId);
                    if (slot == null) {
                        System.out.println("❌ Parking slot with ID " + slotId + " not found.");
                        return;
                    }
                    System.out.println("Current Slot Status: " + slot.getStatus());
                    System.out.println("Select New Status:");
                    System.out.println("  1. AVAILABLE");
                    System.out.println("  2. RESERVED");
                    System.out.println("  3. OCCUPIED");
                    int statusChoice = readIntInput("Choice (1-3): ", 1);
                    SlotStatus newStatus = switch (statusChoice) {
                        case 2 -> SlotStatus.RESERVED;
                        case 3 -> SlotStatus.OCCUPIED;
                        default -> SlotStatus.AVAILABLE;
                    };

                    boolean updated = slotDAO.updateSlotStatus(slotId, newStatus);
                    if (updated) {
                        System.out.println("✅ Slot ID " + slotId + " status updated to " + newStatus);
                    } else {
                        System.out.println("❌ Failed to update slot status.");
                    }
                } catch (SQLException e) {
                    System.out.println("❌ Database Error: " + e.getMessage());
                }
            }
            case 0 -> System.out.println("Returning to main menu.");
            default -> System.out.println("Invalid option.");
        }
    }

    // ========================================================================
    // Option 3: View Available Parking Slots
    // ========================================================================
    private void handleViewAvailableSlots() {
        System.out.println("\n--- [3] VIEW AVAILABLE PARKING SLOTS ---");
        // Always trigger automatic expiry check before showing slots
        checkAndAlertExpiry();

        System.out.println("[1] View All Available Slots Across Campus");
        System.out.println("[2] Filter by Area and Vehicle Type");
        int subChoice = readIntInput("Select viewing option (1-2): ", 1);

        try {
            List<ParkingSlot> slots;
            if (subChoice == 2) {
                System.out.print("Enter Parking Area (e.g., 'Main Block', 'Hostel Block'): ");
                String area = scanner.nextLine().trim();

                System.out.println("Select Vehicle Type:");
                System.out.println("  1. TWO_WHEELER");
                System.out.println("  2. FOUR_WHEELER");
                int typeChoice = readIntInput("Select Type (1 or 2): ", 1);
                SlotType type = (typeChoice == 2) ? SlotType.FOUR_WHEELER : SlotType.TWO_WHEELER;

                slots = slotDAO.getAvailableSlots(area, type);
                System.out.println("\n🔍 Available Slots in [" + area + "] for [" + type.name() + "]:");
            } else {
                slots = slotDAO.getAllAvailableSlots();
                System.out.println("\n🔍 All Available Slots Across Campus:");
            }

            if (slots.isEmpty()) {
                System.out.println("⚠️ No available parking slots found for the specified criteria.");
            } else {
                System.out.println(ParkingSlot.getTableHeader());
                for (ParkingSlot s : slots) {
                    System.out.println(s);
                }
                System.out.println(ParkingSlot.getTableDivider());
                System.out.println("Total Available: " + slots.size() + " slot(s)");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error retrieving available slots: " + e.getMessage());
        }
    }

    // ========================================================================
    // Option 4: Reserve a Parking Slot
    // ========================================================================
    private void handleReserveSlot() {
        System.out.println("\n--- [4] RESERVE A PARKING SLOT ---");
        checkAndAlertExpiry();

        int userId = readIntInput("Enter your User ID: ", -1);
        try {
            User user = userDAO.getUserById(userId);
            if (user == null) {
                System.out.println("❌ User ID " + userId + " does not exist. Please register first.");
                return;
            }
            System.out.println("👤 Reserving for: " + user.getUserName() + " (Vehicle: " + user.getVehicleNumber() + ")");

            int slotId = readIntInput("Enter Slot ID to reserve: ", -1);
            ParkingSlot slot = slotDAO.getSlotById(slotId);
            if (slot == null) {
                System.out.println("❌ Slot ID " + slotId + " does not exist.");
                return;
            }

            if (slot.getStatus() != SlotStatus.AVAILABLE) {
                System.out.println("❌ Slot " + slot.getSlotNumber() + " is currently " + slot.getStatus() + ". Only AVAILABLE slots can be reserved.");
                return;
            }

            int minutes = readIntInput("Enter Arrival Window in minutes (e.g., 15, 30, 45): ", 30);
            if (minutes <= 0) minutes = 30;

            int resId = reservationDAO.createReservation(userId, slotId, minutes);
            if (resId != -1) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime deadline = now.plusMinutes(minutes);

                System.out.println("""
                       
                       ========================================================================
                       🎉 [RESERVATION CONFIRMATION]
                       ------------------------------------------------------------------------
                       🎟️ Reservation ID : #%d
                       🆔 User ID        : %d (%s)
                       🅿️ Slot ID        : %d (Slot No: %s | %s)
                       📍 Parking Area   : %s
                       ⏰ Reserved At    : %s
                       ⏳ Arrival Window : %d minutes
                       🚨 Arrival Deadline : %s
                       ------------------------------------------------------------------------
                       ℹ️ Note: If you do not check in before the arrival deadline, the
                          reservation will be marked EXPIRED and the slot will be released!
                       ========================================================================""".formatted(
                               resId, userId, user.getUserName(), slotId, slot.getSlotNumber(),
                               slot.getSlotType().name(), slot.getParkingArea(),
                               now.format(FORMATTER), minutes, deadline.format(FORMATTER)));
            } else {
                System.out.println("❌ Reservation could not be completed. The slot may have been taken by another user.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database Error during reservation: " + e.getMessage());
        }
    }

    // ========================================================================
    // Option 5: View & Cancel My Reservations
    // ========================================================================
    private void handleViewAndCancelReservations() {
        System.out.println("\n--- [5] VIEW & CANCEL MY RESERVATIONS ---");
        checkAndAlertExpiry();

        int userId = readIntInput("Enter your User ID: ", -1);
        try {
            User user = userDAO.getUserById(userId);
            if (user == null) {
                System.out.println("❌ User ID " + userId + " does not exist.");
                return;
            }

            List<ParkingReservation> reservations = reservationDAO.getReservationsByUser(userId);
            if (reservations.isEmpty()) {
                System.out.println("ℹ️ No reservations found for user: " + user.getUserName());
                return;
            }

            System.out.println("\n📋 Reservations for " + user.getUserName() + ":");
            System.out.println(ParkingReservation.getTableHeader());
            for (ParkingReservation r : reservations) {
                System.out.println(r);
            }
            System.out.println(ParkingReservation.getTableDivider());

            System.out.print("\nDo you want to cancel a CONFIRMED reservation? (y/n): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("y") || ans.equals("yes")) {
                int cancelResId = readIntInput("Enter Reservation ID to cancel: ", -1);
                boolean success = reservationDAO.cancelReservation(cancelResId, userId);
                if (success) {
                    System.out.println("✅ Reservation #" + cancelResId + " successfully CANCELLED. Associated parking slot has been released.");
                } else {
                    System.out.println("❌ Cancellation failed. Reservation must exist, belong to you, and be in CONFIRMED status.");
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Option 6: Vehicle Check-In
    // ========================================================================
    private void handleCheckIn() {
        System.out.println("\n--- [6] VEHICLE CHECK-IN ---");
        // Expiry check run first
        checkAndAlertExpiry();

        int reservationId = readIntInput("Enter Reservation ID for Check-In: ", -1);
        try {
            ParkingReservation res = reservationDAO.getReservationById(reservationId);
            if (res == null) {
                System.out.println("❌ Reservation #" + reservationId + " not found.");
                return;
            }

            if (res.getStatus() == ReservationStatus.EXPIRED) {
                System.out.println("""
                       ========================================================================
                       🚨 [EXPIRATION ALERT] CHECK-IN REJECTED
                       ------------------------------------------------------------------------
                       ⚠️ Reservation #%d has EXPIRED because the arrival deadline (%s)
                          has passed. The slot was already released back to the campus pool.
                       ========================================================================""".formatted(
                               reservationId, res.getArrivalDeadline() != null ? res.getArrivalDeadline().format(FORMATTER) : "N/A"));
                return;
            }

            if (res.getStatus() == ReservationStatus.ACTIVE) {
                System.out.println("ℹ️ Reservation #" + reservationId + " is ALREADY checked in and ACTIVE.");
                return;
            }

            if (res.getStatus() != ReservationStatus.CONFIRMED) {
                System.out.println("❌ Cannot check in. Reservation status is: " + res.getStatus());
                return;
            }

            // Check if deadline has passed just now
            if (res.getArrivalDeadline() != null && LocalDateTime.now().isAfter(res.getArrivalDeadline())) {
                checkAndAlertExpiry();
                System.out.println("🚨 [EXPIRATION ALERT] Arrival deadline has passed! Check-in rejected.");
                return;
            }

            boolean checkedIn = reservationDAO.checkIn(reservationId);
            if (checkedIn) {
                System.out.println("""
                       ========================================================================
                       🟢 [CHECK-IN SUCCESSFUL]
                       ------------------------------------------------------------------------
                       🎟️ Reservation ID : #%d
                       🅿️ Slot ID        : %d
                       ⏰ Check-In Time  : %s
                       📊 Slot Status    : OCCUPIED
                       ========================================================================""".formatted(
                               reservationId, res.getSlotId(), LocalDateTime.now().format(FORMATTER)));
            } else {
                System.out.println("❌ Check-in failed. Please verify reservation status and deadline.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Option 7: Vehicle Check-Out
    // ========================================================================
    private void handleCheckOut() {
        System.out.println("\n--- [7] VEHICLE CHECK-OUT ---");

        int reservationId = readIntInput("Enter Reservation ID for Check-Out: ", -1);
        try {
            ParkingReservation res = reservationDAO.getReservationById(reservationId);
            if (res == null) {
                System.out.println("❌ Reservation #" + reservationId + " not found.");
                return;
            }

            if (res.getStatus() != ReservationStatus.ACTIVE) {
                System.out.println("❌ Cannot check out. Reservation status is '" + res.getStatus() + "' (Only ACTIVE reservations can be checked out).");
                return;
            }

            boolean checkedOut = reservationDAO.checkOut(reservationId);
            if (checkedOut) {
                System.out.println("""
                       ========================================================================
                       🔴 [CHECK-OUT SUCCESSFUL]
                       ------------------------------------------------------------------------
                       🎟️ Reservation ID : #%d
                       🅿️ Slot ID        : %d
                       ⏰ Departure Time : %s
                       📊 Slot Status    : AVAILABLE (Freed for other users)
                       ========================================================================""".formatted(
                               reservationId, res.getSlotId(), LocalDateTime.now().format(FORMATTER)));
            } else {
                System.out.println("❌ Check-out failed. Please try again.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    // ========================================================================
    // Option 8: Manual Expiry Check & Campus Dashboard
    // ========================================================================
    private void handleManualExpiryAndDashboard() {
        System.out.println("\n--- [8] MANUAL EXPIRY CHECK & CAMPUS DASHBOARD ---");

        try {
            int released = reservationDAO.releaseExpiredReservations();
            if (released > 0) {
                System.out.println(buildExpirationAlertBanner(released));
            } else {
                System.out.println("ℹ️ Manual Expiry Check: No overdue reservations found. All slots current.");
            }

            // Generate Dashboard
            List<ParkingSlot> allSlots = slotDAO.getAllSlots();
            int total = allSlots.size();
            long available = allSlots.stream().filter(s -> s.getStatus() == SlotStatus.AVAILABLE).count();
            long reserved = allSlots.stream().filter(s -> s.getStatus() == SlotStatus.RESERVED).count();
            long occupied = allSlots.stream().filter(s -> s.getStatus() == SlotStatus.OCCUPIED).count();

            System.out.println("""
                   
                   ========================================================================
                   📊 CAMPUS PARKING REAL-TIME DASHBOARD
                   ========================================================================
                   Total Campus Slots    : %d
                   🟢 AVAILABLE Slots   : %d  (%.1f%%)
                   🟡 RESERVED Slots    : %d  (%.1f%%)
                   🔴 OCCUPIED Slots    : %d  (%.1f%%)
                   ------------------------------------------------------------------------""".formatted(
                           total,
                           available, total > 0 ? (available * 100.0 / total) : 0,
                           reserved, total > 0 ? (reserved * 100.0 / total) : 0,
                           occupied, total > 0 ? (occupied * 100.0 / total) : 0));

            System.out.println("\n📋 Complete Campus Slots Overview:");
            System.out.println(ParkingSlot.getTableHeader());
            for (ParkingSlot s : allSlots) {
                System.out.println(s);
            }
            System.out.println(ParkingSlot.getTableDivider());

        } catch (SQLException e) {
            System.out.println("❌ Database Error generating dashboard: " + e.getMessage());
        }
    }

    // ========================================================================
    // Utilities & Clean Shutdown
    // ========================================================================
    private int readIntInput(String prompt, int defaultVal) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void shutdown() {
        System.out.println("\nShutting down Smart Parking background services...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        scanner.close();
        com.college.smartparking.config.DatabaseConnection.shutdownCleanup();
        System.out.println("👋 Thank you for using Smart Parking for College. Goodbye!");
    }
}
