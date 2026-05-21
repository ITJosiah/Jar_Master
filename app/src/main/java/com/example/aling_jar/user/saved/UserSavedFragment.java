package com.example.aling_jar.user.saved;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.data.model.Batch;
import com.example.aling_jar.user.notifications.NotificationsActivity;
import com.example.aling_jar.user.shop.ProductVariationBottomSheet;
import com.example.aling_jar.user.shop.ShopProductAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserSavedFragment extends Fragment implements ShopProductAdapter.OnProductActionListener {

    private RecyclerView rvSavedProducts;
    private LinearLayout layoutEmptySaved;
    private ImageView ivNotification;
    private ShopProductAdapter adapter;
    private final List<Batch> savedList = new ArrayList<>();
    private final List<String> savedItemIds = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_fragment_saved, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadSavedItems();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            loadSavedItems();
        }
    }

    private void initViews(View view) {
        rvSavedProducts = view.findViewById(R.id.rvSavedProducts);
        layoutEmptySaved = view.findViewById(R.id.layoutEmptySaved);
        ivNotification = view.findViewById(R.id.ivNotification);

        ivNotification.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), NotificationsActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        rvSavedProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new ShopProductAdapter(savedList, this);
        rvSavedProducts.setAdapter(adapter);
    }

    private void loadSavedItems() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Query all active inventory batches to compute dynamic minimum prices
        db.collection("batches")
                .whereGreaterThan("quantity", 0)
                .get()
                .addOnSuccessListener(batchSnapshots -> {
                    List<Batch> allActiveBatches = batchSnapshots.toObjects(Batch.class);
                    
                    db.collection("users").document(uid).collection("saved_items")
                            .addSnapshotListener((value, error) -> {
                                if (error != null) return;
                                if (value != null) {
                                    savedList.clear();
                                    savedItemIds.clear();
                                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                                        String pName = doc.getString("productName");
                                        String fProfile = doc.getString("flavorProfile");
                                        
                                        Batch b = new Batch();
                                        b.setProductName(pName);
                                        b.setFlavorProfile(fProfile);
                                        b.setPhotoUrl(doc.getString("photoUrl"));
                                        
                                        // Compute dynamic minimum price from inventory batches
                                        double minPrice = 175.0; // default fallback
                                        double foundMin = Double.MAX_VALUE;
                                        boolean found = false;
                                        for (Batch activeBatch : allActiveBatches) {
                                            if (activeBatch.getProductName() != null && activeBatch.getProductName().equalsIgnoreCase(pName) &&
                                                activeBatch.getFlavorProfile() != null && activeBatch.getFlavorProfile().equalsIgnoreCase(fProfile)) {
                                                double price = activeBatch.getPrice();
                                                if (price > 0 && price < foundMin) {
                                                    foundMin = price;
                                                    found = true;
                                                }
                                            }
                                        }
                                        b.setPrice(found ? foundMin : minPrice);
                                        
                                        savedList.add(b);
                                        savedItemIds.add(doc.getId());
                                    }
                                    
                                    adapter.updateList(savedList);
                                    adapter.setSavedItemIds(savedItemIds);

                                    if (savedList.isEmpty()) {
                                        layoutEmptySaved.setVisibility(View.VISIBLE);
                                        rvSavedProducts.setVisibility(View.GONE);
                                    } else {
                                        layoutEmptySaved.setVisibility(View.GONE);
                                        rvSavedProducts.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
                });
    }

    @Override
    public void onAddToCartClick(@NonNull Batch batch, int position) {
        // Open variation sheet (Note: we might need actual batches for variations, 
        // but since we are using base products here, we'll fetch them from Firestore)
        fetchBatchesAndShowVariationSheet(batch);
    }

    private void fetchBatchesAndShowVariationSheet(Batch baseBatch) {
        db.collection("batches")
                .whereEqualTo("productName", baseBatch.getProductName())
                .whereEqualTo("flavorProfile", baseBatch.getFlavorProfile())
                .whereGreaterThan("quantity", 0)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Batch> batches = queryDocumentSnapshots.toObjects(Batch.class);
                    ProductVariationBottomSheet sheet = ProductVariationBottomSheet.newInstance(
                            baseBatch.getProductName(),
                            baseBatch.getFlavorProfile(),
                            baseBatch.getPhotoUrl()
                    );
                    sheet.setAllBatches(batches);
                    sheet.show(getChildFragmentManager(), "ProductVariationBottomSheet");
                });
    }

    @Override
    public void onFavoriteClick(@NonNull Batch batch, int position) {
        // Toggle favorite (Remove from here)
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String itemId = batch.getProductName() + "_" + batch.getFlavorProfile();
        db.collection("users").document(uid).collection("saved_items")
                .document(itemId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Removed from saved", Toast.LENGTH_SHORT).show();
                });
    }
}
