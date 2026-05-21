package com.example.aling_jar.admin;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.aling_jar.R;
import com.example.aling_jar.data.repository.BatchPhotoUploader;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Admin form for logging a new product batch.
 * <p>
 * Responsibilities (Single Responsibility):
 * <ul>
 *   <li>Coordinate UI inputs (product selection, photo, quantity, price)</li>
 *   <li>Delegate photo upload to {@link BatchPhotoUploader}</li>
 *   <li>Delegate data persistence to Firestore</li>
 * </ul>
 * <p>
 * Key lifecycle handling:
 * <ul>
 *   <li>{@code selectedPhotoUri} is saved/restored via {@code onSaveInstanceState}
 *       to survive configuration changes (keyboard, rotation, memory pressure)</li>
 *   <li>A visual preview of the selected photo is shown via Glide</li>
 * </ul>
 */
public class AdminLogNewBatchFragment extends Fragment {

    private static final String TAG = "AdminLogNewBatch";
    private static final String KEY_PHOTO_URI = "selectedPhotoUri";
    private static final int DEFAULT_QUANTITY = 24;
    private static final int EXPIRY_MONTHS_AHEAD = 1;

    // ─── UI ────────────────────────────────────────────────────────
    private MaterialCardView cardPickBatchPhoto;
    private LinearLayout layoutUploadPrompt;
    private LinearLayout layoutPhotoPreview;
    private ImageView ivBatchPhotoPreview;
    private TextView tvBatchPhotoLabel;
    private MaterialCardView cardProductLaing, cardProductSinantol;
    private TextView tvFlavorClassic, tvFlavorSpicy;
    private TextView tvIngredientPork, tvIngredientTinapa;
    private TextView tvSize320g, tvSize190g;
    private MaterialButton btnQtyMinus, btnQtyPlus, btnSubmitBatch;
    private EditText etQty;
    private TextInputEditText etPrice, etNotes;
    private TextView tvBatchIdDisplay;

    // ─── State ─────────────────────────────────────────────────────
    private String selectedProductName = "Laing";
    private String selectedFlavor = "Classic";
    private String selectedIngredient = "Pork";
    private String selectedSize = "320g";
    private Uri selectedPhotoUri = null;
    private String generatedBatchId = null;

    // ─── Dependencies ──────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BatchPhotoUploader photoUploader;

    private ActivityResultLauncher<String> pickImageLauncher;

    // ─── Lifecycle ─────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_fragment_log_new_batch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        photoUploader = new BatchPhotoUploader();

        initViews(view);
        setupPickImage();
        setupInteractions(view);
        restoreState(savedInstanceState);

        // Defaults
        setQty(getQty());
        setSelectedFlavor("Classic");
        setIngredientSelected("Pork");
        setSelectedSize("320g");
        applyDefaultPrice();
        generateAndDisplayBatchId();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Persist the selected photo URI across configuration changes
        if (selectedPhotoUri != null) {
            outState.putString(KEY_PHOTO_URI, selectedPhotoUri.toString());
            Log.d(TAG, "Saved photo URI: " + selectedPhotoUri);
        }
    }

    // ─── State Restoration ─────────────────────────────────────────

    /**
     * Restores the photo URI from savedInstanceState if the fragment was recreated.
     * This fixes the bug where the photo would be lost on configuration change.
     */
    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String uriString = savedInstanceState.getString(KEY_PHOTO_URI);
            if (uriString != null && !uriString.isEmpty()) {
                selectedPhotoUri = Uri.parse(uriString);
                Log.d(TAG, "Restored photo URI: " + selectedPhotoUri);
                showPhotoPreview(selectedPhotoUri);
            }
        }
    }

    // ─── View Init ─────────────────────────────────────────────────

    private void initViews(View root) {
        cardPickBatchPhoto = root.findViewById(R.id.cardPickBatchPhoto);
        layoutUploadPrompt = root.findViewById(R.id.layoutUploadPrompt);
        layoutPhotoPreview = root.findViewById(R.id.layoutPhotoPreview);
        ivBatchPhotoPreview = root.findViewById(R.id.ivBatchPhotoPreview);
        tvBatchPhotoLabel = root.findViewById(R.id.tvBatchPhotoLabel);
        cardProductLaing = root.findViewById(R.id.cardProductLaing);
        cardProductSinantol = root.findViewById(R.id.cardProductSinantol);
        tvFlavorClassic = root.findViewById(R.id.tvFlavorClassic);
        tvFlavorSpicy = root.findViewById(R.id.tvFlavorSpicy);
        tvIngredientPork = root.findViewById(R.id.tvIngredientPork);
        tvIngredientTinapa = root.findViewById(R.id.tvIngredientTinapa);
        tvSize320g = root.findViewById(R.id.tvSize320g);
        tvSize190g = root.findViewById(R.id.tvSize190g);
        btnQtyMinus = root.findViewById(R.id.btnQtyMinus);
        btnQtyPlus = root.findViewById(R.id.btnQtyPlus);
        etQty = root.findViewById(R.id.etQty);
        tvBatchIdDisplay = root.findViewById(R.id.tvBatchIdDisplay);
        etPrice = root.findViewById(R.id.etPrice);
        etNotes = root.findViewById(R.id.etNotes);
        btnSubmitBatch = root.findViewById(R.id.btnSubmitBatch);
    }

    // ─── Image Picker ──────────────────────────────────────────────

    private void setupPickImage() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::onImagePicked
        );
    }

    /**
     * Callback fired when the user picks (or cancels) an image.
     * Uses Glide to load a preview into the card.
     */
    private void onImagePicked(@Nullable Uri uri) {
        selectedPhotoUri = uri;
        Log.d(TAG, "Image picker returned: " + (uri != null ? uri.toString() : "null (cancelled)"));

        if (uri != null) {
            showPhotoPreview(uri);
        } else {
            hidePhotoPreview();
        }
    }

    /**
     * Displays the selected photo in the preview ImageView and hides the upload prompt.
     */
    private void showPhotoPreview(@NonNull Uri uri) {
        if (ivBatchPhotoPreview == null || layoutUploadPrompt == null || layoutPhotoPreview == null) return;

        layoutUploadPrompt.setVisibility(View.GONE);
        layoutPhotoPreview.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(uri)
                .into(ivBatchPhotoPreview);

        if (tvBatchPhotoLabel != null) {
            tvBatchPhotoLabel.setText("Photo selected");
        }
    }

    /**
     * Hides the preview and re-shows the upload prompt.
     */
    private void hidePhotoPreview() {
        if (layoutPhotoPreview == null || layoutUploadPrompt == null) return;

        layoutPhotoPreview.setVisibility(View.GONE);
        layoutUploadPrompt.setVisibility(View.VISIBLE);

        if (tvBatchPhotoLabel != null) {
            tvBatchPhotoLabel.setText("Upload Batch Photo");
        }
    }

    // ─── UI Interactions ───────────────────────────────────────────

    private void setupInteractions(View root) {
        View btnBack = root.findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        if (cardPickBatchPhoto != null) {
            cardPickBatchPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }

        if (cardProductLaing != null) {
            cardProductLaing.setOnClickListener(v -> setSelectedProduct("Laing"));
        }
        if (cardProductSinantol != null) {
            cardProductSinantol.setOnClickListener(v -> setSelectedProduct("Sinantol"));
        }

        if (tvFlavorClassic != null) tvFlavorClassic.setOnClickListener(v -> setSelectedFlavor("Classic"));
        if (tvFlavorSpicy != null) tvFlavorSpicy.setOnClickListener(v -> setSelectedFlavor("Spicy"));

        if (tvIngredientPork != null) tvIngredientPork.setOnClickListener(v -> setIngredientSelected("Pork"));
        if (tvIngredientTinapa != null) tvIngredientTinapa.setOnClickListener(v -> setIngredientSelected("Tinapa"));

        if (tvSize320g != null) tvSize320g.setOnClickListener(v -> setSelectedSize("320g"));
        if (tvSize190g != null) tvSize190g.setOnClickListener(v -> setSelectedSize("190g"));

        if (btnQtyMinus != null) btnQtyMinus.setOnClickListener(v -> setQty(getQty() - 1));
        if (btnQtyPlus != null) btnQtyPlus.setOnClickListener(v -> setQty(getQty() + 1));

        if (btnSubmitBatch != null) {
            btnSubmitBatch.setOnClickListener(v -> submitNewBatch());
        }
    }

    private void setSelectedProduct(String productName) {
        selectedProductName = productName;
        int selectedColor = 0xFF4CAF50;
        int defaultColor = 0xFFE7E7E7;
        if (cardProductLaing != null) {
            cardProductLaing.setStrokeColor("Laing".equals(productName) ? selectedColor : defaultColor);
        }
        if (cardProductSinantol != null) {
            cardProductSinantol.setStrokeColor("Sinantol".equals(productName) ? selectedColor : defaultColor);
        }
    }

    private void setIngredientSelected(String type) {
        selectedIngredient = type;
        if (tvIngredientPork == null || tvIngredientTinapa == null) return;
        if ("Pork".equals(type)) {
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
        applyDefaultPrice();
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
        applyDefaultPrice();
    }

    /**
     * Sets the suggested default price based on size + flavor.
     * Only overwrites if the field is blank or currently holds a known default,
     * so manual edits from the admin are always preserved.
     *
     * Price matrix:
     *   320g Classic → ₱200  |  190g Classic → ₱100
     *   320g Spicy   → ₱210  |  190g Spicy   → ₱110
     */
    private void applyDefaultPrice() {
        if (etPrice == null) return;

        String currentText = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";

        // Known default values — only auto-fill if one of these is showing (or blank)
        boolean isDefault = currentText.isEmpty()
                || currentText.equals("200") || currentText.equals("200.00")
                || currentText.equals("100") || currentText.equals("100.00")
                || currentText.equals("210") || currentText.equals("210.00")
                || currentText.equals("110") || currentText.equals("110.00");

        if (!isDefault) return; // Admin has typed a custom value — don't overwrite

        double defaultPrice;
        boolean is320g = "320g".equals(selectedSize);
        boolean isClassic = "Classic".equalsIgnoreCase(selectedFlavor);

        if (is320g && isClassic)       defaultPrice = 200;
        else if (!is320g && isClassic) defaultPrice = 100;
        else if (is320g)               defaultPrice = 210;
        else                           defaultPrice = 110;

        etPrice.setText(String.valueOf((int) defaultPrice));
    }

    // ─── Quantity Helpers ──────────────────────────────────────────

    private int getQty() {
        if (etQty == null) return DEFAULT_QUANTITY;
        try {
            String s = etQty.getText() != null ? etQty.getText().toString().trim() : "";
            return s.isEmpty() ? DEFAULT_QUANTITY : Integer.parseInt(s);
        } catch (Exception ignored) {
            return DEFAULT_QUANTITY;
        }
    }

    private void setQty(int qty) {
        int safe = Math.max(1, qty);
        if (etQty != null) etQty.setText(String.format(Locale.getDefault(), "%d", safe));
        if (btnQtyMinus != null) btnQtyMinus.setEnabled(safe > 1);
    }

    // ─── Batch ID Generation ───────────────────────────────────────

    /**
     * Generates a unique, human-readable Batch ID.
     * Format: B-[DayOfYear]-[Month]-[RandomSuffix]
     * Example: B-092-MAY-4K2P
     */
    private void generateAndDisplayBatchId() {
        Calendar cal = Calendar.getInstance();
        String dayOfYear = String.format(Locale.getDefault(), "%03d", cal.get(Calendar.DAY_OF_YEAR));
        String month = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.ENGLISH).toUpperCase();
        String random = java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        generatedBatchId = String.format("B-%s-%s-%s", dayOfYear, month, random);

        if (tvBatchIdDisplay != null) {
            tvBatchIdDisplay.setText(generatedBatchId);
            tvBatchIdDisplay.setTextColor(0xFF2E7D32); // Success/Greenish color to indicate it's active
        }
    }

    // ─── Batch Status ─────────────────────────────────────────────

    private String computeStatusFromCreatedAt(Timestamp createdAt) {
        if (createdAt == null) return "FRESH";
        long nowMs = System.currentTimeMillis();
        long createdMs = createdAt.toDate().getTime();
        long daysOld = (nowMs - createdMs) / (24L * 60L * 60L * 1000L);

        if (daysOld > 60) return "CRITICAL";
        if (daysOld > 30) return "30 DAYS OLD";
        return "FRESH";
    }

    // ─── Submit ────────────────────────────────────────────────────

    /**
     * Validates inputs, then submits the batch — uploading the photo first if selected.
     */
    private void submitNewBatch() {
        // 1. Validate all inputs
        FirebaseUser user = mAuth.getCurrentUser();
        String createdBy = user != null ? user.getUid() : null;

        String batchId = generatedBatchId != null ? generatedBatchId : "";
        String notes = getTextFromInput(etNotes);
        String priceStr = getTextFromInput(etPrice);

        if (selectedProductName == null || selectedProductName.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please select a product", Toast.LENGTH_SHORT).show();
            return;
        }
        if (batchId.isEmpty()) {
            Toast.makeText(getContext(), "Batch ID generation error", Toast.LENGTH_SHORT).show();
            return;
        }
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

        // 2. Compute timestamps and status
        Timestamp createdAt = Timestamp.now();
        Calendar cal = Calendar.getInstance();
        cal.setTime(createdAt.toDate());
        cal.add(Calendar.DAY_OF_YEAR, 91);  // Expiry = 91 days from logged date
        Timestamp expiryDate = new Timestamp(cal.getTime());
        String status = computeStatusFromCreatedAt(createdAt);

        // 3. Build the batch data map (avoids 11-parameter methods)
        Map<String, Object> batchData = buildBatchData(
                createdBy, batchId, selectedProductName, selectedFlavor, selectedIngredient, selectedSize,
                quantity, notes, null, price, status, expiryDate, createdAt
        );

        lockSubmit(true);
        Log.d(TAG, "Submitting batch: product=" + selectedProductName
                + ", photoUri=" + (selectedPhotoUri != null ? selectedPhotoUri : "none"));

        // 4. Upload photo first (if selected), then save to Firestore
        if (selectedPhotoUri != null) {
            uploadPhotoThenSave(createdBy, batchData);
        } else {
            Log.d(TAG, "No photo selected, saving batch directly");
            saveBatchToFirestore(batchData);
        }
    }

    // ─── Photo Upload ─────────────────────────────────────────────

    /**
     * Delegates photo upload to {@link BatchPhotoUploader}, then saves batch with the URL.
     */
    private void uploadPhotoThenSave(@Nullable String userId, Map<String, Object> batchData) {
        photoUploader.uploadPhoto(requireContext(), selectedPhotoUri, userId, new BatchPhotoUploader.OnUploadCompleteListener() {
            @Override
            public void onSuccess(@NonNull String downloadUrl) {
                Log.d(TAG, "Photo uploaded successfully: " + downloadUrl);
                batchData.put("photoUrl", downloadUrl);
                saveBatchToFirestore(batchData);
            }

            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Photo upload failed", e);
                lockSubmit(false);
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            "Photo upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // ─── Firestore Persistence ────────────────────────────────────

    /**
     * Saves the batch data map to Firestore.
     * Single entry point for all save operations — with or without photo.
     */
    private void saveBatchToFirestore(Map<String, Object> batchData) {
        Log.d(TAG, "Saving batch to Firestore. photoUrl = " + batchData.get("photoUrl"));

        db.collection("batches")
                .add(batchData)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Batch saved: docId=" + docRef.getId());
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Batch logged successfully!", Toast.LENGTH_SHORT).show();
                    }
                    clearForm();
                    lockSubmit(false);
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save batch to Firestore", e);
                    lockSubmit(false);
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to save batch: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── Data Builder ─────────────────────────────────────────────

    /**
     * Builds the Firestore document map for a batch.
     * Centralizes field naming — single source of truth for the document schema.
     */
    private Map<String, Object> buildBatchData(
            String createdBy, String batchId, String productName,
            String flavorProfile, String ingredient, String size, int quantity, String notes,
            @Nullable String photoUrl, double price, String status,
            Timestamp expiryDate, Timestamp createdAt
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("batchId", batchId);
        data.put("productName", productName);
        data.put("flavorProfile", flavorProfile);
        data.put("ingredient", ingredient);
        data.put("size", size);
        data.put("quantity", quantity);
        data.put("notes", notes);
        data.put("photoUrl", photoUrl);
        data.put("price", price);
        data.put("status", status);
        data.put("expiryDate", expiryDate);
        data.put("createdAt", createdAt);
        data.put("createdBy", createdBy);
        return data;
    }

    // ─── Helpers ──────────────────────────────────────────────────

    /**
     * Safely extracts trimmed text from an EditText (null-safe).
     */
    private String getTextFromInput(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    private void clearForm() {
        selectedPhotoUri = null;
        hidePhotoPreview();
        selectedProductName = null;
        int defaultColor = 0xFFE7E7E7;
        if (cardProductLaing != null) cardProductLaing.setStrokeColor(defaultColor);
        if (cardProductSinantol != null) cardProductSinantol.setStrokeColor(defaultColor);
        setSelectedFlavor("Classic");
        setSelectedSize("320g");
        // Reset price to default for Classic 320g
        if (etPrice != null) etPrice.setText("200");
        setQty(DEFAULT_QUANTITY);
        generateAndDisplayBatchId();
        if (etNotes != null) etNotes.setText("");
    }

    private void lockSubmit(boolean locked) {
        if (btnSubmitBatch == null) return;
        btnSubmitBatch.setEnabled(!locked);
        btnSubmitBatch.setText(locked ? "Logging…" : "Log New Batch");
    }
}
