package com.example.aling_jar.user.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.admin.Order;
import com.example.aling_jar.data.model.BatchOrderItem;
import com.example.aling_jar.user.notifications.NotificationsActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserOrdersFragment extends Fragment {

    private RecyclerView rvUserOrders;
    private LinearLayout layoutEmptyOrders;
    private TabLayout tabLayoutUserOrders;
    private ImageView ivNotification;
    private UserOrderAdapter adapter;
    private final List<Order> allOrdersList = new ArrayList<>();
    private final List<Order> displayList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private int selectedTabIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupTabs();
        setupRecyclerView();
        fetchOrders();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            fetchOrders();
        }
    }

    private void initViews(View view) {
        rvUserOrders = view.findViewById(R.id.rvUserOrders);
        layoutEmptyOrders = view.findViewById(R.id.layoutEmptyOrders);
        tabLayoutUserOrders = view.findViewById(R.id.tabLayoutUserOrders);
        ivNotification = view.findViewById(R.id.ivNotification);
        
        ivNotification.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });
    }

    private void setupTabs() {
        tabLayoutUserOrders.addTab(tabLayoutUserOrders.newTab().setText("Pending"));
        tabLayoutUserOrders.addTab(tabLayoutUserOrders.newTab().setText("Confirmed"));
        tabLayoutUserOrders.addTab(tabLayoutUserOrders.newTab().setText("Out for Delivery"));
        tabLayoutUserOrders.addTab(tabLayoutUserOrders.newTab().setText("Cancelled/Declined"));
        tabLayoutUserOrders.addTab(tabLayoutUserOrders.newTab().setText("History"));

        tabLayoutUserOrders.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                buildAndUpdateDisplayList();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        rvUserOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserOrderAdapter(displayList, new UserOrderAdapter.OnUserOrderActionListener() {
            @Override
            public void onCancelClick(Order order) {
                showCancelConfirmation(order);
            }

            @Override
            public void onOrderReceivedClick(Order order) {
                updateOrderStatus(order, "DELIVERED");
            }
        });
        rvUserOrders.setAdapter(adapter);
    }

    private void fetchOrders() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("orders")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("UserOrdersFragment", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        allOrdersList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                order.setDocumentId(doc.getId());
                                allOrdersList.add(order);
                            }
                        }

                        // Sort locally to avoid needing a Firestore Composite Index
                        java.util.Collections.sort(allOrdersList, (o1, o2) -> {
                            if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                            return o2.getCreatedAt().compareTo(o1.getCreatedAt()); // Descending
                        });

                        buildAndUpdateDisplayList();
                    }
                });
    }

    private void buildAndUpdateDisplayList() {
        displayList.clear();

        for (Order o : allOrdersList) {
            String status = o.getStatus() != null ? o.getStatus().toUpperCase() : "";
            switch (selectedTabIndex) {
                case 0: // Pending
                    if ("PENDING".equals(status)) displayList.add(o);
                    break;
                case 1: // Confirmed
                    if ("CONFIRMED".equals(status) || "PREPARING".equals(status)) displayList.add(o);
                    break;
                case 2: // Out for Delivery
                    if ("DELIVERY".equals(status) || "DELIVERING".equals(status)) displayList.add(o);
                    break;
                case 3: // Cancelled/Declined
                    if ("CANCELLED".equals(status) || "DECLINED".equals(status)) displayList.add(o);
                    break;
                case 4: // History (Delivered)
                    if ("DELIVERED".equals(status)) displayList.add(o);
                    break;
            }
        }

        adapter.notifyDataSetChanged();

        if (displayList.isEmpty()) {
            layoutEmptyOrders.setVisibility(View.VISIBLE);
            rvUserOrders.setVisibility(View.GONE);
        } else {
            layoutEmptyOrders.setVisibility(View.GONE);
            rvUserOrders.setVisibility(View.VISIBLE);
        }
    }

    private void showCancelConfirmation(Order order) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> cancelOrderWithRestoration(order))
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelOrderWithRestoration(Order order) {
        if (order.getDocumentId() == null) return;

        db.runTransaction(transaction -> {
            if (order.getBatchDetails() != null && !order.getBatchDetails().isEmpty()) {
                List<DocumentSnapshot> snaps = new ArrayList<>();
                // READ FIRST
                for (BatchOrderItem item : order.getBatchDetails()) {
                    if (item.getBatchDocId() != null) {
                        snaps.add(transaction.get(db.collection("batches").document(item.getBatchDocId())));
                    }
                }
                
                // THEN WRITE
                int i = 0;
                for (BatchOrderItem item : order.getBatchDetails()) {
                    if (item.getBatchDocId() != null) {
                        DocumentSnapshot batchDoc = snaps.get(i++);
                        if (batchDoc.exists()) {
                            long currentQty = batchDoc.getLong("quantity") != null 
                                    ? batchDoc.getLong("quantity") : 0;
                            transaction.update(batchDoc.getReference(), 
                                    "quantity", currentQty + item.getQuantity());
                        }
                    }
                }
            }
            transaction.update(db.collection("orders").document(order.getDocumentId()), "status", "CANCELLED");
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Order cancelled", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getDocumentId() == null) return;

        db.collection("orders").document(order.getDocumentId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Order marked as " + newStatus.toLowerCase(),
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
}
