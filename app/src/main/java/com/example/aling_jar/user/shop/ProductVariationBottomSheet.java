package com.example.aling_jar.user.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.aling_jar.R;
import com.example.aling_jar.data.CartManager;
import com.example.aling_jar.data.model.Batch;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class ProductVariationBottomSheet extends BottomSheetDialogFragment {

    private String productName;
    private String flavorProfile;
    private String photoUrl;
    private List<Batch> allBatches;

    // UI
    private ImageView ivProductImage;
    private TextView tvPrice;
    private TextView tvStockLabel;
    private TextView tvSelectHint;
    private RadioGroup rgMeatGroup;
    private RadioGroup rgSizeGroup;
    private View btnQtyMinus, btnQtyPlus;
    private EditText etQuantity;
    private MaterialButton btnAddToCart;

    // State
    private String selectedIngredient = null;
    private String selectedSize = null;
    private int selectedQuantity = 1;

    private int availableStock = 0;
    private double currentPrice = 0;
    private double minPrice = 0;
    private double maxPrice = 0;

    public static ProductVariationBottomSheet newInstance(String productName, String flavorProfile, String photoUrl) {
        ProductVariationBottomSheet fragment = new ProductVariationBottomSheet();
        Bundle args = new Bundle();
        args.putString("productName", productName);
        args.putString("flavorProfile", flavorProfile);
        args.putString("photoUrl", photoUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public void setAllBatches(List<Batch> allBatches) {
        this.allBatches = allBatches;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productName = getArguments().getString("productName", "");
            flavorProfile = getArguments().getString("flavorProfile", "");
            photoUrl = getArguments().getString("photoUrl", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_product_variation_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        computePriceRange();
        updateUIState();
        setupListeners();
    }

    private void initViews(View view) {
        ivProductImage = view.findViewById(R.id.ivProductImage);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvStockLabel = view.findViewById(R.id.tvStockLabel);
        tvSelectHint = view.findViewById(R.id.tvSelectHint);
        rgMeatGroup = view.findViewById(R.id.rgMeatGroup);
        rgSizeGroup = view.findViewById(R.id.rgSizeGroup);
        btnQtyMinus = view.findViewById(R.id.btnQtyMinus);
        btnQtyPlus = view.findViewById(R.id.btnQtyPlus);
        etQuantity = view.findViewById(R.id.etQuantity);
        btnAddToCart = view.findViewById(R.id.btnAddToCart);

        // Clear default selections explicitly
        rgMeatGroup.clearCheck();
        rgSizeGroup.clearCheck();

        // Load image
        int defaultRes = R.drawable.ic_placeholder_transparent;
        String pName = productName != null ? productName.toLowerCase() : "";
        if (pName.contains("sinantol")) {
            defaultRes = R.drawable.img_sinantol_default;
        } else if (pName.contains("laing")) {
            defaultRes = R.drawable.img_laing_default;
        }

        Glide.with(this)
             .load(photoUrl != null && photoUrl.startsWith("http") ? photoUrl : defaultRes)
             .transform(new CenterCrop(), new RoundedCorners(16))
             .placeholder(defaultRes)
             .error(defaultRes)
             .into(ivProductImage);
    }

    private void setupListeners() {
        rgMeatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMeatPork) selectedIngredient = "Pork";
            else if (checkedId == R.id.rbMeatTinapa) selectedIngredient = "Tinapa";
            updateUIState();
        });

        rgSizeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbSize190g) selectedSize = "190g";
            else if (checkedId == R.id.rbSize320g) selectedSize = "320g";
            updateUIState();
        });

        btnQtyMinus.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                etQuantity.setText(String.valueOf(selectedQuantity));
            }
        });

        btnQtyPlus.setOnClickListener(v -> {
            if (selectedIngredient != null && selectedSize != null) {
                if (selectedQuantity < availableStock) {
                    selectedQuantity++;
                    etQuantity.setText(String.valueOf(selectedQuantity));
                } else {
                    Toast.makeText(getContext(), "Max stock reached", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please select variations first", Toast.LENGTH_SHORT).show();
            }
        });

        etQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String input = s.toString().trim();
                if (input.isEmpty()) return;
                try {
                    int val = Integer.parseInt(input);
                    if (val == selectedQuantity) return;
                    if (selectedIngredient != null && selectedSize != null) {
                        if (val > availableStock) {
                            selectedQuantity = availableStock;
                            etQuantity.removeTextChangedListener(this);
                            etQuantity.setText(String.valueOf(selectedQuantity));
                            etQuantity.setSelection(etQuantity.getText().length());
                            etQuantity.addTextChangedListener(this);
                            Toast.makeText(getContext(), "Max stock is " + availableStock, Toast.LENGTH_SHORT).show();
                        } else if (val < 1) {
                            selectedQuantity = 1;
                        } else {
                            selectedQuantity = val;
                        }
                    } else {
                        if (val != 1) {
                            selectedQuantity = 1;
                            etQuantity.removeTextChangedListener(this);
                            etQuantity.setText("1");
                            etQuantity.setSelection(1);
                            etQuantity.addTextChangedListener(this);
                            Toast.makeText(getContext(), "Please select variations first", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        btnAddToCart.setOnClickListener(v -> handleAddToCart());
    }

    private void computePriceRange() {
        if (allBatches == null) return;
        boolean found = false;
        
        for (Batch b : allBatches) {
            String p = b.getProductName() != null ? b.getProductName() : "";
            String f = b.getFlavorProfile() != null ? b.getFlavorProfile() : "";
            
            if (productName.equalsIgnoreCase(p) && flavorProfile.equalsIgnoreCase(f)) {
                double price = b.getPrice();
                if (price > 0) {
                    if (!found) {
                        minPrice = price;
                        maxPrice = price;
                        found = true;
                    } else {
                        if (price < minPrice) minPrice = price;
                        if (price > maxPrice) maxPrice = price;
                    }
                }
            }
        }
    }

    private void updateUIState() {
        if (selectedIngredient == null || selectedSize == null) {
            // Options missing
            tvSelectHint.setVisibility(View.VISIBLE);
            tvStockLabel.setText("");
            btnAddToCart.setEnabled(false);
            btnAddToCart.setBackgroundColor(0xFFBDBDBD); // disabled color
            
            // Show price range
            if (minPrice > 0 && maxPrice > 0) {
                if (minPrice == maxPrice) {
                    tvPrice.setText(String.format(Locale.getDefault(), "₱%,.2f", minPrice));
                } else {
                    tvPrice.setText(String.format(Locale.getDefault(), "₱%,.2f - ₱%,.2f", minPrice, maxPrice));
                }
            } else {
                tvPrice.setText("₱0.00");
            }
            return;
        }

        tvSelectHint.setVisibility(View.GONE);

        // Filter batches for matching stock and price
        availableStock = 0;
        currentPrice = 0;

        if (allBatches != null) {
            for (Batch b : allBatches) {
                String p = b.getProductName() != null ? b.getProductName() : "";
                String f = b.getFlavorProfile() != null ? b.getFlavorProfile() : "";
                String i = b.getIngredient() != null ? b.getIngredient() : "";
                String s = b.getSize() != null ? b.getSize() : "";

                if (productName.equalsIgnoreCase(p) && flavorProfile.equalsIgnoreCase(f) 
                        && selectedIngredient.equalsIgnoreCase(i) && selectedSize.equalsIgnoreCase(s)) {
                    availableStock += b.getQuantity();
                    if (currentPrice == 0) { // take price of first valid batch
                        currentPrice = b.getPrice();
                    }
                }
            }
        }

        // Clip quantity if stock is lower
        if (selectedQuantity > availableStock) {
            selectedQuantity = Math.max(1, availableStock);
            etQuantity.setText(String.valueOf(selectedQuantity));
        }

        tvStockLabel.setText("Stock: " + availableStock);
        
        if (currentPrice > 0) {
            tvPrice.setText(String.format(Locale.getDefault(), "₱%,.2f", currentPrice));
        }

        if (availableStock <= 0) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setBackgroundColor(0xFFBDBDBD); // Darkened
            btnAddToCart.setText("Out of Stock");
        } else {
            btnAddToCart.setEnabled(true);
            btnAddToCart.setBackgroundColor(requireContext().getColor(R.color.green_primary));
            btnAddToCart.setText("Add to Cart");
        }
    }

    private void handleAddToCart() {
        if (selectedIngredient == null || selectedSize == null) return;
        if (availableStock <= 0) return;

        Batch cartBatch = new Batch();
        cartBatch.setProductName(productName);
        cartBatch.setFlavorProfile(flavorProfile);
        cartBatch.setIngredient(selectedIngredient);
        cartBatch.setSize(selectedSize);
        cartBatch.setPrice(currentPrice);
        cartBatch.setPhotoUrl(photoUrl);

        int qty = selectedQuantity;
        String qtyStr = etQuantity.getText().toString().trim();
        if (!qtyStr.isEmpty()) {
            try {
                qty = Integer.parseInt(qtyStr);
                if (qty < 1) qty = 1;
                if (qty > availableStock) qty = availableStock;
            } catch (NumberFormatException ignored) {}
        }

        CartManager.getInstance().addItem(cartBatch, qty);

        // Update badge on parent activity/fragment
        if (getParentFragment() instanceof UserShopFragment) {
            ((UserShopFragment) getParentFragment()).updateCartBadge(CartManager.getInstance().getItemCount());
        }

        dismiss();
        Toast.makeText(requireContext(), "Added to cart", Toast.LENGTH_SHORT).show();
    }
}
