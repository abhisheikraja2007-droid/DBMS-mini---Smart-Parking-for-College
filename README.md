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

## 📌 Phase 3: User & Parking Slot DAOs

### 1. `dao/UserDAO.java`
- `int registerUser(User user)`: Inserts a new user into `USERS` and extracts the generated ID via `Statement.RETURN_GENERATED_KEYS`.
- `User getUserById(int userId)`: Queries a user record by primary key.
- `User getUserByVehicleNumber(String vehicleNumber)`: Queries a user record by unique vehicle number.
- `User getUserByEmail(String email)`: Helper to check/fetch user by email.
- `List<User> getAllUsers()`: Lists all registered campus users.

### 2. `dao/ParkingSlotDAO.java`
- `boolean addParkingSlot(ParkingSlot slot)`: Inserts a new parking spot into `PARKING_SLOTS`.
- `List<ParkingSlot> getAvailableSlots(String parkingArea, SlotType slotType)`: Queries `AVAILABLE` slots filtered by campus area and vehicle type.
- `List<ParkingSlot> getAllAvailableSlots()`: Overloaded method retrieving all currently available spots campus-wide.
- `boolean updateSlotStatus(int slotId, SlotStatus newStatus)`: Modifies the status of a specific slot (`AVAILABLE`, `RESERVED`, `OCCUPIED`).
- `List<ParkingSlot> getAllSlots()`: Returns complete inventory of slots across all areas and statuses.
- `ParkingSlot getSlotById(int slotId)` & `ParkingSlot getSlotByNumber(String slotNumber)`: Slot lookup helpers.

All DAO operations utilize `PreparedStatement` and `try-with-resources` ensuring protection against SQL injection and avoiding database connection leaks.

---

## 📌 Phase 4: Transactional Reservation, Check-In/Out & Auto-Expiry Engine

### `dao/ReservationDAO.java`
All methods modifying multiple tables run under explicit JDBC transactions (`conn.setAutoCommit(false)`, `commit()`, and `rollback()` in catch):
- **`int createReservation(int userId, int slotId, int deadlineMinutes)`**:
  - Validates slot availability with row-level lock (`SELECT ... FOR UPDATE`).
  - Sets `reservation_time = now()` and `arrival_deadline = now() + deadlineMinutes`.
  - Atomically creates `CONFIRMED` reservation and flips slot status to `RESERVED`.
  - Returns generated `reservation_id`.
- **`boolean cancelReservation(int reservationId, int userId)`**:
  - Verifies user ownership and that status is `CONFIRMED`.
  - Atomically marks reservation `CANCELLED` and releases slot back to `AVAILABLE`.
- **`boolean checkIn(int reservationId)`**:
  - Verifies reservation is `CONFIRMED` and current time is before `arrival_deadline`.
  - Records `arrival_time = now()`, changes reservation status to `ACTIVE`, and marks slot `OCCUPIED`.
- **`boolean checkOut(int reservationId)`**:
  - Verifies reservation is `ACTIVE`.
  - Marks reservation `COMPLETED` and frees slot back to `AVAILABLE`.
- **`int releaseExpiredReservations()` (Auto-Expiry Engine)**:
  - Scans for reservations where `status = 'CONFIRMED'` and `arrival_deadline < NOW()`.
  - Batch/transactionally transitions them to `EXPIRED` and frees corresponding slots to `AVAILABLE`.
  - Returns the count of released slots for notification alerts.
- **`List<ParkingReservation> getReservationsByUser(int userId)`**:
  - Retrieves chronological history of reservations for a user.

---

## 📌 Phase 5: Interactive Menu-Driven Console Interface & Alerts

### `ui/SmartParkingApp.java`
Main console application tying all components together with an interactive `Scanner`-based CLI:

1. **Automatic Expiry Hook & Background Scheduler**:
   - `ScheduledExecutorService` runs in the background every 60 seconds executing `reservationDAO.releaseExpiredReservations()`.
   - Also automatically invoked before querying available slots and prior to processing check-ins.
   - When overdue reservations are detected and released, a prominent **`[EXPIRATION ALERT]`** banner is printed to the console.

2. **Menu Options**:
   - **`[1] Register User (Student / Staff)`**: Collects name, email, contact, and vehicle number; returns newly generated `User ID`.
   - **`[2] Add / Manage Parking Slots (Admin)`**: Submenu to insert new parking spots across campus or manually update slot statuses (`AVAILABLE`, `RESERVED`, `OCCUPIED`).
   - **`[3] View Available Parking Slots`**: Allows filtering by Campus Area (`Main Block`, `Hostel Block`, etc.) and Vehicle Type (`TWO_WHEELER`, `FOUR_WHEELER`) or viewing all available spaces campus-wide in an ASCII table.
   - **`[4] Reserve a Parking Slot`**: Prompts for `user_id`, `slot_id`, and arrival window (e.g., 15 or 30 minutes); outputs a formatted **`[RESERVATION CONFIRMATION]`** alert with reservation ID and formatted arrival deadline.
   - **`[5] View & Cancel My Reservations`**: Displays all reservations for a user with status, deadline, and arrival times; allows cancellation of active `CONFIRMED` bookings with atomic slot release.
   - **`[6] Vehicle Check-In`**: Validates arrival deadline with automatic expiry check. Rejects check-in with an **`[EXPIRATION ALERT]`** if overdue; otherwise marks reservation `ACTIVE` and slot `OCCUPIED` with timestamp (`yyyy-MM-dd HH:mm:ss`).
   - **`[7] Vehicle Check-Out`**: Completes active reservations, marks slot `AVAILABLE`, and confirms vehicle departure.
   - **`[8] Run Manual Slot Expiry Check & View Campus Dashboard`**: Triggers immediate expiry check and displays real-time percentage statistics and status breakdown across all campus slots.
   - **`[0] Exit`**: Performs clean shutdown of the background scheduler and closes resources.

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
