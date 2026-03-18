package com.example.aling_jar.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {

    private static final String TAG = "AdminDashboard";

    // Views
    private TextView tvAdminName, tvStock, tvStockChange;
    private TextView tvOrders, tvOrdersChange;
    private TextView tvAlerts, tvAlertsLabel;
    private TextView tvPendingOrderCount;
    private RecyclerView rvRecentBatches, rvPendingOrders;
    private MaterialButton btnLogNewBatch;
    private View btnNotification;
    private FloatingActionButton fabAdd;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Adapters
    private DashboardBatchAdapter batchAdapter;
    private OrderAdapter orderAdapter;
    private List<Batch> recentBatches = new ArrayList<>();
    private List<Order> pendingOrders = new ArrayList<>();

    // Listeners for real-time updates
    private ListenerRegistration batchListener;
    private ListenerRegistration orderListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerViews();
        loadAdminName();
        setupClickListeners();
        startRealtimeListeners();
    }

    private void initViews(View view) {
        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvStock = view.findViewById(R.id.tvStock);
        tvStockChange = view.findViewById(R.id.tvStockChange);
        tvOrders = view.findViewById(R.id.tvOrders);
        tvOrdersChange = view.findViewById(R.id.tvOrdersChange);
        tvAlerts = view.findViewById(R.id.tvAlerts);
        tvAlertsLabel = view.findViewById(R.id.tvAlertsLabel);
        tvPendingOrderCount = view.findViewById(R.id.tvPendingOrderCount);
        rvRecentBatches = view.findViewById(R.id.rvRecentBatches);
        rvPendingOrders = view.findViewById(R.id.rvPendingOrders);
        btnLogNewBatch = view.findViewById(R.id.btnLogNewBatch);
        btnNotification = view.findViewById(R.id.btnNotification);
        fabAdd = view.findViewById(R.id.fabAdd);
    }

    private void setupRecyclerViews() {
        // Recent Batches — using new DashboardBatchAdapter
        batchAdapter = new DashboardBatchAdapter(recentBatches, new DashboardBatchAdapter.OnBatchActionListener() {
            @Override
            public void onEditClick(Batch batch, int position) {
                Toast.makeText(getContext(), "Edit: " + batch.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(Batch batch, int position) {
                deleteBatch(batch);
            }
        });
        rvRecentBatches.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentBatches.setAdapter(batchAdapter);

        // Pending Orders — HORIZONTAL layout
        orderAdapter = new OrderAdapter(pendingOrders, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onConfirmClick(Order order, int position) {
                updateOrderStatus(order, "CONFIRMED");
            }

            @Override
            public void onDeclineClick(Order order, int position) {
                updateOrderStatus(order, "DECLINED");
            }
        });
        rvPendingOrders.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPendingOrders.setAdapter(orderAdapter);
    }

    private void loadAdminName() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getString("fullName") != null) {
                            tvAdminName.setText(doc.getString("fullName"));
                        }
                    });
        }
    }

    private void setupClickListeners() {
        btnLogNewBatch.setOnClickListener(v -> showNewBatchDialog());

        // FAB also opens the new batch dialog
        fabAdd.setOnClickListener(v -> showNewBatchDialog());

        // View All Batches — navigate to Batches tab
        View tvViewAllBatches = requireView().findViewById(R.id.tvViewAllBatches);
        if (tvViewAllBatches != null) {
            tvViewAllBatches.setOnClickListener(v -> {
                if (getActivity() instanceof AdminActivity) {
                    ((AdminActivity) getActivity()).navigateToBatches();
                }
            });
        }
    }

    // ─────────────────────────────────────────────
    //  Real-time Firestore Listeners
    // ─────────────────────────────────────────────

    private void startRealtimeListeners() {
        // Listen to batches (recent 5, ordered by createdAt)
        batchListener = db.collection("batches")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Batch listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        recentBatches.clear();
                        long totalStock = 0;
                        int criticalCount = 0;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Batch batch = doc.toObject(Batch.class);
                            if (batch != null) {
                                batch.setDocumentId(doc.getId());
                                recentBatches.add(batch);
                                totalStock += batch.getQuantity();
                                if ("CRITICAL".equalsIgnoreCase(batch.getStatus())) {
                                    criticalCount++;
                                }
                            }
                        }

                        batchAdapter.updateList(recentBatches);
                        updateStockCard(totalStock);
                        updateAlertsCard(criticalCount);
                    }
                });

        // Also get total stock from ALL batches for dashboard card
        loadTotalDashboardStats();

        // Listen to orders (real-time)
        orderListener = db.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Order listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        pendingOrders.clear();
                        int pendingCount = 0;
                        int totalOrders = snapshots.size();

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                order.setDocumentId(doc.getId());
                                if ("PENDING".equalsIgnoreCase(order.getStatus())) {
                                    pendingOrders.add(order);
                                    pendingCount++;
                                }
                            }
                        }

                        orderAdapter.updateList(pendingOrders);
                        tvPendingOrderCount.setText(pendingCount + " NEW");
                        tvOrders.setText(String.valueOf(totalOrders));
                        tvOrdersChange.setText(pendingCount + " pending");
                    }
                });
    }

    private void loadTotalDashboardStats() {
        // Get total stock from ALL batches
        db.collection("batches")
                .get()
                .addOnSuccessListener(snapshots -> {
                    long totalStock = 0;
                    int criticalCount = 0;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Batch batch = doc.toObject(Batch.class);
                        if (batch != null) {
                            totalStock += batch.getQuantity();
                            if ("CRITICAL".equalsIgnoreCase(batch.getStatus())) {
                                criticalCount++;
                            }
                        }
                    }
                    updateStockCard(totalStock);
                    updateAlertsCard(criticalCount);
                });
    }

    private void updateStockCard(long totalStock) {
        if (tvStock != null) {
            tvStock.setText(String.format(Locale.getDefault(), "%,d", totalStock));
            tvStockChange.setText("Total units");
        }
    }

    private void updateAlertsCard(int criticalCount) {
        if (tvAlerts != null) {
            tvAlerts.setText(String.valueOf(criticalCount));
            tvAlertsLabel.setText(criticalCount > 0 ? "! Urgent" : "All good");
        }
    }

    // ─────────────────────────────────────────────
    //  Order Actions
    // ─────────────────────────────────────────────

    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getDocumentId() == null) return;

        db.collection("orders").document(order.getDocumentId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Order " + newStatus.toLowerCase(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to update order",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  Batch Actions
    // ─────────────────────────────────────────────

    private void deleteBatch(Batch batch) {
        if (batch.getDocumentId() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Mark as Spoiled")
                .setMessage("Are you sure you want to mark \"" + batch.getName() + "\" as spoiled and remove it?")
                .setPositiveButton("Yes, Remove", (dialog, which) -> {
                    db.collection("batches").document(batch.getDocumentId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Batch removed",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to remove batch",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNewBatchDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_new_batch, null);

        TextInputEditText etBatchName = dialogView.findViewById(R.id.etBatchName);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        TextInputEditText etExpiryDate = dialogView.findViewById(R.id.etExpiryDate);
        Spinner spinnerStatus = dialogView.findViewById(R.id.spinnerStatus);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSaveBatch);

        // Status spinner
        String[] statuses = {"FRESH", "SELLING FAST", "CRITICAL"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, statuses);
        spinnerStatus.setAdapter(adapter);

        // Date picker
        final Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        etExpiryDate.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        etExpiryDate.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });

        // Build dialog
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etBatchName.getText() != null ?
                    etBatchName.getText().toString().trim() : "";
            String qtyStr = etQuantity.getText() != null ?
                    etQuantity.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etBatchName.setError("Required");
                return;
            }
            if (qtyStr.isEmpty()) {
                etQuantity.setError("Required");
                return;
            }

            long quantity = Long.parseLong(qtyStr);
            String status = spinnerStatus.getSelectedItem().toString();
            Timestamp expiryTimestamp = new Timestamp(calendar.getTime());
            Timestamp nowTimestamp = Timestamp.now();

            Map<String, Object> batchData = new HashMap<>();
            batchData.put("name", name);
            batchData.put("quantity", quantity);
            batchData.put("status", status);
            batchData.put("expiryDate", expiryTimestamp);
            batchData.put("createdAt", nowTimestamp);

            btnSave.setEnabled(false);
            btnSave.setText("Saving…");

            db.collection("batches")
                    .add(batchData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), "Batch logged successfully!",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Batch");
                        Toast.makeText(getContext(), "Failed to save batch",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listeners
        if (batchListener != null) batchListener.remove();
        if (orderListener != null) orderListener.remove();
    }
}
