package com.example.aling_jar.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.aling_jar.data.model.Batch;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-responsibility data access layer for the {@code batches} Firestore collection.
 * <p>
 * Encapsulates all query logic so that fragments/activities never interact
 * with Firestore directly. This makes it easy to swap in a different data
 * source (e.g. Room cache, mock data for testing) without touching UI code
 * (Dependency Inversion).
 */
public class BatchRepository {

    private static final String TAG = "BatchRepository";
    private static final String COLLECTION_BATCHES = "batches";

    private final FirebaseFirestore db;

    public BatchRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Constructor for testing — accepts an injected Firestore instance. */
    public BatchRepository(@NonNull FirebaseFirestore firestore) {
        this.db = firestore;
    }

    // ─── Callback contracts ────────────────────────────────────────

    /** Callback for a list of batches. */
    public interface OnBatchesLoadedCallback {
        void onSuccess(@NonNull List<Batch> batches);
        void onFailure(@NonNull Exception e);
    }

    // ─── Public API ────────────────────────────────────────────────

    /**
     * Fetch all batches from Firestore.
     * Results are delivered asynchronously via {@code callback}.
     */
    public ListenerRegistration fetchAllBatches(@NonNull OnBatchesLoadedCallback callback) {
        return db.collection(COLLECTION_BATCHES)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for batches", e);
                        callback.onFailure(e);
                        return;
                    }
                    if (querySnapshot != null) {
                        List<Batch> batches = parseBatches(querySnapshot);
                        callback.onSuccess(batches);
                    }
                });
    }

    /**
     * Fetch only batches that are currently available for purchase
     * (quantity > 0 and status is not "SPOILED").
     */
    public ListenerRegistration fetchAvailableBatches(@NonNull OnBatchesLoadedCallback callback) {
        return db.collection(COLLECTION_BATCHES)
                .whereGreaterThan("quantity", 0)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen for available batches", e);
                        callback.onFailure(e);
                        return;
                    }
                    if (querySnapshot != null) {
                        List<Batch> batches = parseBatches(querySnapshot);
                        callback.onSuccess(batches);
                    }
                });
    }

    // ─── Internal helpers ──────────────────────────────────────────

    /**
     * Converts a Firestore {@link QuerySnapshot} into a list of {@link Batch} objects,
     * preserving document IDs for downstream operations.
     */
    private List<Batch> parseBatches(@NonNull QuerySnapshot querySnapshot) {
        List<Batch> batches = new ArrayList<>();
        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
            Batch batch = doc.toObject(Batch.class);
            if (batch != null) {
                batch.setDocumentId(doc.getId());
                batches.add(batch);
            }
        }
        return batches;
    }
}
