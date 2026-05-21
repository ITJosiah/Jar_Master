package com.example.aling_jar.data.model;

/**
 * Model representing a specific batch source for an order item.
 * Stores the Firestore document ID of the batch and the quantity taken from it.
 */
public class BatchOrderItem {
    private String batchDocId;
    private long quantity;

    public BatchOrderItem() {
        // Required for Firestore
    }

    public BatchOrderItem(String batchDocId, long quantity) {
        this.batchDocId = batchDocId;
        this.quantity = quantity;
    }

    public String getBatchDocId() {
        return batchDocId;
    }

    public void setBatchDocId(String batchDocId) {
        this.batchDocId = batchDocId;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }
}
