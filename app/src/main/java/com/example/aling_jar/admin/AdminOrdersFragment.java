package com.example.aling_jar.admin;

import android.os.Bundle;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminOrdersFragment extends Fragment {

    private static final String TAG = "AdminOrders";

    private RecyclerView rvOrders;
    private TextView tvOrderCount;
    private OrderAdapter orderAdapter;
    private List<Order> orderList = new ArrayList<>();
    private List<Order> filteredList = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration orderListener;
    private String currentFilter = "ALL";

    // Filter tab views
    private View tabAllOrders, tabPending, tabConfirmed, tabDeclined;

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
        tvOrderCount = view.findViewById(R.id.tvOrderCount);
        tabAllOrders = view.findViewById(R.id.tabAllOrders);
        tabPending = view.findViewById(R.id.tabPending);
        tabConfirmed = view.findViewById(R.id.tabConfirmed);
        tabDeclined = view.findViewById(R.id.tabDeclined);

        setupRecyclerView();
        setupFilterTabs();
        startRealtimeListener();
    }

    private void setupRecyclerView() {
        orderAdapter = new OrderAdapter(filteredList, new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onConfirmClick(Order order, int position) {
                updateOrderStatus(order, "CONFIRMED");
            }

            @Override
            public void onDeclineClick(Order order, int position) {
                updateOrderStatus(order, "DECLINED");
            }
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(orderAdapter);
    }

    private void setupFilterTabs() {
        tabAllOrders.setOnClickListener(v -> applyFilter("ALL"));
        tabPending.setOnClickListener(v -> applyFilter("PENDING"));
        tabConfirmed.setOnClickListener(v -> applyFilter("CONFIRMED"));
        tabDeclined.setOnClickListener(v -> applyFilter("DECLINED"));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;

        // Update tab styles
        tabAllOrders.setBackgroundResource(
                "ALL".equals(filter) ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        if (tabAllOrders instanceof TextView) {
            ((TextView) tabAllOrders).setTextColor(
                    "ALL".equals(filter) ? 0xFFFFFFFF : 0xFF555555);
        }

        // Filter the list
        filteredList.clear();
        for (Order order : orderList) {
            if ("ALL".equals(filter) || filter.equalsIgnoreCase(order.getStatus())) {
                filteredList.add(order);
            }
        }

        orderAdapter.updateList(filteredList);
        tvOrderCount.setText(filteredList.size() + " Orders");
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
                                orderList.add(order);
                            }
                        }
                        applyFilter(currentFilter);
                    }
                });
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) orderListener.remove();
    }
}
