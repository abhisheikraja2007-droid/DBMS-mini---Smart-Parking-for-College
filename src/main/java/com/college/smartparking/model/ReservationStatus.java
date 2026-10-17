package com.college.smartparking.model;

/**
 * Enumeration representing the status lifecycle of a parking reservation.
 */
public enum ReservationStatus {
    CONFIRMED("Confirmed"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    EXPIRED("Expired");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReservationStatus fromString(String text) {
        if (text == null) return null;
        for (ReservationStatus status : ReservationStatus.values()) {
            if (status.name().equalsIgnoreCase(text.trim()) || status.displayName.equalsIgnoreCase(text.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReservationStatus: " + text);
    }
}
