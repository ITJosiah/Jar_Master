package com.example.aling_jar.admin;

import com.google.firebase.Timestamp;

public class Order {
    private String documentId;
    private String orderId;
    private String customerName;
    private String address;
    private String items;
    private double total;
    private String status;
    private Timestamp createdAt;
    private String userId;
    private java.util.List<com.example.aling_jar.data.model.BatchOrderItem> batchDetails;

    public Order() {
        // Required empty constructor for Firestore
    }

    public Order(String customerName, String items, double total, String status, Timestamp createdAt) {
        this.customerName = customerName;
        this.items = items;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getItems() { return items; }
    public void setItems(String items) { this.items = items; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public java.util.List<com.example.aling_jar.data.model.BatchOrderItem> getBatchDetails() { return batchDetails; }
    public void setBatchDetails(java.util.List<com.example.aling_jar.data.model.BatchOrderItem> batchDetails) { this.batchDetails = batchDetails; }
}
