package com.college.smartparking.model;

import java.util.Objects;

/**
 * Domain Model representing a Registered User.
 * Corresponds to USERS database table.
 */
public class User {

    private Integer userId;
    private String userName;
    private String email;
    private String contactInfo;
    private String vehicleNumber;

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Parameterized constructor without primary key (for inserting new users).
     */
    public User(String userName, String email, String contactInfo, String vehicleNumber) {
        this.userName = userName;
        this.email = email;
        this.contactInfo = contactInfo;
        this.vehicleNumber = vehicleNumber;
    }

    /**
     * Parameterized constructor with primary key (for retrieved users).
     */
    public User(Integer userId, String userName, String email, String contactInfo, String vehicleNumber) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.contactInfo = contactInfo;
        this.vehicleNumber = vehicleNumber;
    }

    // Getters and Setters

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) &&
               Objects.equals(email, user.email) &&
               Objects.equals(vehicleNumber, user.vehicleNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, vehicleNumber);
    }

    /**
     * Formats user as a readable console table row.
     */
    @Override
    public String toString() {
        return String.format("| %-6s | %-20s | %-28s | %-14s | %-14s |",
                userId != null ? userId : "-",
                truncate(userName, 20),
                truncate(email, 28),
                truncate(contactInfo, 14),
                truncate(vehicleNumber, 14));
    }

    public static String getTableHeader() {
        String border = "+--------+----------------------+------------------------------+----------------+----------------+";
        String header = "| ID     | Name                 | Email                        | Contact        | Vehicle No     |";
        return border + "\n" + header + "\n" + border;
    }

    public static String getTableDivider() {
        return "+--------+----------------------+------------------------------+----------------+----------------+";
    }

    private static String truncate(String val, int maxLen) {
        if (val == null) return "-";
        return val.length() > maxLen ? val.substring(0, maxLen - 3) + "..." : val;
    }
}
