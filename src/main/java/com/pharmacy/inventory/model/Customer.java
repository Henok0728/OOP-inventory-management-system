package com.pharmacy.inventory.model;

import java.sql.Timestamp;

public class Customer {
    private Long customerId;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String address;
    private String dateOfBirth;
    private String gender;
    private String medicalRecordNumber;
    private Timestamp createdAt;

    // Getters and Setters
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMedicalRecordNumber() { return medicalRecordNumber; }
    public void setMedicalRecordNumber(String medicalRecordNumber) { this.medicalRecordNumber = medicalRecordNumber; }

    // IMPORTANT: toString for the JComboBox in SalesPanel
    @Override
    public String toString() {
        if (customerId != null && customerId == 0L) return "Walk-in Customer";
        return firstName + " " + lastName + (medicalRecordNumber != null ? " [" + medicalRecordNumber + "]" : "");
    }
}