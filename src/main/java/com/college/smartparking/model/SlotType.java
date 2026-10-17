package com.college.smartparking.model;

/**
 * Enumeration representing the vehicle type supported by a parking slot.
 */
public enum SlotType {
    TWO_WHEELER("Two Wheeler"),
    FOUR_WHEELER("Four Wheeler");

    private final String displayName;

    SlotType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SlotType fromString(String text) {
        if (text == null) return null;
        for (SlotType type : SlotType.values()) {
            if (type.name().equalsIgnoreCase(text.trim()) || type.displayName.equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SlotType: " + text);
    }
}
