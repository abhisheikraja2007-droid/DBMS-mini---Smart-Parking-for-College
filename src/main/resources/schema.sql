-- ========================================================
-- Smart Parking for College Database Schema
-- Database: smart_parking
-- ========================================================

CREATE DATABASE IF NOT EXISTS smart_parking;
USE smart_parking;

-- Drop child tables first if existing to avoid FK constraint issues
DROP TABLE IF EXISTS PARKING_RESERVATIONS;
DROP TABLE IF EXISTS PARKING_SLOTS;
DROP TABLE IF EXISTS USERS;

-- 1. USERS Table
CREATE TABLE USERS (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    contact_info VARCHAR(20) NOT NULL,
    vehicle_number VARCHAR(20) UNIQUE NOT NULL
);

-- 2. PARKING_SLOTS Table
CREATE TABLE PARKING_SLOTS (
    slot_id INT AUTO_INCREMENT PRIMARY KEY,
    slot_number VARCHAR(20) UNIQUE NOT NULL,
    parking_area VARCHAR(50) NOT NULL,
    slot_type ENUM('TWO_WHEELER', 'FOUR_WHEELER') NOT NULL,
    status ENUM('AVAILABLE', 'RESERVED', 'OCCUPIED') DEFAULT 'AVAILABLE'
);

-- 3. PARKING_RESERVATIONS Table
CREATE TABLE PARKING_RESERVATIONS (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    slot_id INT NOT NULL,
    reservation_time DATETIME NOT NULL,
    arrival_deadline DATETIME NOT NULL,
    arrival_time DATETIME NULL,
    status ENUM('CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED') DEFAULT 'CONFIRMED',
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES USERS(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_reservation_slot FOREIGN KEY (slot_id) REFERENCES PARKING_SLOTS(slot_id) ON DELETE CASCADE
);

-- ========================================================
-- Sample INSERT statements for immediate testing
-- ========================================================

-- 2 Sample Users
INSERT INTO USERS (user_name, email, contact_info, vehicle_number) VALUES
('John Doe', 'john.doe@college.edu', '9876543210', 'TN09AB1234'),
('Alice Smith', 'alice.smith@college.edu', '9123456780', 'TN07CD5678');

-- 6 Sample Parking Slots across Main Block and Hostel Block
INSERT INTO PARKING_SLOTS (slot_number, parking_area, slot_type, status) VALUES
('MB-2W-01', 'Main Block', 'TWO_WHEELER', 'AVAILABLE'),
('MB-2W-02', 'Main Block', 'TWO_WHEELER', 'AVAILABLE'),
('MB-4W-01', 'Main Block', 'FOUR_WHEELER', 'AVAILABLE'),
('HB-2W-01', 'Hostel Block', 'TWO_WHEELER', 'AVAILABLE'),
('HB-4W-01', 'Hostel Block', 'FOUR_WHEELER', 'AVAILABLE'),
('HB-4W-02', 'Hostel Block', 'FOUR_WHEELER', 'AVAILABLE');
