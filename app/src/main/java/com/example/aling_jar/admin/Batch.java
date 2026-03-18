package com.example.aling_jar.admin;

import com.google.firebase.Timestamp;

public class Batch {
    private String documentId;
    private String name;
    private long quantity;
    private String status;
    private Timestamp expiryDate;
    private Timestamp createdAt;

    public Batch() {
        // Required empty constructor for Firestore
    }

    public Batch(String name, long quantity, String status, Timestamp expiryDate, Timestamp createdAt) {
        this.name = name;
        this.quantity = quantity;
        this.status = status;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Timestamp expiryDate) { this.expiryDate = expiryDate; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
