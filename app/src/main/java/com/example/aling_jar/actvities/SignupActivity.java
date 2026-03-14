package com.example.aling_jar.actvities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.example.aling_jar.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupActivity extends AppCompatActivity {

    // ── input fields ──
    private TextInputLayout tilFullName, tilEmail, tilRegPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etRegPassword, etConfirmPassword;

    // ── role cards (from <include>) ──
    private MaterialCardView cardAdmin, cardProduction, cardSales;
    private RadioButton rbAdmin, rbProduction, rbSales;
    private TextView tvAdminTitle, tvAdminDesc;
    private TextView tvProductionTitle, tvProductionDesc;
    private TextView tvSalesTitle, tvSalesDesc;

    // button & footer
    private Button btnCreateAccount;
    private TextView tvLogIn, tvTerms;

    // selected role
    private String selectedRole = "";

    private static final String ROLE_ADMIN      = "Admin";
    private static final String ROLE_PRODUCTION = "Production Staff";
    private static final String ROLE_SALES      = "Sales Assistant";

    // Firebase Firestore
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRoleCardLabels();
        setupRoleCards();
        setupTermsText();
        setClickListeners();
        checkAdminExists();
    }

    // ── View Binding ──
    private void initViews() {
        // Input fields
        tilFullName        = findViewById(R.id.tilFullName);
        tilEmail           = findViewById(R.id.tilEmail);
        tilRegPassword     = findViewById(R.id.tilRegPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName         = findViewById(R.id.etFullName);
        etEmail            = findViewById(R.id.etEmail);
        etRegPassword      = findViewById(R.id.etRegPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);

        // Role cards
        View adminCard      = findViewById(R.id.cardAdmin);
        View productionCard = findViewById(R.id.cardProduction);
        View salesCard      = findViewById(R.id.cardSales);

        cardAdmin      = (MaterialCardView) adminCard;
        cardProduction = (MaterialCardView) productionCard;
        cardSales      = (MaterialCardView) salesCard;

        rbAdmin      = adminCard.findViewById(R.id.rbRole);
        rbProduction = productionCard.findViewById(R.id.rbRole);
        rbSales      = salesCard.findViewById(R.id.rbRole);

        tvAdminTitle      = adminCard.findViewById(R.id.tvRoleTitle);
        tvAdminDesc       = adminCard.findViewById(R.id.tvRoleDesc);
        tvProductionTitle = productionCard.findViewById(R.id.tvRoleTitle);
        tvProductionDesc  = productionCard.findViewById(R.id.tvRoleDesc);
        tvSalesTitle      = salesCard.findViewById(R.id.tvRoleTitle);
        tvSalesDesc       = salesCard.findViewById(R.id.tvRoleDesc);

        // Button & footer
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvLogIn          = findViewById(R.id.tvLogIn);
        tvTerms          = findViewById(R.id.tvTerms);
    }

    // ── Set Role Labels ──
    private void setupRoleCardLabels() {
        tvAdminTitle.setText(getString(R.string.role_admin));
        tvAdminDesc.setText(getString(R.string.role_admin_desc));

        tvProductionTitle.setText(getString(R.string.role_production));
        tvProductionDesc.setText(getString(R.string.role_production_desc));

        tvSalesTitle.setText(getString(R.string.role_sales));
        tvSalesDesc.setText(getString(R.string.role_sales_desc));
    }

    // ── Role Card Click Logic ──
    private void setupRoleCards() {
        cardAdmin.setOnClickListener(v -> selectRole(ROLE_ADMIN));
        cardProduction.setOnClickListener(v -> selectRole(ROLE_PRODUCTION));
        cardSales.setOnClickListener(v -> selectRole(ROLE_SALES));

        rbAdmin.setOnClickListener(v -> selectRole(ROLE_ADMIN));
        rbProduction.setOnClickListener(v -> selectRole(ROLE_PRODUCTION));
        rbSales.setOnClickListener(v -> selectRole(ROLE_SALES));
    }

    private void selectRole(String role) {
        selectedRole = role;

        // Reset all cards to default
        setCardSelected(cardAdmin, rbAdmin, false);
        setCardSelected(cardProduction, rbProduction, false);
        setCardSelected(cardSales, rbSales, false);

        // Highlight selected card
        switch (role) {
            case ROLE_ADMIN:
                setCardSelected(cardAdmin, rbAdmin, true);
                break;
            case ROLE_PRODUCTION:
                setCardSelected(cardProduction, rbProduction, true);
                break;
            case ROLE_SALES:
                setCardSelected(cardSales, rbSales, true);
                break;
        }
    }

    private void setCardSelected(MaterialCardView card, RadioButton rb, boolean selected) {
        if (selected) {
            card.setStrokeColor(getColor(R.color.green_primary));
            card.setCardBackgroundColor(getColor(R.color.green_light_bg));
            rb.setChecked(true);
        } else {
            card.setStrokeColor(getColor(R.color.progress_track));
            card.setCardBackgroundColor(getColor(R.color.splash_background));
            rb.setChecked(false);
        }
    }

    // ── Check if Admin already exists in Firestore ──
    private void checkAdminExists() {
        db.collection("users")
                .whereEqualTo("role", ROLE_ADMIN)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Admin exists — hide the admin card
                        cardAdmin.setVisibility(View.GONE);

                        // Update constraint: cardProduction should now anchor below tvUserRoleLabel
                        ConstraintLayout parent = (ConstraintLayout) cardAdmin.getParent();
                        ConstraintSet constraintSet = new ConstraintSet();
                        constraintSet.clone(parent);
                        constraintSet.connect(
                                R.id.cardProduction,
                                ConstraintSet.TOP,
                                R.id.tvUserRoleLabel,
                                ConstraintSet.BOTTOM,
                                (int) getResources().getDimension(R.dimen.margin_xsmall)
                        );
                        constraintSet.applyTo(parent);

                        // If admin was previously selected, clear selection
                        if (ROLE_ADMIN.equals(selectedRole)) {
                            selectedRole = "";
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Silently fail — admin card stays visible
                    Log.w("SignupActivity", "Admin check failed", e);
                });
    }

    // ── Terms & Services ──
    private void setupTermsText() {
        String full = getString(R.string.terms_text);
        SpannableString spannable = new SpannableString(full);
        int green = getColor(R.color.green_primary);

        // Green "Terms of Service"
        String terms = "Terms of Service";
        int tStart = full.indexOf(terms);
        if (tStart >= 0) {
            spannable.setSpan(new ForegroundColorSpan(green),
                    tStart, tStart + terms.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ClickableSpan() {
                @Override public void onClick(View widget) {
                    Toast.makeText(SignupActivity.this,
                            "Terms of Service", Toast.LENGTH_SHORT).show();
                }
            }, tStart, tStart + terms.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Green "Privacy Policy"
        String privacy = "Privacy Policy";
        int pStart = full.indexOf(privacy);
        if (pStart >= 0) {
            spannable.setSpan(new ForegroundColorSpan(green),
                    pStart, pStart + privacy.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ClickableSpan() {
                @Override public void onClick(View widget) {
                    Toast.makeText(SignupActivity.this,
                            "Privacy Policy", Toast.LENGTH_SHORT).show();
                }
            }, pStart, pStart + privacy.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvTerms.setText(spannable);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
        tvTerms.setHighlightColor(Color.TRANSPARENT);
    }

    // ── Click Listeners ──
    private void setClickListeners() {
        btnCreateAccount.setOnClickListener(v -> attemptRegister());

        tvLogIn.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    // ── Input Validation ──
    private void attemptRegister() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilRegPassword.setError(null);
        tilConfirmPassword.setError(null);

        String fullName    = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email       = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password    = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";
        String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (TextUtils.isEmpty(fullName)) {
            tilFullName.setError("Full name is required");
            isValid = false;
        } else if (fullName.length() < 2) {
            tilFullName.setError("Enter a valid full name");
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilRegPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilRegPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPass)) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPass)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (TextUtils.isEmpty(selectedRole)) {
            Toast.makeText(this, "Please select a user role", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid) return;

        // Navigate to email verification instead of creating account directly
        Intent intent = new Intent(SignupActivity.this, EmailVerificationActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("fullName", fullName);
        intent.putExtra("password", password);
        intent.putExtra("role", selectedRole);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}