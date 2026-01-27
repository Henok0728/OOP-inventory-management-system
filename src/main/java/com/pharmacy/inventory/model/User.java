package com.pharmacy.inventory.model;

public class User {
    private long userId;
    private String name;
    private String email;
    private String role;
    private String password;
    private String rfidTag;
    private boolean inWarehouse; // Changed to boolean for easier logic toggling

    public User() {
    }

    // Updated constructor to include new fields if needed
    public User(long userId, String name, String email, String password, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRfidTag() {
        return rfidTag;
    }

    public void setRfidTag(String rfidTag) {
        this.rfidTag = rfidTag;
    }

    public boolean isInWarehouse() {
        return inWarehouse;
    }

    public void setInWarehouse(boolean inWarehouse) {
        this.inWarehouse = inWarehouse;
    }
}