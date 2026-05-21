package com.example.aling_jar.admin;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import com.example.aling_jar.data.model.Batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminDashboardFragment extends Fragment {

    private static final String TAG = "AdminDashboard";

    // Views
    private TextView tvStock, tvStockChange;
    private TextView tvOrders, tvOrdersChange;
    private TextView tvAlerts, tvAlertsLabel;
    private TextView tvPendingOrderCount;
    private TextView tvLaingStock, tvLaingBatches;
    private TextView tvSinantolStock, tvSinantolBatches;
    private RecyclerView rvRecentBatches, rvPendingOrders, rvVariationsStock;
    private MaterialButton btnLogNewBatch;
    private View btnNotification;

    private androidx.core.widget.NestedScrollView scrollDashboard;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Adapters
    private DashboardBatchAdapter batchAdapter;
    private OrderAdapter orderAdapter;
    private VariationStockAdapter variationAdapter;
    private List<Batch> recentBatches = new ArrayList<>();
    private List<Order> pendingOrders = new ArrayList<>();
    private List<com.example.aling_jar.data.model.VariationStock> variationStocks = new ArrayList<>();

    // Listeners for real-time updates
    private ListenerRegistration batchListener;
    private ListenerRegistration orderListener;
    private ListenerRegistration totalStatsListener;

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
        setupClickListeners();
        startRealtimeListeners();
    }

    private void initViews(View view) {
        tvStock = view.findViewById(R.id.tvStock);
        tvStockChange = view.findViewById(R.id.tvStockChange);
        tvOrders = view.findViewById(R.id.tvOrders);
        tvOrdersChange = view.findViewById(R.id.tvOrdersChange);
        tvAlerts = view.findViewById(R.id.tvAlerts);
        tvAlertsLabel = view.findViewById(R.id.tvAlertsLabel);
        tvPendingOrderCount = view.findViewById(R.id.tvPendingOrderCount);
        tvLaingStock        = view.findViewById(R.id.tvLaingStock);
        tvLaingBatches      = view.findViewById(R.id.tvLaingBatches);
        tvSinantolStock     = view.findViewById(R.id.tvSinantolStock);
        tvSinantolBatches   = view.findViewById(R.id.tvSinantolBatches);
        rvRecentBatches = view.findViewById(R.id.rvRecentBatches);
        rvPendingOrders = view.findViewById(R.id.rvPendingOrders);
        rvVariationsStock = view.findViewById(R.id.rvVariationsStock);
        btnLogNewBatch = view.findViewById(R.id.btnLogNewBatch);
        btnNotification = view.findViewById(R.id.btnNotification);

        scrollDashboard = view.findViewById(R.id.scrollDashboard);
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

        // Variations Stock Adapter
        variationAdapter = new VariationStockAdapter(variationStocks);
        rvVariationsStock.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvVariationsStock.setAdapter(variationAdapter);
    }

    private void setupClickListeners() {
        btnLogNewBatch.setOnClickListener(v -> {
            if (getActivity() instanceof AdminActivity) {
                ((AdminActivity) getActivity()).openLogNewBatch();
            }
        });

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.aling_jar.user.notifications.NotificationsActivity.class);
                startActivity(intent);
            });
        }

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
                .limit(3)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
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
                }
                });

        // Also get total stock from ALL batches for dashboard card
        loadTotalDashboardStats();

        // Listen to orders (real-time)
        orderListener = db.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException error) {
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

                        // Update the bottom nav badge
                        if (getActivity() instanceof AdminActivity) {
                            ((AdminActivity) getActivity()).updateOrdersBadge(pendingCount);
                        }
                    }
                }
                });
    }

    private void loadTotalDashboardStats() {
        // Build empty permutations
        variationStocks.clear();
        String[] products = {"Laing", "Sinantol"};
        String[] flavors = {"Classic", "Spicy"};
        String[] ingredients = {"Pork", "Tinapa"};
        String[] sizes = {"190g", "320g"};
        
        for (String p : products) {
            for (String f : flavors) {
                for (String i : ingredients) {
                    for (String s : sizes) {
                        variationStocks.add(new com.example.aling_jar.data.model.VariationStock(p, f, i, s));
                    }
                }
            }
        }
        
        totalStatsListener = db.collection("batches")
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                    if (error != null) {
                        Log.e(TAG, "Dashboard batches listener error", error);
                        return;
                    }
                    if (snapshot != null) {
                    long totalStock = 0;
                    int criticalCount = 0;
                    long laingStock = 0;  int laingBatches = 0;
                    long sinantolStock = 0; int sinantolBatches = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Batch batch = doc.toObject(Batch.class);
                        if (batch != null) {
                            totalStock += batch.getQuantity();
                            if ("CRITICAL".equalsIgnoreCase(batch.getStatus())) criticalCount++;

                            String name = batch.getProductName() != null
                                    ? batch.getProductName().toLowerCase() : "";
                            if (name.contains("laing")) {
                                laingStock += batch.getQuantity();
                                laingBatches++;
                            } else if (name.contains("sinantol")) {
                                sinantolStock += batch.getQuantity();
                                sinantolBatches++;
                            }

                            // Add to variation
                            for (com.example.aling_jar.data.model.VariationStock var : variationStocks) {
                                if (var.matches(batch.getProductName(), batch.getFlavorProfile(), batch.getIngredient(), batch.getSize())) {
                                    var.addStock(batch.getQuantity());
                                    break;
                                }
                            }
                        }
                    }
                    
                    variationAdapter.updateList(variationStocks);
                    updateStockCard(totalStock);
                    updateAlertsCard(criticalCount);
                    updateProductCards(laingStock, laingBatches, sinantolStock, sinantolBatches);
                    }
                }
                });
    }

    private void updateProductCards(long laingStock, int laingBatches,
                                    long sinantolStock, int sinantolBatches) {
        if (tvLaingStock != null) {
            tvLaingStock.setText(String.format(Locale.getDefault(), "%,d", laingStock));
            tvLaingBatches.setText(laingBatches + " Active Batch" + (laingBatches != 1 ? "es" : ""));
        }
        if (tvSinantolStock != null) {
            tvSinantolStock.setText(String.format(Locale.getDefault(), "%,d", sinantolStock));
            tvSinantolBatches.setText(sinantolBatches + " Active Batch" + (sinantolBatches != 1 ? "es" : ""));
        }
    }

    private void updateStockCard(long totalStock) {
        if (tvStock != null) {
            tvStock.setText(String.format(Locale.getDefault(), "%,d", totalStock));
            tvStockChange.setText("Total Jars");
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
                .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Order " + newStatus.toLowerCase(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to update order",
                                    Toast.LENGTH_SHORT).show();
                        }
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
                            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(getContext(), "Batch removed", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getContext(), "Failed to remove batch", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up listeners
        if (batchListener != null) batchListener.remove();
        if (orderListener != null) orderListener.remove();
        if (totalStatsListener != null) totalStatsListener.remove();
    }
}
