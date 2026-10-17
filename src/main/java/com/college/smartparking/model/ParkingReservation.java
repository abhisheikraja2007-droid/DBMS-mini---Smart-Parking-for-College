package com.college.smartparking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain Model representing a Parking Slot Reservation.
 * Corresponds to PARKING_RESERVATIONS database table.
 */
public class ParkingReservation {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Integer reservationId;
    private Integer userId;
    private Integer slotId;
    private LocalDateTime reservationTime;
    private LocalDateTime arrivalDeadline;
    private LocalDateTime arrivalTime;
    private ReservationStatus status;

    /**
     * Default constructor.
     */
    public ParkingReservation() {
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * Parameterized constructor without primary key (for creating new reservations).
     */
    public ParkingReservation(Integer userId, Integer slotId, LocalDateTime reservationTime,
                              LocalDateTime arrivalDeadline, LocalDateTime arrivalTime, ReservationStatus status) {
        this.userId = userId;
        this.slotId = slotId;
        this.reservationTime = reservationTime;
        this.arrivalDeadline = arrivalDeadline;
        this.arrivalTime = arrivalTime;
        this.status = (status != null) ? status : ReservationStatus.CONFIRMED;
    }

    /**
     * Parameterized constructor with primary key (for retrieved reservations).
     */
    public ParkingReservation(Integer reservationId, Integer userId, Integer slotId, LocalDateTime reservationTime,
                              LocalDateTime arrivalDeadline, LocalDateTime arrivalTime, ReservationStatus status) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.slotId = slotId;
        this.reservationTime = reservationTime;
        this.arrivalDeadline = arrivalDeadline;
        this.arrivalTime = arrivalTime;
        this.status = status;
    }

    // Getters and Setters

    public Integer getReservationId() {
        return reservationId;
    }

    public void setReservationId(Integer reservationId) {
        this.reservationId = reservationId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public void setReservationTime(LocalDateTime reservationTime) {
        this.reservationTime = reservationTime;
    }

    public LocalDateTime getArrivalDeadline() {
        return arrivalDeadline;
    }

    public void setArrivalDeadline(LocalDateTime arrivalDeadline) {
        this.arrivalDeadline = arrivalDeadline;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingReservation that = (ParkingReservation) o;
        return Objects.equals(reservationId, that.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }

    /**
     * Formats parking reservation as a readable console table row.
     */
    @Override
    public String toString() {
        return String.format("| %-8s | %-7s | %-7s | %-16s | %-16s | %-16s | %-10s |",
                reservationId != null ? reservationId : "-",
                userId != null ? userId : "-",
                slotId != null ? slotId : "-",
                formatDate(reservationTime),
                formatDate(arrivalDeadline),
                formatDate(arrivalTime),
                status != null ? status.name() : "-");
    }

    public static String getTableHeader() {
        String border = "+----------+---------+---------+------------------+------------------+------------------+------------+";
        String header = "| Res ID   | User ID | Slot ID | Reservation Time | Arrival Deadline | Arrival Time     | Status     |";
        return border + "\n" + header + "\n" + border;
    }

    public static String getTableDivider() {
        return "+----------+---------+---------+------------------+------------------+------------------+------------+";
    }

    private static String formatDate(LocalDateTime dateTime) {
        return (dateTime != null) ? dateTime.format(DATE_FORMATTER) : "N/A";
    }
}
