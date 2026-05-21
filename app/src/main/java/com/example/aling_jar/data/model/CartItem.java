package com.example.aling_jar.data.model;

import java.io.Serializable;

/**
 * Model representing an aggregated item in the user's cart.
 */
public class CartItem implements Serializable {
    private String productName;
    private String flavorProfile;
    private String ingredient;
    private String size;
    private int quantity;
    private double unitPrice;
    private String photoUrl;

    public CartItem() {}

    public CartItem(String productName, String flavorProfile, String ingredient, String size, int quantity, double unitPrice, String photoUrl) {
        this.productName = productName;
        this.flavorProfile = flavorProfile;
        this.ingredient = ingredient;
        this.size = size;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.photoUrl = photoUrl;
    }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getFlavorProfile() { return flavorProfile; }
    public void setFlavorProfile(String flavorProfile) { this.flavorProfile = flavorProfile; }

    public String getIngredient() { return ingredient; }
    public void setIngredient(String ingredient) {
        this.ingredient = ingredient;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public double getTotalPrice() {
        return unitPrice * quantity;
    }
}
