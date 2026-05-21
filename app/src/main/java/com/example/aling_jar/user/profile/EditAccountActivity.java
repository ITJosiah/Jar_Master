package com.example.aling_jar.user.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aling_jar.R;
import com.example.aling_jar.data.model.User;
import com.example.aling_jar.utils.MapPickerActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditAccountActivity extends AppCompatActivity {

    private static final String TAG = "EditAccountActivity";

    private MaterialToolbar toolbar;
    private EditText etFullName, etEmail, etMobileNumber, etDeliveryAddress;
    private TextInputLayout tilDeliveryAddress;
    private MaterialButton btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;

    private ActivityResultLauncher<Intent> mapPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUid = mAuth.getUid();

        if (currentUid == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadUserData();

        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String newAddress = result.getData().getStringExtra("EXTRA_ADDRESS");
                        if (newAddress != null && !newAddress.isEmpty()) {
                            etDeliveryAddress.setText(newAddress);
                        }
                    }
                }
        );
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarEditAccount);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress);
        tilDeliveryAddress = findViewById(R.id.tilDeliveryAddress);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        // Launch MapPickerActivity when tapping on the address field or its icon
        etDeliveryAddress.setOnClickListener(v -> launchMapPicker());
        tilDeliveryAddress.setEndIconOnClickListener(v -> launchMapPicker());

        btnSave.setOnClickListener(v -> saveUserData());
    }

    private void launchMapPicker() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        mapPickerLauncher.launch(intent);
    }

    private void loadUserData() {
        btnSave.setEnabled(false);
        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            etFullName.setText(user.getFullName());
                            etEmail.setText(user.getEmail());
                            etMobileNumber.setText(user.getMobile());
                            etDeliveryAddress.setText(user.getAddress());
                        }
                    }
                    btnSave.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }

    private void saveUserData() {
        String fullName = etFullName.getText().toString().trim();
        String mobile = etMobileNumber.getText().toString().trim();
        String address = etDeliveryAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Full Name is required");
            etFullName.requestFocus();
            return;
        }

        if (mobile.isEmpty()) {
            etMobileNumber.setError("Mobile Number is required");
            etMobileNumber.requestFocus();
            return;
        }

        if (address.isEmpty()) {
            etDeliveryAddress.setError("Delivery Address is required");
            etDeliveryAddress.requestFocus();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("mobile", mobile);
        updates.put("address", address);

        db.collection("users").document(currentUid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Account info updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(this, "Failed to update account", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Save Changes");
                });
    }
}
