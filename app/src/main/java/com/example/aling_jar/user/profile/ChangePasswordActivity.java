package com.example.aling_jar.user.profile;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aling_jar.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final String TAG = "ChangePasswordActivity";

    private MaterialToolbar toolbar;
    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnUpdatePassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarChangePassword);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());
        btnUpdatePassword.setOnClickListener(v -> updatePassword());
    }

    private void updatePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmNewPassword = etConfirmNewPassword.getText().toString().trim();

        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return;
        }

        if (newPassword.isEmpty() || newPassword.length() < 6) {
            etNewPassword.setError("New password must be at least 6 characters");
            etNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            etConfirmNewPassword.setError("Passwords do not match");
            etConfirmNewPassword.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("Updating...");

        // 1. Re-authenticate the user first
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // 2. If re-auth is successful, update the password
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update password", e);
                                Toast.makeText(this, "Failed to update password", Toast.LENGTH_SHORT).show();
                                btnUpdatePassword.setEnabled(true);
                                btnUpdatePassword.setText("Update Password");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Re-authentication failed", e);
                    Toast.makeText(this, "Incorrect current password", Toast.LENGTH_SHORT).show();
                    etCurrentPassword.setError("Incorrect password");
                    etCurrentPassword.requestFocus();
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText("Update Password");
                });
    }
}
