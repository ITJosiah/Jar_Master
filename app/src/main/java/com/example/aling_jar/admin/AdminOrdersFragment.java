package com.example.aling_jar.admin;

import android.os.Bundle;
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
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(orderAdapter);
    }

    private void buildAndUpdateDisplayList() {
        List<Object> displayList = new ArrayList<>();

        List<Order> pending = new ArrayList<>();
        List<Order> processing = new ArrayList<>();

        for (Order o : orderList) {
            String status = o.getStatus() != null ? o.getStatus().toUpperCase() : "";
            switch (selectedTabIndex) {
                case 0: // Pending tab: NEW ORDERS (PENDING) + PROCESSING (CONFIRMED)
                    if ("PENDING".equals(status)) {
                        pending.add(o);
                    } else if ("CONFIRMED".equals(status) || "PREPARING".equals(status)) {
                        processing.add(o);
                    }
                    break;
                case 1: // Confirmed: CONFIRMED only
                    if ("CONFIRMED".equals(status) || "PREPARING".equals(status)) {
                        processing.add(o);
                    }
                    break;
                case 2: // Delivery: DELIVERY, PREPARING, etc.
                    if ("DELIVERY".equals(status) || "DELIVERING".equals(status) || "PREPARING".equals(status) || "CONFIRMED".equals(status)) {
                        processing.add(o);
                    }
                    break;
                case 3: // History: DECLINED, DELIVERED, CANCELLED
                    if ("DECLINED".equals(status) || "DELIVERED".equals(status) || "CANCELLED".equals(status)) {
                        processing.add(o); // show as card without actions
                    }
                    break;
            }
        }

        if (selectedTabIndex == 0) {
            if (!pending.isEmpty()) {
                displayList.add(new AdminOrdersAdapter.SectionHeader(
                        "NEW ORDERS (" + pending.size() + ")",
                        "Today",
                        true));
                for (Order o : pending) {
                    displayList.add(new AdminOrdersAdapter.OrderCardItem(o, false));
                }
            }
            if (!processing.isEmpty()) {
                displayList.add(new AdminOrdersAdapter.SectionHeader(
                        "PROCESSING (" + processing.size() + ")",
                        "",
                        false));
                for (Order o : processing) {
                    displayList.add(new AdminOrdersAdapter.OrderCardItem(o, true));
                }
            }
        } else {
            String sectionTitle = selectedTabIndex == 1 ? "CONFIRMED"
                    : selectedTabIndex == 2 ? "DELIVERY"
                    : "HISTORY";
            if (!processing.isEmpty()) {
                displayList.add(new AdminOrdersAdapter.SectionHeader(
                        sectionTitle + " (" + processing.size() + ")",
                        "",
                        false));
                for (Order o : processing) {
                    boolean showAsProcessing = selectedTabIndex != 0;
                    displayList.add(new AdminOrdersAdapter.OrderCardItem(o, showAsProcessing));
                }
            }
        }

        orderAdapter.setItems(displayList);
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
