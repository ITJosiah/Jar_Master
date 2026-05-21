package com.example.aling_jar.user.shop;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.data.CartManager;
import com.example.aling_jar.data.model.Batch;
import com.example.aling_jar.data.repository.BatchRepository;
import com.example.aling_jar.user.notifications.NotificationsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the user-facing product shop with a 2-column scrollable grid.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Coordinate UI elements (search, filter chips, grid)</li>
 *   <li>Delegate data fetching to {@link BatchRepository}</li>
 *   <li>Delegate rendering to {@link ShopProductAdapter}</li>
 * </ul>
 * <p>
 * Follows Single Responsibility — this fragment only wires UI to data.
 * Business logic (filtering, sorting) could be extracted to a ViewModel
 * in a future iteration.
 */
public class UserShopFragment extends Fragment implements ShopProductAdapter.OnProductActionListener {

    private static final int GRID_SPAN_COUNT = 2;

    // ─── Data ──────────────────────────────────────────────────────
    private final BatchRepository batchRepository = new BatchRepository();
    private final List<Batch> allBatches = new ArrayList<>();

    // ─── UI ────────────────────────────────────────────────────────
    private RecyclerView rvProducts;
    private ShopProductAdapter adapter;
    private EditText etSearch;
    private ImageView ivNotification;
    private TextView chipAll, chipBestSellers, chipLaing, chipSinantol;
    private TextView activeChip;
    private final List<String> savedItemIds = new ArrayList<>(); // Format: "ProductName_Flavor"
    private com.google.firebase.firestore.ListenerRegistration shopListener;

    // ─── Lifecycle ─────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_fragment_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupProductGrid();
        setupFilterChips();
        setupSearch();
        loadSavedItems();
        loadProducts();
        updateCartBadge(CartManager.getInstance().getItemCount());
    }

    // ─── Setup ─────────────────────────────────────────────────────

    private void initViews(View view) {
        ivNotification = view.findViewById(R.id.ivNotification);
        ivNotification.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });
        rvProducts      = view.findViewById(R.id.rvProducts);
        etSearch        = view.findViewById(R.id.etSearch);
        chipAll         = view.findViewById(R.id.chipAll);
        chipBestSellers = view.findViewById(R.id.chipBestSellers);
        chipLaing       = view.findViewById(R.id.chipLaing);
        chipSinantol    = view.findViewById(R.id.chipSinantol);
        activeChip      = chipAll;
    }

    private void setupProductGrid() {
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), GRID_SPAN_COUNT);
        rvProducts.setLayoutManager(layoutManager);
        adapter = new ShopProductAdapter(new ArrayList<>(), this);
        rvProducts.setAdapter(adapter);
    }

    private void setupFilterChips() {
        chipAll.setOnClickListener(v -> selectChip(chipAll));
        chipBestSellers.setOnClickListener(v -> selectChip(chipBestSellers));
        chipLaing.setOnClickListener(v -> selectChip(chipLaing));
        chipSinantol.setOnClickListener(v -> selectChip(chipSinantol));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }



    // ─── Data Loading ──────────────────────────────────────────────

    private void loadProducts() {
        shopListener = batchRepository.fetchAvailableBatches(new BatchRepository.OnBatchesLoadedCallback() {
            @Override
            public void onSuccess(@NonNull List<Batch> batches) {
                allBatches.clear();
                allBatches.addAll(batches);

                // Create exactly 4 base products for the storefront
                applyCurrentFilters();
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ─── Filtering ─────────────────────────────────────────────────

    private void filterProducts(String query) {
        applyCurrentFilters();
    }

    private List<Batch> generateBaseProducts() {
        List<Batch> list = new ArrayList<>();
        list.add(createBaseProduct("Laing", "Classic", "https://res.cloudinary.com/dtg0tgqat/image/upload/v1731669273/Laingc_phzshn.png"));
        list.add(createBaseProduct("Laing", "Spicy", "https://res.cloudinary.com/dtg0tgqat/image/upload/v1731669299/Laingsp_pns21s.png"));
        list.add(createBaseProduct("Sinantol", "Classic", "https://res.cloudinary.com/dtg0tgqat/image/upload/v1731669272/sinc_cix7x1.png"));
        list.add(createBaseProduct("Sinantol", "Spicy", "https://res.cloudinary.com/dtg0tgqat/image/upload/v1731669273/sinp_d84wng.png"));
        return list;
    }

    private double getMinimumPriceForProduct(String name, String flavor) {
        double minPrice = Double.MAX_VALUE;
        boolean found = false;
        for (Batch b : allBatches) {
            if (b.getProductName() != null && b.getProductName().equalsIgnoreCase(name) &&
                b.getFlavorProfile() != null && b.getFlavorProfile().equalsIgnoreCase(flavor)) {
                double price = b.getPrice();
                if (price > 0 && price < minPrice) {
                    minPrice = price;
                    found = true;
                }
            }
        }
        return found ? minPrice : 175.0; // Fallback to 175 if no matching active batches
    }

    private Batch createBaseProduct(String name, String flavor, String defaultImageUrl) {
        Batch b = new Batch();
        b.setProductName(name);
        b.setFlavorProfile(flavor);
        b.setPhotoUrl(defaultImageUrl);
        
        // Calculate the dynamic minimum price among all matching active batches
        double minPrice = getMinimumPriceForProduct(name, flavor);
        b.setPrice(minPrice);
        
        // Quantity indicates generic available, won't be displayed directly in new design
        b.setQuantity(999); 
        return b;
    }

    private void selectChip(TextView chip) {
        if (chip == activeChip) return;

        // Deactivate previous
        styleChipInactive(activeChip);

        // Activate new
        styleChipActive(chip);
        activeChip = chip;

        // Apply filter based on chip
        applyChipFilter(chip);
    }

    private void styleChipActive(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_chip_active);
        chip.setTextColor(requireContext().getColor(R.color.user_chip_active_text));
    }

    private void styleChipInactive(TextView chip) {
        chip.setBackgroundResource(R.drawable.bg_chip_inactive);
        chip.setTextColor(requireContext().getColor(R.color.user_chip_inactive_text));
    }

    private void applyChipFilter(TextView chip) {
        applyCurrentFilters();
    }

    private void applyCurrentFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        List<Batch> baseProducts = generateBaseProducts();
        List<Batch> filtered = new ArrayList<>();

        for (Batch batch : baseProducts) {
            String name = batch.getProductName() != null ? batch.getProductName().toLowerCase() : "";
            String flavor = batch.getFlavorProfile() != null ? batch.getFlavorProfile().toLowerCase() : "";
            
            // Text Match
            boolean matchesSearch = query.isEmpty() || name.contains(query) || flavor.contains(query);
            
            // Chip Match
            boolean matchesChip = true;
            if (activeChip == chipLaing) {
                matchesChip = name.contains("laing") || flavor.contains("laing");
            } else if (activeChip == chipSinantol) {
                matchesChip = name.contains("sinantol") || flavor.contains("sinantol");
            }

            if (matchesSearch && matchesChip) {
                filtered.add(batch);
            }
        }
        
        adapter.updateList(filtered);
    }

    // ─── Product Actions ───────────────────────────────────────────

    @Override
    public void onAddToCartClick(@NonNull Batch batch, int position) {
        ProductVariationBottomSheet sheet = ProductVariationBottomSheet.newInstance(
                batch.getProductName(), 
                batch.getFlavorProfile(),
                batch.getPhotoUrl()
        );
        sheet.setAllBatches(allBatches);
        sheet.show(getChildFragmentManager(), "ProductVariationBottomSheet");
    }

    @Override
    public void onFavoriteClick(@NonNull Batch batch, int position) {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(requireContext(), "Please log in to save items", Toast.LENGTH_SHORT).show();
            return;
        }

        String itemId = batch.getProductName() + "_" + batch.getFlavorProfile();
        boolean isSaved = savedItemIds.contains(itemId);

        if (isSaved) {
            // Remove from saved
            db.collection("users").document(uid).collection("saved_items")
                    .document(itemId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        savedItemIds.remove(itemId);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add to saved
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("productName", batch.getProductName());
            data.put("flavorProfile", batch.getFlavorProfile());
            data.put("photoUrl", batch.getPhotoUrl());
            data.put("addedAt", com.google.firebase.Timestamp.now());

            db.collection("users").document(uid).collection("saved_items")
                    .document(itemId)
                    .set(data)
                    .addOnSuccessListener(aVoid -> {
                        savedItemIds.add(itemId);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Saved to favorites", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadSavedItems() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).collection("saved_items")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        savedItemIds.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            savedItemIds.add(doc.getId());
                        }
                        if (adapter != null) {
                            adapter.setSavedItemIds(savedItemIds);
                        }
                    }
                });
    }

    private final com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

    // ─── Cart Badge ────────────────────────────────────────────────

    /**
     * Updates the cart badge with the current item count.
     * Hides the badge when count is 0.
     *
     * @param count Number of items in the cart.
     */
    public void updateCartBadge(int count) {
        // Also update the bottom bar cart badge
        if (requireActivity() instanceof com.example.aling_jar.user.UserActivity) {
            ((com.example.aling_jar.user.UserActivity) requireActivity()).updateCartBadge(count);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (shopListener != null) {
            shopListener.remove();
        }
    }
}
