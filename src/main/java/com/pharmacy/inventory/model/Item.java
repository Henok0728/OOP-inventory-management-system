package com.pharmacy.inventory.model;

import java.sql.Date;

public class Item {
    private int item_id;
    private String name;
    private String genericName;
    private String brandName;
    private String barcode;
    private String category;
    private String dosageForm;
    private String strength;
    private double retailPrice;
    private int reorderLevel;
    private boolean prescriptionRequired;

    public Item(){}
    public Item(String name, String genericName, String brandName, String barcode,
                String category, String dosageForm, String strength,
                double retailPrice, int reorderLevel, boolean prescriptionRequired) {
        this.name = name;
        this.genericName = genericName;
        this.brandName = brandName;
        this.barcode = barcode;
        this.category = category;
        this.dosageForm = dosageForm;
        this.strength = strength;
        this.retailPrice = retailPrice;
        this.reorderLevel = reorderLevel;
        this.prescriptionRequired = prescriptionRequired;
    }

    public int getItem_id() {
        return item_id;
    }

    public void setItem_id(int item_id) {
        this.item_id = item_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public double getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(double retailPrice) {
        this.retailPrice = retailPrice;
    }

    public boolean isPrescriptionRequired() {
        return prescriptionRequired;
    }

    public void setPrescriptionRequired(boolean prescriptionRequired) {
        this.prescriptionRequired = prescriptionRequired;
    }

    public String getDosageForm() {
        return dosageForm;
    }

    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(int reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
}
