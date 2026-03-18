package com.example.aling_jar.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminBatchesFragment extends Fragment {

    private static final String TAG = "AdminBatches";

    private RecyclerView rvBatches;
    private EditText etSearch;
    private TextView tvBatchCount;
    private BatchAdapter batchAdapter;
    private List<Batch> allBatches = new ArrayList<>();
    private List<Batch> filteredBatches = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration batchListener;
    private String currentFilter = "ALL";

    // Filter tabs
    private View tabAll, tabFresh, tab30Days, tabExpired;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_fragment_batches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rvBatches = view.findViewById(R.id.rvBatches);
        etSearch = view.findViewById(R.id.etSearch);
        tvBatchCount = view.findViewById(R.id.tvBatchCount);
        tabAll = view.findViewById(R.id.tabAll);
        tabFresh = view.findViewById(R.id.tabFresh);
        tab30Days = view.findViewById(R.id.tab30Days);
        tabExpired = view.findViewById(R.id.tabExpired);

        setupRecyclerView();
        setupFilterTabs();
        setupSearch();
        startRealtimeListener();
    }

    private void setupRecyclerView() {
        batchAdapter = new BatchAdapter(filteredBatches, new BatchAdapter.OnBatchClickListener() {
            @Override
            public void onEditClick(Batch batch, int position) {
                Toast.makeText(getContext(), "Edit: " + batch.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(Batch batch, int position) {
                deleteBatch(batch);
            }
        });
        rvBatches.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBatches.setAdapter(batchAdapter);
    }

    private void setupFilterTabs() {
        tabAll.setOnClickListener(v -> applyFilter("ALL"));
        tabFresh.setOnClickListener(v -> applyFilter("FRESH"));
        tab30Days.setOnClickListener(v -> applyFilter("SELLING FAST"));
        tabExpired.setOnClickListener(v -> applyFilter("CRITICAL"));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBySearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        filterBatches();
    }

    private void filterBySearch(String query) {
        filteredBatches.clear();
        for (Batch batch : allBatches) {
            boolean matchesFilter = "ALL".equals(currentFilter) ||
                    currentFilter.equalsIgnoreCase(batch.getStatus());
            boolean matchesSearch = query.isEmpty() ||
                    (batch.getName() != null && batch.getName().toLowerCase().contains(query.toLowerCase()));

            if (matchesFilter && matchesSearch) {
                filteredBatches.add(batch);
            }
        }
        batchAdapter.updateList(filteredBatches);
        tvBatchCount.setText("ACTIVE BATCHES (" + filteredBatches.size() + ")");
    }

    private void filterBatches() {
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        filterBySearch(query);
    }

    private void startRealtimeListener() {
        batchListener = db.collection("batches")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Batch listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        allBatches.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Batch batch = doc.toObject(Batch.class);
                            if (batch != null) {
                                batch.setDocumentId(doc.getId());
                                allBatches.add(batch);
                            }
                        }
                        filterBatches();
                    }
                });
    }

    private void deleteBatch(Batch batch) {
        if (batch.getDocumentId() == null) return;

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Mark as Spoiled")
                .setMessage("Remove \"" + batch.getName() + "\" from inventory?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    db.collection("batches").document(batch.getDocumentId())
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(), "Batch removed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to remove", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (batchListener != null) batchListener.remove();
    }
}
