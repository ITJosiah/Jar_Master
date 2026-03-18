package com.example.aling_jar.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aling_jar.R;
import com.example.aling_jar.utils.GmailSender;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EmailVerificationActivity extends AppCompatActivity {

    private EditText etCode1, etCode2, etCode3, etCode4;
    private Button btnVerify;
    private TextView tvResendCode, tvVerifyDesc;
    private ImageView ivBack;

    private static final String DEFAULT_ROLE = "User";

    private String email, fullName, password;
    private String generatedCode;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        // Transparent status bar
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get data from intent
        email    = getIntent().getStringExtra("email");
        fullName = getIntent().getStringExtra("fullName");
        password = getIntent().getStringExtra("password");

        initViews();
        setupDescription();
        setupCodeInputs();
        generateAndSendCode();
        setClickListeners();
        handleBackPress();
    }

    private void initViews() {
        ivBack      = findViewById(R.id.ivBack);
        etCode1     = findViewById(R.id.etCode1);
        etCode2     = findViewById(R.id.etCode2);
        etCode3     = findViewById(R.id.etCode3);
        etCode4     = findViewById(R.id.etCode4);
        btnVerify   = findViewById(R.id.btnVerify);
        tvResendCode = findViewById(R.id.tvResendCode);
        tvVerifyDesc = findViewById(R.id.tvVerifyDesc);
    }

    private void setupDescription() {
        String desc = getString(R.string.verification_code_sent, email);
        tvVerifyDesc.setText(desc);
    }

    // ── Auto-advance between digit inputs ──
    private void setupCodeInputs() {
        EditText[] inputs = {etCode1, etCode2, etCode3, etCode4};

        for (int i = 0; i < inputs.length; i++) {
            final int index = i;
            final EditText current = inputs[i];

            current.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < inputs.length - 1) {
                        inputs[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > 0) {
                        current.setBackgroundResource(R.drawable.bg_code_input_focused);
                    } else {
                        current.setBackgroundResource(R.drawable.bg_code_input);
                    }
                }
            });

            current.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL
                        && event.getAction() == android.view.KeyEvent.ACTION_DOWN
                        && current.getText().toString().isEmpty()
                        && index > 0) {
                    inputs[index - 1].requestFocus();
                    inputs[index - 1].setText("");
                    return true;
                }
                return false;
            });
        }

        etCode1.requestFocus();
    }

    // ── Generate code and send via Gmail ──
    private void generateAndSendCode() {
        Random random = new Random();
        int code = 1000 + random.nextInt(9000);
        generatedCode = String.valueOf(code);

        // Store code in Firestore
        String sanitizedEmail = email.replace(".", ",");
        db.collection("verification_codes").document(sanitizedEmail)
                .set(java.util.Collections.singletonMap("code", generatedCode));

        // Send verification code via Gmail SMTP
        showLoadingDialog("Sending verification code…");

        GmailSender.sendVerificationEmail(email, generatedCode,
                new GmailSender.EmailCallback() {
                    @Override
                    public void onSuccess() {
                        dismissLoadingDialog();
                        Toast.makeText(EmailVerificationActivity.this,
                                "Verification code sent to your email!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        dismissLoadingDialog();
                        Toast.makeText(EmailVerificationActivity.this,
                                "Failed to send email: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                }, this);
    }

    private void setClickListeners() {
        // Back button
        ivBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Verify button
        btnVerify.setOnClickListener(v -> verifyCode());

        // Resend code
        tvResendCode.setOnClickListener(v -> {
            etCode1.setText("");
            etCode2.setText("");
            etCode3.setText("");
            etCode4.setText("");
            etCode1.requestFocus();

            generateAndSendCode();
        });
    }

    private void verifyCode() {
        String enteredCode = etCode1.getText().toString()
                + etCode2.getText().toString()
                + etCode3.getText().toString()
                + etCode4.getText().toString();

        if (enteredCode.length() < 4) {
            Toast.makeText(this, "Please enter the complete 4-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!enteredCode.equals(generatedCode)) {
            Toast.makeText(this, "Invalid verification code. Please try again.", Toast.LENGTH_SHORT).show();
            etCode1.setText("");
            etCode2.setText("");
            etCode3.setText("");
            etCode4.setText("");
            etCode1.requestFocus();
            return;
        }

        // Code is correct — show loading and create account
        btnVerify.setEnabled(false);
        showLoadingDialog("Creating your account…");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid());
                        }
                    } else {
                        dismissLoadingDialog();
                        btnVerify.setEnabled(true);
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Account creation failed";
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ── Save user data to Firestore ──
    private void saveUserToFirestore(String uid) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("role", DEFAULT_ROLE);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    // Clean up verification code from Firestore
                    String sanitizedEmail = email.replace(".", ",");
                    db.collection("verification_codes").document(sanitizedEmail).delete();

                    // Sign out so user can log in fresh
                    mAuth.signOut();

                    dismissLoadingDialog();

                    // Navigate to success screen
                    Intent intent = new Intent(EmailVerificationActivity.this, SignupSuccessActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .addOnFailureListener(e -> {
                    dismissLoadingDialog();
                    btnVerify.setEnabled(true);
                    String errorMsg = e.getMessage() != null
                            ? e.getMessage()
                            : "Failed to save user data";
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    // ── Loading Dialog ──
    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            // Update the message if already showing
            TextView tvMessage = loadingDialog.findViewById(R.id.tvLoadingMessage);
            if (tvMessage != null) {
                tvMessage.setText(message);
            }
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvLoadingMessage);
        tvMessage.setText(message);

        loadingDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_rounded);
        }

        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void handleBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }
}
