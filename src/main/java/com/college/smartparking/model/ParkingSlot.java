package com.college.smartparking.model;

import java.util.Objects;

/**
 * Domain Model representing a Parking Slot on campus.
 * Corresponds to PARKING_SLOTS database table.
 */
public class ParkingSlot {

    private Integer slotId;
    private String slotNumber;
    private String parkingArea;
    private SlotType slotType;
    private SlotStatus status;

    /**
     * Default constructor.
     */
    public ParkingSlot() {
        this.status = SlotStatus.AVAILABLE;
    }

    /**
     * Parameterized constructor without primary key (for creating new slots).
     */
    public ParkingSlot(String slotNumber, String parkingArea, SlotType slotType, SlotStatus status) {
        this.slotNumber = slotNumber;
        this.parkingArea = parkingArea;
        this.slotType = slotType;
        this.status = (status != null) ? status : SlotStatus.AVAILABLE;
    }

    /**
     * Parameterized constructor with primary key (for retrieved slots).
     */
    public ParkingSlot(Integer slotId, String slotNumber, String parkingArea, SlotType slotType, SlotStatus status) {
        this.slotId = slotId;
        this.slotNumber = slotNumber;
        this.parkingArea = parkingArea;
        this.slotType = slotType;
        this.status = status;
    }

    // Getters and Setters

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public String getParkingArea() {
        return parkingArea;
    }

    public void setParkingArea(String parkingArea) {
        this.parkingArea = parkingArea;
    }

    public SlotType getSlotType() {
        return slotType;
    }

    public void setSlotType(SlotType slotType) {
        this.slotType = slotType;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public void setStatus(SlotStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingSlot that = (ParkingSlot) o;
        return Objects.equals(slotId, that.slotId) &&
               Objects.equals(slotNumber, that.slotNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotId, slotNumber);
    }

    /**
     * Formats parking slot as a readable console table row.
     */
    @Override
    public String toString() {
        return String.format("| %-7s | %-12s | %-16s | %-15s | %-12s |",
                slotId != null ? slotId : "-",
                truncate(slotNumber, 12),
                truncate(parkingArea, 16),
                slotType != null ? slotType.name() : "-",
                status != null ? status.name() : "-");
    }

    public static String getTableHeader() {
        String border = "+---------+--------------+------------------+-----------------+--------------+";
        String header = "| Slot ID | Slot Number  | Parking Area     | Vehicle Type    | Status       |";
        return border + "\n" + header + "\n" + border;
    }

    public static String getTableDivider() {
        return "+---------+--------------+------------------+-----------------+--------------+";
    }

    private static String truncate(String val, int maxLen) {
        if (val == null) return "-";
        return val.length() > maxLen ? val.substring(0, maxLen - 3) + "..." : val;
    }
}
