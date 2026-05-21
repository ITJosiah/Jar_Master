package com.example.aling_jar.admin;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.data.model.BatchOrderItem;
import com.example.aling_jar.utils.CustomDialog;
import com.example.aling_jar.utils.NotificationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminOrdersFragment extends Fragment {

    private static final String TAG = "AdminOrders";

    private RecyclerView rvOrders;
    private TabLayout tabLayout;
    private AdminOrdersAdapter orderAdapter;
    private List<Order> orderList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration orderListener;
    private int selectedTabIndex = 0; // 0=Pending, 1=Confirmed, 2=Delivery, 3=History

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        rvOrders = view.findViewById(R.id.rvOrders);
        tabLayout = view.findViewById(R.id.tabLayout);

        setupTabs();
        setupRecyclerView();
        startRealtimeListener();

        View btnNotification = view.findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), com.example.aling_jar.user.notifications.NotificationsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Confirmed"));
        tabLayout.addTab(tabLayout.newTab().setText("Delivery"));
        tabLayout.addTab(tabLayout.newTab().setText("History"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                buildAndUpdateDisplayList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    private void setupRecyclerView() {
        orderAdapter = new AdminOrdersAdapter(new AdminOrdersAdapter.OnOrderActionListener() {
            @Override
            public void onConfirmClick(Order order, int position) {
                updateOrderStatus(order, "CONFIRMED");
            }

            @Override
            public void onDeclineClick(Order order, int position) {
                updateOrderStatus(order, "DECLINED");
            }

            @Override
            public void onDeliverClick(Order order, int position) {
                updateOrderStatus(order, "DELIVERY");
            }
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(orderAdapter);
    }

    private void buildAndUpdateDisplayList() {
        List<Order> displayList = new ArrayList<>();

        for (Order o : orderList) {
            String status = o.getStatus() != null ? o.getStatus().toUpperCase() : "";
            switch (selectedTabIndex) {
                case 0: // Pending tab: PENDING only
                    if ("PENDING".equals(status)) {
                        displayList.add(o);
                    }
                    break;
                case 1: // Confirmed tab: CONFIRMED only
                    if ("CONFIRMED".equals(status) || "PREPARING".equals(status)) {
                        displayList.add(o);
                    }
                    break;
                case 2: // Delivery tab: DELIVERY only
                    if ("DELIVERY".equals(status) || "DELIVERING".equals(status)) {
                        displayList.add(o);
                    }
                    break;
                case 3: // History tab: DECLINED, DELIVERED, CANCELLED
                    if ("DECLINED".equals(status) || "DELIVERED".equals(status) || "CANCELLED".equals(status)) {
                        displayList.add(o);
                    }
                    break;
            }
        }

        orderAdapter.setItems(displayList, selectedTabIndex);
    }

    private void startRealtimeListener() {
        orderListener = db.collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Order listener error", error);
                        return;
                    }
                    if (snapshots != null) {
                        orderList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                order.setDocumentId(doc.getId());
                                if (order.getOrderId() == null) {
                                    order.setOrderId(doc.getId().length() >= 4
                                            ? doc.getId().substring(doc.getId().length() - 4)
                                            : doc.getId());
                                }
                                orderList.add(order);
                            }
                        }
                        buildAndUpdateDisplayList();
                    }
                });
    }

    private void updateOrderStatus(Order order, String newStatus) {
        if (order.getDocumentId() == null) return;

        if ("DECLINED".equals(newStatus)) {
            // Run transaction to restore stock AND update status
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
                transaction.update(db.collection("orders").document(order.getDocumentId()), "status", newStatus);
                return null;
            }).addOnSuccessListener(aVoid -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Order declined & stock restored", Toast.LENGTH_SHORT).show();
                }
                // Notify the user that their order was declined
                if (order.getUserId() != null) {
                    NotificationHelper.notifyOrderDeclined(order.getUserId(), order.getOrderId());
                }
            }).addOnFailureListener(e -> {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to decline order", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Just update the status normally (e.g. CONFIRMED)
            db.collection("orders").document(order.getDocumentId())
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Order " + newStatus.toLowerCase(),
                                    Toast.LENGTH_SHORT).show();
                        }
                        // Send the appropriate notification based on the new status
                        if (order.getUserId() != null) {
                            if ("CONFIRMED".equals(newStatus)) {
                                NotificationHelper.notifyOrderConfirmed(order.getUserId(), order.getOrderId());
                            } else if ("DELIVERY".equals(newStatus)) {
                                NotificationHelper.notifyOrderOutForDelivery(order.getUserId(), order.getOrderId());
                            }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) orderListener.remove();
    }
}
