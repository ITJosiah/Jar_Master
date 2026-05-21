package com.example.aling_jar.admin;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.aling_jar.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Full-screen fragment for editing an existing product batch.
 * Pre-populates all fields with the batch's current values and
 * saves changes back to Firestore on submit.
 */
public class AdminEditBatchFragment extends Fragment {

    private static final String TAG = "AdminEditBatch";
    private static final String ARG_BATCH = "batch";

    // ─── UI ────────────────────────────────────────────────────────
    private TextView tvBatchIdDisplay;
    private TextView tvDateMade;
    private View layoutDateMade;
    private MaterialCardView cardProductLaing, cardProductSinantol;
    private TextView tvFlavorClassic, tvFlavorSpicy;
    private TextView tvIngredientPork, tvIngredientTinapa;
    private TextView tvSize320g, tvSize190g;
    private MaterialButton btnQtyMinus, btnQtyPlus, btnSaveChanges;
    private EditText etQty;
    private TextInputEditText etPrice, etNotes;

    // ─── State ─────────────────────────────────────────────────────
    private Batch batch;
    private String selectedProductName;
    private String selectedFlavor;
    private String selectedIngredient;
    private String selectedSize;
    private Calendar selectedCreatedAt = Calendar.getInstance(); // editable date

    // ─── Dependencies ──────────────────────────────────────────────
    private FirebaseFirestore db;

    // ─── Factory ───────────────────────────────────────────────────

    /**
     * Creates a new instance of the edit form pre-loaded with the given batch.
     */
    public static AdminEditBatchFragment newInstance(@NonNull Batch batch) {
        AdminEditBatchFragment fragment = new AdminEditBatchFragment();
        Bundle args = new Bundle();
        // Pass batch data as individual args since Batch is not Parcelable
        args.putString("docId", batch.getDocumentId());
        args.putString("batchId", batch.getBatchId());
        args.putString("productName", batch.getProductName());
        args.putString("flavorProfile", batch.getFlavorProfile());
        args.putString("ingredient", batch.getIngredient());
        args.putString("size", batch.getSize());
        args.putLong("quantity", batch.getQuantity());
        args.putDouble("price", batch.getPrice());
        args.putString("notes", batch.getNotes());
        args.putString("photoUrl", batch.getPhotoUrl());
        args.putString("status", batch.getStatus());
        // Pass createdAt as epoch millis so DatePicker can pre-populate
        if (batch.getCreatedAt() != null) {
            args.putLong("createdAtMillis", batch.getCreatedAt().toDate().getTime());
        }
        fragment.setArguments(args);
        return fragment;
    }

    // ─── Lifecycle ─────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_fragment_edit_batch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        restoreBatchFromArgs();
        initViews(view);
        populateFields();
        setupInteractions(view);
    }

    // ─── Restore from Bundle ────────────────────────────────────────

    private void restoreBatchFromArgs() {
        Bundle args = getArguments();
        if (args == null) return;

        batch = new Batch();
        batch.setDocumentId(args.getString("docId"));
        batch.setBatchId(args.getString("batchId"));
        batch.setProductName(args.getString("productName"));
        batch.setFlavorProfile(args.getString("flavorProfile"));
        batch.setIngredient(args.getString("ingredient"));
        batch.setSize(args.getString("size"));
        batch.setQuantity(args.getLong("quantity", 24));
        batch.setPrice(args.getDouble("price", 0));
        batch.setNotes(args.getString("notes"));
        batch.setPhotoUrl(args.getString("photoUrl"));
        batch.setStatus(args.getString("status"));

        selectedProductName = batch.getProductName();
        selectedFlavor = batch.getFlavorProfile() != null ? batch.getFlavorProfile() : "Classic";
        selectedIngredient = batch.getIngredient() != null ? batch.getIngredient() : "Pork";
        selectedSize = batch.getSize() != null ? batch.getSize() : "320g";

        // Restore createdAt into a Calendar for the DatePicker
        long createdAtMillis = args.getLong("createdAtMillis", 0);
        if (createdAtMillis > 0) {
            selectedCreatedAt.setTimeInMillis(createdAtMillis);
        }
    }

    // ─── View Init ─────────────────────────────────────────────────

    private void initViews(View root) {
        tvBatchIdDisplay    = root.findViewById(R.id.tvBatchIdDisplay);
        tvDateMade          = root.findViewById(R.id.tvDateMade);
        layoutDateMade      = root.findViewById(R.id.layoutDateMade);
        cardProductLaing    = root.findViewById(R.id.cardProductLaing);
        cardProductSinantol = root.findViewById(R.id.cardProductSinantol);
        tvFlavorClassic     = root.findViewById(R.id.tvFlavorClassic);
        tvFlavorSpicy       = root.findViewById(R.id.tvFlavorSpicy);
        tvIngredientPork    = root.findViewById(R.id.tvIngredientPork);
        tvIngredientTinapa  = root.findViewById(R.id.tvIngredientTinapa);
        tvSize320g          = root.findViewById(R.id.tvSize320g);
        tvSize190g          = root.findViewById(R.id.tvSize190g);
        btnQtyMinus         = root.findViewById(R.id.btnQtyMinus);
        btnQtyPlus          = root.findViewById(R.id.btnQtyPlus);
        etQty               = root.findViewById(R.id.etQty);
        etPrice             = root.findViewById(R.id.etPrice);
        etNotes             = root.findViewById(R.id.etNotes);
        btnSaveChanges      = root.findViewById(R.id.btnSaveChanges);
    }

    // ─── Pre-populate ───────────────────────────────────────────────

    private void populateFields() {
        if (batch == null) return;

        // Batch ID (read-only)
        if (tvBatchIdDisplay != null && batch.getBatchId() != null) {
            tvBatchIdDisplay.setText(batch.getBatchId());
        }

        // Date Made (editable)
        updateDateMadeLabel();

        // Product selection
        setSelectedProduct(selectedProductName);

        // Flavor
        setSelectedFlavor(selectedFlavor);

        // Ingredient
        setSelectedIngredient(selectedIngredient);

        // Size
        setSelectedSize(selectedSize);

        // Quantity
        setQty((int) batch.getQuantity());

        // Price
        if (etPrice != null && batch.getPrice() > 0) {
            etPrice.setText(String.format(Locale.getDefault(), "%.2f", batch.getPrice()));
        }

        // Notes
        if (etNotes != null && batch.getNotes() != null) {
            etNotes.setText(batch.getNotes());
        }
    }

    // ─── Interactions ───────────────────────────────────────────────

    private void setupInteractions(View root) {
        View btnBack = root.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Date Made picker
        if (layoutDateMade != null) {
            layoutDateMade.setOnClickListener(v -> showDatePicker());
        }

        if (cardProductLaing != null)
            cardProductLaing.setOnClickListener(v -> setSelectedProduct("Laing"));
        if (cardProductSinantol != null)
            cardProductSinantol.setOnClickListener(v -> setSelectedProduct("Sinantol"));

        if (tvFlavorClassic != null)
            tvFlavorClassic.setOnClickListener(v -> setSelectedFlavor("Classic"));
        if (tvFlavorSpicy != null)
            tvFlavorSpicy.setOnClickListener(v -> setSelectedFlavor("Spicy"));

        if (tvIngredientPork != null)
            tvIngredientPork.setOnClickListener(v -> setSelectedIngredient("Pork"));
        if (tvIngredientTinapa != null)
            tvIngredientTinapa.setOnClickListener(v -> setSelectedIngredient("Tinapa"));

        if (tvSize320g != null)
            tvSize320g.setOnClickListener(v -> setSelectedSize("320g"));
        if (tvSize190g != null)
            tvSize190g.setOnClickListener(v -> setSelectedSize("190g"));

        if (btnQtyMinus != null)
            btnQtyMinus.setOnClickListener(v -> setQty(getQty() - 1));
        if (btnQtyPlus != null)
            btnQtyPlus.setOnClickListener(v -> setQty(getQty() + 1));

        if (btnSaveChanges != null)
            btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    // ─── Date Picker ────────────────────────────────────────────────

    private void showDatePicker() {
        int y = selectedCreatedAt.get(Calendar.YEAR);
        int m = selectedCreatedAt.get(Calendar.MONTH);
        int d = selectedCreatedAt.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            selectedCreatedAt.set(year, month, day);
            updateDateMadeLabel();
        }, y, m, d)
        // Prevent picking future dates
        .show();
    }

    private void updateDateMadeLabel() {
        if (tvDateMade == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvDateMade.setText(sdf.format(selectedCreatedAt.getTime()));
    }

    // ─── Product / Flavor Helpers ───────────────────────────────────

    private void setSelectedProduct(String productName) {
        selectedProductName = productName;
        int selectedColor = 0xFF4CAF50;
        int defaultColor = 0xFFE7E7E7;
        if (cardProductLaing != null)
            cardProductLaing.setStrokeColor("Laing".equals(productName) ? selectedColor : defaultColor);
        if (cardProductSinantol != null)
            cardProductSinantol.setStrokeColor("Sinantol".equals(productName) ? selectedColor : defaultColor);
    }

    private void setSelectedSize(String size) {
        selectedSize = size;
        if (tvSize320g == null || tvSize190g == null) return;
        if ("320g".equals(size)) {
            tvSize320g.setBackgroundResource(R.drawable.bg_flavor_selected);
            tvSize320g.setTextColor(0xFF222222);
            tvSize190g.setBackgroundResource(android.R.color.transparent);
            tvSize190g.setTextColor(0xFF666666);
        } else {
            tvSize190g.setBackgroundResource(R.drawable.bg_flavor_selected);
            tvSize190g.setTextColor(0xFF222222);
            tvSize320g.setBackgroundResource(android.R.color.transparent);
            tvSize320g.setTextColor(0xFF666666);
        }
    }

    private void setSelectedFlavor(String flavor) {
        selectedFlavor = flavor;
        if (tvFlavorClassic == null || tvFlavorSpicy == null) return;
        if ("Classic".equalsIgnoreCase(flavor)) {
            tvFlavorClassic.setBackgroundResource(R.drawable.bg_flavor_selected);
            tvFlavorClassic.setTextColor(0xFF222222);
            tvFlavorSpicy.setBackgroundResource(android.R.color.transparent);
            tvFlavorSpicy.setTextColor(0xFF666666);
        } else {
            tvFlavorSpicy.setBackgroundResource(R.drawable.bg_flavor_selected);
            tvFlavorSpicy.setTextColor(0xFF222222);
            tvFlavorClassic.setBackgroundResource(android.R.color.transparent);
            tvFlavorClassic.setTextColor(0xFF666666);
        }
    }

    private void setSelectedIngredient(String ingredient) {
        selectedIngredient = ingredient;
        if (tvIngredientPork == null || tvIngredientTinapa == null) return;
        if ("Pork".equals(ingredient)) {
            tvIngredientPork.setBackgroundResource(R.drawable.bg_flavor_selected);
            tvIngredientPork.setTextColor(0xFF222222);
            tvIngredientTinapa.setBackgroundResource(android.R.color.transparent);
            tvIngredientTinapa.setTextColor(0xFF666666);
        } else {
            tvIngredientTinapa.setBackgroundResource(R.drawable.bg_flavor_selected);
            tvIngredientTinapa.setTextColor(0xFF222222);
            tvIngredientPork.setBackgroundResource(android.R.color.transparent);
            tvIngredientPork.setTextColor(0xFF666666);
        }
    }

    // ─── Quantity Helpers ───────────────────────────────────────────

    private int getQty() {
        if (etQty == null) return 1;
        try {
            String s = etQty.getText() != null ? etQty.getText().toString().trim() : "";
            return s.isEmpty() ? 1 : Integer.parseInt(s);
        } catch (Exception ignored) {
            return 1;
        }
    }

    private void setQty(int qty) {
        int safe = Math.max(1, qty);
        if (etQty != null) etQty.setText(String.format(Locale.getDefault(), "%d", safe));
        if (btnQtyMinus != null) btnQtyMinus.setEnabled(safe > 1);
    }

    // ─── Status from Age ────────────────────────────────────────────

    /**
     * Computes status based on how old the batch is from createdAt:
     *   < 30 days  → FRESH
     *   30–59 days → 30 DAYS OLD  (shows in "30 Days" filter tab)
     *   ≥ 60 days  → CRITICAL      (expired)
     */
    private String computeStatusFromCreatedAt(Calendar createdAt) {
        long nowMs = System.currentTimeMillis();
        long createdMs = createdAt.getTimeInMillis();
        long daysOld = (nowMs - createdMs) / (24L * 60L * 60L * 1000L);
        if (daysOld > 60) return "CRITICAL";
        if (daysOld > 30) return "30 DAYS OLD";
        return "FRESH";
    }

    // ─── Save ───────────────────────────────────────────────────────

    private void saveChanges() {
        if (batch == null || batch.getDocumentId() == null) {
            Toast.makeText(getContext(), "Cannot save: missing batch data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate
        if (selectedProductName == null || selectedProductName.isEmpty()) {
            Toast.makeText(getContext(), "Please select a product", Toast.LENGTH_SHORT).show();
            return;
        }

        String priceStr = etPrice != null && etPrice.getText() != null
                ? etPrice.getText().toString().trim() : "";
        if (priceStr.isEmpty()) {
            if (etPrice != null) etPrice.setError("Required");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (Exception e) {
            if (etPrice != null) etPrice.setError("Invalid price");
            return;
        }

        int quantity = getQty();
        String notes = etNotes != null && etNotes.getText() != null
                ? etNotes.getText().toString().trim() : "";

        // Recompute createdAt timestamp, expiryDate (+91 days), and status from age
        Timestamp createdAtTs = new Timestamp(selectedCreatedAt.getTime());
        Calendar expCal = (Calendar) selectedCreatedAt.clone();
        expCal.add(Calendar.DAY_OF_YEAR, 91);  // Expiry = 91 days from logged date
        Timestamp expiryDate = new Timestamp(expCal.getTime());
        String newStatus = computeStatusFromCreatedAt(selectedCreatedAt);

        Map<String, Object> updates = new HashMap<>();
        updates.put("productName", selectedProductName);
        updates.put("flavorProfile", selectedFlavor);
        updates.put("ingredient", selectedIngredient);
        updates.put("size", selectedSize);
        updates.put("quantity", (long) quantity);
        updates.put("price", price);
        updates.put("notes", notes);
        updates.put("createdAt", createdAtTs);
        updates.put("expiryDate", expiryDate);
        updates.put("status", newStatus);

        lockSubmit(true);

        db.collection("batches").document(batch.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Batch updated: " + batch.getDocumentId());
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Batch updated!", Toast.LENGTH_SHORT).show();
                    }
                    lockSubmit(false);
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update batch", e);
                    lockSubmit(false);
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void lockSubmit(boolean locked) {
        if (btnSaveChanges == null) return;
        btnSaveChanges.setEnabled(!locked);
        btnSaveChanges.setText(locked ? "Saving…" : "Save Changes");
    }
}
