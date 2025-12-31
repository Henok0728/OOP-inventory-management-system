package com.pharmacy.inventory.model;

public class Batch {
        private long batchId;
        private String batchNumber;
        private int itemId;
        private int quantityReceived;
        private int quantityRemaining;
        private String manufacturedDate; // Using String for simplicity with SQL dates
        private String expirationDate;
        private double purchasePrice;
        private double sellingPrice;
        private String storageLocation;
        private String status;
        private String receivedDate;

    public Batch() {
    }

    public Batch(String batchNumber, int itemId, int quantityReceived, int quantityRemaining, String expirationDate, double purchasePrice, double sellingPrice, String status, String receivedDate) {
        this.batchNumber = batchNumber;
        this.itemId = itemId;
        this.quantityReceived = quantityReceived;
        this.quantityRemaining = quantityRemaining;
        this.expirationDate = expirationDate;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.status = "active";
        this.receivedDate = receivedDate;
    }
    // Add this constructor to Batch.java
    public Batch(String batchNumber, int itemId, int quantity, String expirationDate, double purchasePrice, double sellingPrice) {
        this.batchNumber = batchNumber;
        this.itemId = itemId;
        this.quantityReceived = quantity;
        this.quantityRemaining = quantity;
        this.expirationDate = expirationDate;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.status = "active";
        this.receivedDate = java.time.LocalDate.now().toString();
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public int getQuantityRemaining() {
        return quantityRemaining;
    }

    public void setQuantityRemaining(int quantityRemaining) {
        this.quantityRemaining = quantityRemaining;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getManufacturedDate() {
        return manufacturedDate;
    }

    public void setManufacturedDate(String manufacturedDate) {
        this.manufacturedDate = manufacturedDate;
    }

    public long getBatchId() {
        return batchId;
    }

    public void setBatchId(long batchId) {
        this.batchId = batchId;
    }
}
