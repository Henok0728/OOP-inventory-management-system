package com.pharmacy.inventory.model;

public class SaleItem {
    private long saleItemId;
    private int saleId;
    private int itemId;
    private int batchId;
    private int quantity;
    private double unitPrice;
    private double subtotal;

    public SaleItem() {
    }

    public SaleItem(int saleId, int itemId, int batchId, int quantity, double unitPrice, double subtotal) {
        this.saleId = saleId;
        this.itemId = itemId;
        this.batchId = batchId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public long getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(long saleItemId) {
        this.saleItemId = saleItemId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
}
