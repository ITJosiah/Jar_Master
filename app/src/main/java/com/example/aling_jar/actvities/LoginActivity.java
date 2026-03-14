package com.example.aling_jar.actvities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aling_jar.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    // Views
    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private CheckBox cbRememberMe;
    private Button btnSignIn;
    private TextView tvForgotPassword, tvRequestAccess;

    // SharedPreferences for Remember Me
    private static final String PREFS_NAME  = "AlingMasterPrefs";
    private static final String KEY_USERNAME = "saved_username";
    private static final String KEY_REMEMBER = "remember_me";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Transparent status bar
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadRememberedUser();
        setClickListeners();
    }

    // ─────────────────────────────────────────────
    //  1. Bind Views
    // ─────────────────────────────────────────────

    private void initViews() {
        tilUsername      = findViewById(R.id.tilUsername);
        tilPassword      = findViewById(R.id.tilPassword);
        etUsername       = findViewById(R.id.etUsername);
        etPassword       = findViewById(R.id.etPassword);
        cbRememberMe     = findViewById(R.id.cbRememberMe);
        btnSignIn        = findViewById(R.id.btnSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRequestAccess  = findViewById(R.id.tvRequestAccess);
    }

    // ─────────────────────────────────────────────
    //  2. Load Remembered Username
    // ─────────────────────────────────────────────

    private void loadRememberedUser() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean remembered = prefs.getBoolean(KEY_REMEMBER, false);
        if (remembered) {
            String savedUsername = prefs.getString(KEY_USERNAME, "");
            etUsername.setText(savedUsername);
            cbRememberMe.setChecked(true);
        }
    }

    // ─────────────────────────────────────────────
    //  3. Click Listeners
    // ─────────────────────────────────────────────

    private void setClickListeners() {
        // Sign In button
        btnSignIn.setOnClickListener(v -> attemptLogin());

        // Forgot Password
        tvForgotPassword.setOnClickListener(v -> {
            String email = etUsername.getText() != null
                    ? etUsername.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(email)) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Failed to send reset email. Check your email address.", Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
            }
        });

        // Request Access → Sign Up
        tvRequestAccess.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    // ─────────────────────────────────────────────
    //  4. Login Validation with Firebase Auth
    // ─────────────────────────────────────────────

    private void attemptLogin() {
        // Clear previous errors
        tilUsername.setError(null);
        tilPassword.setError(null);

        String email = etUsername.getText() != null
                ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null
                ? etPassword.getText().toString().trim() : "";

        boolean isValid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilUsername.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilUsername.setError("Enter a valid email address");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (!isValid) return;

        // ── Save Remember Me ──
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (cbRememberMe.isChecked()) {
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putString(KEY_USERNAME, email);
        } else {
            editor.putBoolean(KEY_REMEMBER, false);
            editor.remove(KEY_USERNAME);
        }
        editor.apply();

        // ── Firebase Auth Sign In ──
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing in…");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            fetchUserRoleAndNavigate(user.getUid());
                        }
                    } else {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                        tilUsername.setError(" ");
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Invalid email or password";
                        tilPassword.setError(errorMsg);
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  5. Fetch User Role from Firebase DB
    // ─────────────────────────────────────────────

    private void fetchUserRoleAndNavigate(String uid) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = "Unknown";
                    if (documentSnapshot.exists() && documentSnapshot.getString("role") != null) {
                        role = documentSnapshot.getString("role");
                    }
                    onLoginSuccess(role);
                })
                .addOnFailureListener(e -> onLoginSuccess("Unknown"));
    }

    private void onLoginSuccess(String role) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
