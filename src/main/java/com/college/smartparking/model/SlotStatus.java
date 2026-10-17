package com.college.smartparking.model;

/**
 * Enumeration representing the current availability status of a parking slot.
 */
public enum SlotStatus {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    OCCUPIED("Occupied");

    private final String displayName;

    SlotStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SlotStatus fromString(String text) {
        if (text == null) return null;
        for (SlotStatus status : SlotStatus.values()) {
            if (status.name().equalsIgnoreCase(text.trim()) || status.displayName.equalsIgnoreCase(text.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown SlotStatus: " + text);
    }
}
