package com.example.aling_jar.admin;

import com.google.firebase.Timestamp;

public class Batch {
    private String documentId;
    private String batchId;
    private String productName;
    private String flavorProfile;
    private long quantity;
    private String notes;
    private String photoUrl;
    private double price;
    private String status;
    private Timestamp expiryDate;
    private Timestamp createdAt;
    private String createdBy;

    public Batch() {
        // Required empty constructor for Firestore
    }

    public Batch(
            String batchId,
            String productName,
            String flavorProfile,
            long quantity,
            String notes,
            String photoUrl,
            double price,
            String status,
            Timestamp expiryDate,
            Timestamp createdAt,
            String createdBy
    ) {
        this.batchId = batchId;
        this.productName = productName;
        this.flavorProfile = flavorProfile;
        this.quantity = quantity;
        this.notes = notes;
        this.photoUrl = photoUrl;
        this.price = price;
        this.status = status;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    /**
     * Backwards compatibility: old UI uses getName()
     */
    public String getName() { return productName; }

    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }

    public String getFlavorProfile() { return flavorProfile; }
    public void setFlavorProfile(String flavorProfile) { this.flavorProfile = flavorProfile; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Timestamp expiryDate) { this.expiryDate = expiryDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
