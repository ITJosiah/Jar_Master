package com.example.aling_jar.admin;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.aling_jar.R;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminLogNewBatchFragment extends Fragment {

    private MaterialCardView cardPickBatchPhoto;
    private TextView tvBatchPhotoLabel;
    private MaterialCardView cardProductLaing, cardProductSinantol;
    private TextView tvFlavorClassic, tvFlavorSpicy;
    private MaterialButton btnQtyMinus, btnQtyPlus, btnSubmitBatch;
    private EditText etQty;
    private TextInputEditText etBatchId, etPrice, etNotes;

    private String selectedProductName = null;
    private String selectedFlavor = "Classic";
    private Uri selectedPhotoUri = null;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    private ActivityResultLauncher<String> pickImageLauncher;

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
        storage = FirebaseStorage.getInstance();

        initViews(view);
        setupPickImage();
        setupInteractions(view);

        // Defaults
        setQty(getQty());
        setSelectedFlavor("Classic");
    }

    private void initViews(View root) {
        cardPickBatchPhoto = root.findViewById(R.id.cardPickBatchPhoto);
        tvBatchPhotoLabel = root.findViewById(R.id.tvBatchPhotoLabel);
        cardProductLaing = root.findViewById(R.id.cardProductLaing);
        cardProductSinantol = root.findViewById(R.id.cardProductSinantol);
        tvFlavorClassic = root.findViewById(R.id.tvFlavorClassic);
        tvFlavorSpicy = root.findViewById(R.id.tvFlavorSpicy);
        btnQtyMinus = root.findViewById(R.id.btnQtyMinus);
        btnQtyPlus = root.findViewById(R.id.btnQtyPlus);
        etQty = root.findViewById(R.id.etQty);
        etBatchId = root.findViewById(R.id.etBatchId);
        etPrice = root.findViewById(R.id.etPrice);
        etNotes = root.findViewById(R.id.etNotes);
        btnSubmitBatch = root.findViewById(R.id.btnSubmitBatch);
    }

    private void setupPickImage() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    selectedPhotoUri = uri;
                    if (tvBatchPhotoLabel != null) {
                        tvBatchPhotoLabel.setText(uri != null ? "Photo selected" : "Upload Batch Photo");
                    }
                }
        );
    }

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

    private int getQty() {
        if (etQty == null) return 24;
        try {
            String s = etQty.getText() != null ? etQty.getText().toString().trim() : "";
            return s.isEmpty() ? 24 : Integer.parseInt(s);
        } catch (Exception ignored) {
            return 24;
        }
    }

    private void setQty(int qty) {
        int safe = Math.max(1, qty);
        if (etQty != null) etQty.setText(String.format(Locale.getDefault(), "%d", safe));
        if (btnQtyMinus != null) btnQtyMinus.setEnabled(safe > 1);
    }

    private String computeStatusFromExpiry(Timestamp expiryDate) {
        long nowMs = System.currentTimeMillis();
        long expiryMs = expiryDate.toDate().getTime();
        long daysLeft = (expiryMs - nowMs) / (24L * 60L * 60L * 1000L);

        if (daysLeft <= 7) return "Critical";
        if (daysLeft <= 20) return "Selling Fast";
        return "Fresh";
    }

    private void submitNewBatch() {
        FirebaseUser user = mAuth.getCurrentUser();
        String createdBy = user != null ? user.getUid() : null;

        String batchId = etBatchId != null && etBatchId.getText() != null
                ? etBatchId.getText().toString().trim()
                : "";
        String notes = etNotes != null && etNotes.getText() != null
                ? etNotes.getText().toString().trim()
                : "";
        String priceStr = etPrice != null && etPrice.getText() != null
                ? etPrice.getText().toString().trim()
                : "";

        if (selectedProductName == null || selectedProductName.trim().isEmpty()) {
            Toast.makeText(getContext(), "Please select a product", Toast.LENGTH_SHORT).show();
            return;
        }
        if (batchId.isEmpty()) {
            if (etBatchId != null) etBatchId.setError("Required");
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

        Timestamp createdAt = Timestamp.now();
        Calendar cal = Calendar.getInstance();
        cal.setTime(createdAt.toDate());
        cal.add(Calendar.MONTH, 1);
        Timestamp expiryDate = new Timestamp(cal.getTime());
        String status = computeStatusFromExpiry(expiryDate);

        lockSubmit(true);

        if (selectedPhotoUri != null) {
            uploadBatchPhotoThenSave(createdBy, batchId, selectedProductName, selectedFlavor, quantity, notes, price, status, expiryDate, createdAt);
        } else {
            saveBatchToFirestore(createdBy, batchId, selectedProductName, selectedFlavor, quantity, notes, null, price, status, expiryDate, createdAt);
        }
    }

    private void uploadBatchPhotoThenSave(
            String createdBy,
            String batchId,
            String productName,
            String flavorProfile,
            int quantity,
            String notes,
            double price,
            String status,
            Timestamp expiryDate,
            Timestamp createdAt
    ) {
        String safeUid = createdBy != null ? createdBy : "unknown";
        String path = "batch_photos/" + safeUid + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        ref.putFile(selectedPhotoUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        return Tasks.forException(e != null ? e : new Exception("Upload failed"));
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> saveBatchToFirestore(createdBy, batchId, productName, flavorProfile, quantity, notes, uri != null ? uri.toString() : null, price, status, expiryDate, createdAt))
                .addOnFailureListener(e -> {
                    lockSubmit(false);
                    Toast.makeText(getContext(), "Photo upload failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveBatchToFirestore(
            String createdBy,
            String batchId,
            String productName,
            String flavorProfile,
            int quantity,
            String notes,
            String photoUrl,
            double price,
            String status,
            Timestamp expiryDate,
            Timestamp createdAt
    ) {
        Map<String, Object> batchData = new HashMap<>();
        batchData.put("batchId", batchId);
        batchData.put("productName", productName);
        batchData.put("flavorProfile", flavorProfile);
        batchData.put("quantity", quantity);
        batchData.put("notes", notes);
        batchData.put("photoUrl", photoUrl);
        batchData.put("price", price);
        batchData.put("status", status);
        batchData.put("expiryDate", expiryDate);
        batchData.put("createdAt", createdAt);
        batchData.put("createdBy", createdBy);

        db.collection("batches")
                .add(batchData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(getContext(), "Batch logged successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                    lockSubmit(false);
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    lockSubmit(false);
                    Toast.makeText(getContext(), "Failed to save batch", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        selectedPhotoUri = null;
        if (tvBatchPhotoLabel != null) tvBatchPhotoLabel.setText("Upload Batch Photo");
        selectedProductName = null;
        int defaultColor = 0xFFE7E7E7;
        if (cardProductLaing != null) cardProductLaing.setStrokeColor(defaultColor);
        if (cardProductSinantol != null) cardProductSinantol.setStrokeColor(defaultColor);
        setSelectedFlavor("Classic");
        setQty(24);
        if (etBatchId != null) etBatchId.setText("");
        if (etPrice != null) etPrice.setText("");
        if (etNotes != null) etNotes.setText("");
    }

    private void lockSubmit(boolean locked) {
        if (btnSubmitBatch == null) return;
        btnSubmitBatch.setEnabled(!locked);
        btnSubmitBatch.setText(locked ? "Logging…" : "Log New Batch");
    }
}

