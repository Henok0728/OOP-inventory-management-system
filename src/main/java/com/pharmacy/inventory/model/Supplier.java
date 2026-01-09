package com.pharmacy.inventory.model;

public class Supplier {
    private int supplierId;
    private String name;
    private String contact;
    private String phoneNumber;
    private String email;
    private String address;
    private String licenseNumber;
    private String paymentTerms;

    public Supplier() {
    }

    public Supplier(int supplierId, String name, String licenseNumber, String address, String email, String contact, String phoneNumber, String paymentTerms) {
        this.supplierId = supplierId;
        this.name = name;
        this.licenseNumber = licenseNumber;
        this.address = address;
        this.email = email;
        this.contact = contact;
        this.phoneNumber = phoneNumber;
        this.paymentTerms = paymentTerms;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    @Override
    public String toString() {
        return (name != null) ? name : "Unknown Supplier";
    }
}
