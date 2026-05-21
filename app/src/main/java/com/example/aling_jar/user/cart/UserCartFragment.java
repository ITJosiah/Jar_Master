package com.example.aling_jar.user.cart;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;

import com.example.aling_jar.R;
import com.example.aling_jar.admin.Order;
import com.example.aling_jar.data.CartManager;
import com.example.aling_jar.data.model.BatchOrderItem;
import com.example.aling_jar.data.model.CartItem;
import com.example.aling_jar.user.notifications.NotificationsActivity;
import com.example.aling_jar.utils.MapPickerActivity;
import com.example.aling_jar.user.UserActivity;
import com.example.aling_jar.user.shop.CartAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserCartFragment extends Fragment {

    private static final String TAG = "UserCartFragment";
    private RecyclerView rvCartItems;
    private TextView tvTotalAmount;
    private TextView tvSubtotal;
    private TextView tvDeliveryFee;
    private TextView tvCartItemCount;
    private TextInputEditText etAddress;
    private TextInputLayout tilAddress;
    private MaterialButton btnPlaceOrder;
    private MaterialButton btnAddMoreItems;
    private CartAdapter adapter;
    private ImageView ivNotification;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    private double currentSubtotal = 0;
    private double currentDeliveryFee = 0;
    private String currentUserFullName = "Guest";

    private ActivityResultLauncher<Intent> mapPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        String newAddress = result.getData().getStringExtra("address");
                        if (newAddress != null) {
                            etAddress.setText(newAddress);
                            updateUI(); // Recalculate fee when address brings new context
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupCartList();
        updateUI();

        btnPlaceOrder.setOnClickListener(v -> handlePlaceOrder());
        btnAddMoreItems.setOnClickListener(v -> {
             if (getActivity() instanceof UserActivity) {
                ((UserActivity) getActivity()).navigateToShop();
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            setupCartList();
            updateUI();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupCartList();
        updateUI();
    }

    private void initViews(View view) {
        rvCartItems = view.findViewById(R.id.rvCartItems);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvDeliveryFee = view.findViewById(R.id.tvDeliveryFee);
        tvCartItemCount = view.findViewById(R.id.tvCartItemCount);
        tilAddress = view.findViewById(R.id.tilAddress);
        etAddress = view.findViewById(R.id.etAddress);
        btnPlaceOrder = view.findViewById(R.id.btnPlaceOrder);
        btnAddMoreItems = view.findViewById(R.id.btnAddMoreItems);
        ivNotification = view.findViewById(R.id.ivNotification);

        ivNotification.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });
        
        etAddress.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });
        
        tilAddress.setEndIconOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MapPickerActivity.class);
            mapPickerLauncher.launch(intent);
        });

        loadUserAddress();
    }

    private void setupCartList() {
        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CartAdapter(CartManager.getInstance().getCartItems(), this::updateUI);
        rvCartItems.setAdapter(adapter);
    }

    private void updateUI() {
        currentSubtotal = CartManager.getInstance().getTotalPrice();
        int count = CartManager.getInstance().getItemCount();
        
        String address = etAddress.getText().toString().trim();
        currentDeliveryFee = calculateDeliveryFee(address);

        double finalTotal = currentSubtotal + currentDeliveryFee;
        
        tvCartItemCount.setText(count + (count == 1 ? " Item" : " Items"));
        tvSubtotal.setText(String.format(Locale.getDefault(), "₱%,.2f", currentSubtotal));
        tvDeliveryFee.setText(currentDeliveryFee > 0 ? String.format(Locale.getDefault(), "₱%,.2f", currentDeliveryFee) : "₱0.00");
        tvTotalAmount.setText(String.format(Locale.getDefault(), "₱%,.2f", finalTotal));
        
        if (count == 0) {
            btnPlaceOrder.setEnabled(false);
            btnPlaceOrder.setText("Cart is Empty");
        } else {
            btnPlaceOrder.setEnabled(true);
            btnPlaceOrder.setText("Place Order");
        }
        
        if (adapter != null) {
            // Only force refresh the list if an item was completely deleted/removed.
            // This preserves the soft keyboard focus while the user is actively typing.
            if (adapter.getItemCount() != CartManager.getInstance().getCartItems().size()) {
                setupCartList();
            }
        }

        // Update badge everywhere logic (on the activity)
        if (getActivity() instanceof UserActivity) {
            ((UserActivity) getActivity()).updateCartBadge(count);
        }
    }

    private double calculateDeliveryFee(String address) {
        if (address.isEmpty() || address.equalsIgnoreCase("Select your delivery location")) {
            return 0; // Default until location is picked
        }
        
        String lowerAddress = address.toLowerCase();
        
        // Tier 1: Daet (₱50)
        if (lowerAddress.contains("daet")) {
            return 50.0;
        }
        
        // Tier 2: Within Camarines Norte (₱100)
        if (lowerAddress.contains("camarines norte")) {
            return 100.0;
        }
        
        // Tier 3: Outside (₱200)
        return 200.0;
    }

    private void loadUserAddress() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String savedAddress = documentSnapshot.getString("address");
                    if (savedAddress != null && !savedAddress.isEmpty()) {
                        etAddress.setText(savedAddress);
                        updateUI(); // Trigger fee calculation for saved address
                    }
                    String fullName = documentSnapshot.getString("fullName");
                    if (fullName != null && !fullName.isEmpty()) {
                        currentUserFullName = fullName;
                    }
                }
            });
        }
    }

    private void handlePlaceOrder() {
        String address = etAddress.getText().toString().trim();
        if (address.isEmpty() || address.equalsIgnoreCase("Select your delivery location")) {
            Toast.makeText(requireContext(), "Please select a delivery address", Toast.LENGTH_SHORT).show();
            return;
        }

        List<CartItem> items = CartManager.getInstance().getCartItems();
        if (items.isEmpty() || currentSubtotal <= 0) {
            Toast.makeText(requireContext(), "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlaceOrder.setEnabled(false);
        btnPlaceOrder.setText("Processing...");

        placeOrderTransaction(items, address);
    }

    private void placeOrderTransaction(List<CartItem> cartItems, String address) {
        executeOrderPlacement(cartItems, address);
    }
    
    private void executeOrderPlacement(List<CartItem> cartItems, String address) {
        // Fetch all batches with stock to process FIFO locally
        db.collection("batches")
                .whereGreaterThan("quantity", 0)
                .get()
                .addOnSuccessListener(batchSnapshots -> {
                    processCheckout(cartItems, address, batchSnapshots.getDocuments());
                })
                .addOnFailureListener(e -> {
                    handleError("Failed to check inventory", e);
                });
    }

    private void processCheckout(List<CartItem> cartItems, String address, List<DocumentSnapshot> allBatches) {
        db.runTransaction(transaction -> {
            List<BatchOrderItem> batchDetails = new ArrayList<>();
            StringBuilder orderItemsString = new StringBuilder();
            double calculatedSubtotal = 0;

            // 1. Determine which batches we MIGHT need to read from the transaction.
            // We group matching batches per cart item.
            java.util.Map<CartItem, List<DocumentSnapshot>> itemBatchesMap = new java.util.HashMap<>();
            List<com.google.firebase.firestore.DocumentReference> refsToRead = new ArrayList<>();

            for (CartItem cartItem : cartItems) {
                String productName = cartItem.getProductName();
                String flavor = cartItem.getFlavorProfile();
                String ingredient = cartItem.getIngredient() != null ? cartItem.getIngredient() : "";
                String size = cartItem.getSize() != null ? cartItem.getSize() : "";

                List<DocumentSnapshot> matchingBatches = new ArrayList<>();
                for (DocumentSnapshot doc : allBatches) {
                    String p = doc.getString("productName");
                    String f = doc.getString("flavorProfile");
                    String i = doc.getString("ingredient") != null ? doc.getString("ingredient") : "";
                    String s = doc.getString("size") != null ? doc.getString("size") : "";

                    if (productName.equals(p) && flavor.equals(f) && ingredient.equals(i) && size.equals(s)) {
                        matchingBatches.add(doc);
                        refsToRead.add(doc.getReference());
                    }
                }

                // Sort by createdAt ASC for FIFO
                matchingBatches.sort((d1, d2) -> {
                    Timestamp t1 = d1.getTimestamp("createdAt");
                    Timestamp t2 = d2.getTimestamp("createdAt");
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t1.compareTo(t2);
                });
                
                itemBatchesMap.put(cartItem, matchingBatches);
            }

            // 2. Perform ALL reads (transaction.get) FIRST.
            java.util.Map<String, Long> batchQuantities = new java.util.HashMap<>();
            for (com.google.firebase.firestore.DocumentReference ref : refsToRead) {
                DocumentSnapshot snap = transaction.get(ref);
                Long qty = snap.getLong("quantity");
                batchQuantities.put(ref.getId(), qty != null ? qty : 0L);
            }

            // 3. Process logic and perform ALL writes (transaction.update / transaction.set)
            for (CartItem cartItem : cartItems) {
                long needed = cartItem.getQuantity();
                String productName = cartItem.getProductName();
                String flavor = cartItem.getFlavorProfile();
                String ingredient = cartItem.getIngredient() != null ? cartItem.getIngredient() : "";
                String size = cartItem.getSize() != null ? cartItem.getSize() : "";

                List<DocumentSnapshot> matchingBatches = itemBatchesMap.get(cartItem);
                long obtained = 0;

                for (DocumentSnapshot batchSnapshot : matchingBatches) {
                    if (obtained >= needed) break;

                    String batchId = batchSnapshot.getId();
                    long available = batchQuantities.get(batchId);
                    long take = Math.min(available, needed - obtained);

                    if (take > 0) {
                        // Update our local map in case the same batch is used again (though unlikely)
                        batchQuantities.put(batchId, available - take);
                        // Queue the write
                        transaction.update(batchSnapshot.getReference(), "quantity", available - take);
                        batchDetails.add(new BatchOrderItem(batchId, take));
                        obtained += take;
                    }
                }

                if (obtained < needed) {
                    throw new RuntimeException("Insufficient stock for: " + productName + " (" + flavor + ")");
                }

                if (orderItemsString.length() > 0) orderItemsString.append(", ");
                orderItemsString.append(cartItem.getQuantity()).append("x ").append(productName);

                StringBuilder itemDetails = new StringBuilder();
                if (size != null && !size.isEmpty()) itemDetails.append(size);
                if (flavor != null && !flavor.isEmpty()) {
                    if (itemDetails.length() > 0) itemDetails.append(" · ");
                    itemDetails.append(flavor);
                }
                if (ingredient != null && !ingredient.isEmpty()) {
                    if (itemDetails.length() > 0) itemDetails.append(" · ");
                    itemDetails.append(ingredient);
                }

                if (itemDetails.length() > 0) {
                    orderItemsString.append(" (").append(itemDetails.toString()).append(")");
                }

                calculatedSubtotal += cartItem.getTotalPrice();
            }

            double orderDeliveryFee = calculateDeliveryFee(address);
            double finalTotal = calculatedSubtotal + orderDeliveryFee;

            String userId = FirebaseAuth.getInstance().getUid();
            String orderId = "ORD-" + System.currentTimeMillis();

            Order order = new Order();
            order.setOrderId(orderId);
            order.setCustomerName(currentUserFullName);
            order.setAddress(address);
            order.setItems(orderItemsString.toString());
            order.setTotal(finalTotal);
            order.setStatus("PENDING");
            order.setCreatedAt(Timestamp.now());
            order.setUserId(userId);
            order.setBatchDetails(batchDetails);

            // Create Order write
            transaction.set(db.collection("orders").document(), order);

            return new Object[]{orderId, finalTotal};
        }).addOnSuccessListener(result -> {
            Object[] data = (Object[]) result;
            String orderId = (String) data[0];
            double finalTotal = (double) data[1];

            CartManager.getInstance().clear();
            Toast.makeText(requireContext(), "Order placed successfully!", Toast.LENGTH_LONG).show();

            // Notify all admins about the new order
            notifyAdminsOfNewOrder(orderId, currentUserFullName, finalTotal);

            // Automatically switch to orders tab
            if (getActivity() instanceof UserActivity) {
                ((UserActivity) getActivity()).navigateToOrders();
            }
        }).addOnFailureListener(e -> {
            handleError(e.getMessage(), e);
        });
    }

    private void notifyAdminsOfNewOrder(String orderId, String customerName, double total) {
        db.collection("users")
                .whereEqualTo("role", "admin")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String adminUid = doc.getId();
                        com.example.aling_jar.utils.NotificationHelper.sendNotification(
                                adminUid,
                                "New Order Received! 📦",
                                "Order #" + orderId + " has been placed by " + customerName + " for ₱" + String.format(java.util.Locale.getDefault(), "%,.2f", total) + ".",
                                "NEW_ORDER"
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch admins for order notification", e);
                });
    }

    private void handleError(String message, Exception e) {
        Log.e(TAG, message, e);
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            btnPlaceOrder.setEnabled(true);
            btnPlaceOrder.setText("Place Order");
        }
    }
}
