# Smart Parking for College

A console-based Smart Parking Management System designed for college campuses, built using Java and MySQL.

---

## 📌 Phase 1: Project Architecture, Maven & SQL Schema

### 1. Technology Stack
- **Language**: Java 21 (LTS)
- **Build Tool**: Apache Maven
- **Database Driver**: `com.mysql:mysql-connector-j:8.3.0`
- **Database**: MySQL 8.x

### 2. Package Architecture
```
com.college.smartparking
├── config
│   └── DatabaseConnection.java    # JDBC connection manager
├── dao                            # Data Access Objects (CRUD operations)
├── model                          # Entity models & enums
│   ├── ParkingReservation.java
│   ├── ParkingSlot.java
│   ├── ReservationStatus.java
│   ├── SlotStatus.java
│   ├── SlotType.java
│   └── User.java
├── service                        # Business rules & service logic
├── ui                             # Console CLI interface & interactive menus
└── App.java                       # Main application entry point
```

### 3. Database Schema (`smart_parking`)
The application operates on the `smart_parking` MySQL database with three core tables:
- **`USERS`**: Stores user credentials, contact details, and vehicle numbers.
- **`PARKING_SLOTS`**: Stores parking spots categorized by area (`Main Block`, `Hostel Block`), vehicle type (`TWO_WHEELER`, `FOUR_WHEELER`), and slot status (`AVAILABLE`, `RESERVED`, `OCCUPIED`).
- **`PARKING_RESERVATIONS`**: Tracks reservations, deadlines, actual arrivals, and reservation statuses (`CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`, `EXPIRED`) with foreign key constraints.

Includes initial seed data:
- 2 Users
- 6 Parking Slots across Main Block and Hostel Block

---

## 📌 Phase 2: JDBC Connection & Entity Models

### 1. Database Connection (`config/DatabaseConnection.java`)
- Connects to `jdbc:mysql://localhost:3306/smart_parking`.
- Provides static `getConnection()` with clean `SQLException` diagnostics.
- Configurable via environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`) or system properties (`db.url`, `db.user`, `db.password`), falling back to default localhost settings.

### 2. Enums (`model/`)
- **`SlotType`**: `TWO_WHEELER`, `FOUR_WHEELER`
- **`SlotStatus`**: `AVAILABLE`, `RESERVED`, `OCCUPIED`
- **`ReservationStatus`**: `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`, `EXPIRED`

### 3. Entity Models (`model/`)
- **`User`**: Fields for `userId`, `userName`, `email`, `contactInfo`, `vehicleNumber`.
- **`ParkingSlot`**: Fields for `slotId`, `slotNumber`, `parkingArea`, `slotType`, `status`.
- **`ParkingReservation`**: Fields for `reservationId`, `userId`, `slotId`, `reservationTime` (`LocalDateTime`), `arrivalDeadline` (`LocalDateTime`), `arrivalTime` (`LocalDateTime`), `status`.
- All entities provide default constructors, parameterized constructors (with and without ID), getters, setters, and console table row `toString()` formatting with header helpers.

---

## 🚀 Getting Started

### Database Setup
Execute `schema.sql` in MySQL:
```bash
mysql -u <username> -p < schema.sql
```

### Build & Run
Compile using Maven:
```bash
mvn clean compile
```

Run the application:
```bash
mvn exec:java -Dexec.mainClass="com.college.smartparking.App"
```
