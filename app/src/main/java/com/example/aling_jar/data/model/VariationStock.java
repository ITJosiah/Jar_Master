package com.example.aling_jar.data.model;

public class VariationStock {
    private String productName;
    private String flavorProfile;
    private String ingredient;
    private String size;
    private long stock;

    public VariationStock(String productName, String flavorProfile, String ingredient, String size) {
        this.productName = productName;
        this.flavorProfile = flavorProfile;
        this.ingredient = ingredient;
        this.size = size;
        this.stock = 0;
    }

    public String getProductName() { return productName; }
    public String getFlavorProfile() { return flavorProfile; }
    public String getIngredient() { return ingredient; }
    public String getSize() { return size; }
    public long getStock() { return stock; }
    
    public void setStock(long stock) { this.stock = stock; }
    public void addStock(long amount) { this.stock += amount; }
    
    public boolean matches(String pName, String fProfile, String ing, String sz) {
        String p1 = productName != null ? productName.toLowerCase() : "";
        String f1 = flavorProfile != null ? flavorProfile.toLowerCase() : "";
        String i1 = ingredient != null ? ingredient.toLowerCase() : "";
        String s1 = size != null ? size.toLowerCase() : "";
        
        String p2 = pName != null ? pName.toLowerCase() : "";
        String f2 = fProfile != null ? fProfile.toLowerCase() : "";
        String i2 = ing != null ? ing.toLowerCase() : "";
        String s2 = sz != null ? sz.toLowerCase() : "";
        
        return p1.equals(p2) && f1.equals(f2) && i1.equals(i2) && s1.equals(s2);
    }
}
