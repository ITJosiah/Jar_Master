package com.example.aling_jar.admin;

import android.os.Bundle;
import android.content.Intent;
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
    private TextView tvBatchCount, tvTotalStock, tvActiveBatchesCount;
    private TextView tvLaingStock, tvLaingBatches;
    private TextView tvSinantolStock, tvSinantolBatches;
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
        tvTotalStock = view.findViewById(R.id.tvTotalStock);
        tvActiveBatchesCount = view.findViewById(R.id.tvActiveBatchesCount);
        tvLaingStock    = view.findViewById(R.id.tvLaingStock);
        tvLaingBatches  = view.findViewById(R.id.tvLaingBatches);
        tvSinantolStock   = view.findViewById(R.id.tvSinantolStock);
        tvSinantolBatches = view.findViewById(R.id.tvSinantolBatches);
        tabAll = view.findViewById(R.id.tabAll);
        tabFresh = view.findViewById(R.id.tabFresh);
        tab30Days = view.findViewById(R.id.tab30Days);
        tabExpired = view.findViewById(R.id.tabExpired);

        setupRecyclerView();
        setupFilterTabs();
        setupSearch();
        startRealtimeListener();

        View btnNotification = view.findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.aling_jar.user.notifications.NotificationsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupRecyclerView() {
        batchAdapter = new BatchAdapter(filteredBatches, new BatchAdapter.OnBatchClickListener() {
            @Override
            public void onEditClick(Batch batch, int position) {
                if (getActivity() instanceof AdminActivity) {
                    ((AdminActivity) getActivity()).openEditBatch(batch);
                }
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
        tabAll.setOnClickListener(v     -> { applyFilter("ALL");          setActiveTab(tabAll); });
        tabFresh.setOnClickListener(v   -> { applyFilter("FRESH");        setActiveTab(tabFresh); });
        tab30Days.setOnClickListener(v  -> { applyFilter("30 DAYS OLD");  setActiveTab(tab30Days); });
        tabExpired.setOnClickListener(v -> { applyFilter("CRITICAL");     setActiveTab(tabExpired); });

        // Default active tab on load
        setActiveTab(tabAll);
    }

    /** Highlights the selected tab green and resets all others to inactive. */
    private void setActiveTab(View selected) {
        View[] tabs = { tabAll, tabFresh, tab30Days, tabExpired };
        for (View tab : tabs) {
            boolean isActive = tab == selected;
            tab.setBackgroundResource(isActive ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);

            // For plain TextView tabs (tabAll)
            if (tab instanceof TextView) {
                ((TextView) tab).setTextColor(isActive ? 0xFFFFFFFF : 0xFF555555);
            }

            // For LinearLayout tabs (tabFresh, tab30Days, tabExpired) — update child TextView
            if (tab instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout ll = (android.widget.LinearLayout) tab;
                for (int i = 0; i < ll.getChildCount(); i++) {
                    android.view.View child = ll.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(isActive ? 0xFFFFFFFF : 0xFF555555);
                    }
                }
            }
        }
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
        updateSummary(filteredBatches);
    }

    private void updateSummary(List<Batch> currentList) {
        long totalStock = 0;
        long laingStock = 0;    int laingBatches = 0;
        long sinantolStock = 0; int sinantolBatches = 0;

        for (Batch b : currentList) {
            totalStock += b.getQuantity();
            String name = b.getProductName() != null ? b.getProductName().toLowerCase() : "";
            if (name.contains("laing")) {
                laingStock += b.getQuantity();
                laingBatches++;
            } else if (name.contains("sinantol")) {
                sinantolStock += b.getQuantity();
                sinantolBatches++;
            }
        }
        if (tvTotalStock != null)
            tvTotalStock.setText(String.format(java.util.Locale.getDefault(), "%,d", totalStock));
        if (tvActiveBatchesCount != null)
            tvActiveBatchesCount.setText(String.valueOf(currentList.size()));
        if (tvLaingStock != null) {
            tvLaingStock.setText(String.format(java.util.Locale.getDefault(), "%,d", laingStock));
            tvLaingBatches.setText(laingBatches + " Active Batch" + (laingBatches != 1 ? "es" : ""));
        }
        if (tvSinantolStock != null) {
            tvSinantolStock.setText(String.format(java.util.Locale.getDefault(), "%,d", sinantolStock));
            tvSinantolBatches.setText(sinantolBatches + " Active Batch" + (sinantolBatches != 1 ? "es" : ""));
        }
    }

    private void filterBatches() {
        String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
        filterBySearch(query);
    }

    private void startRealtimeListener() {
        batchListener = db.collection("batches")
                .orderBy("createdAt", Query.Direction.ASCENDING)
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
