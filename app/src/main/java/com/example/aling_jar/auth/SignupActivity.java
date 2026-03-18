package com.example.aling_jar.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aling_jar.R;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private static final String DEFAULT_ROLE = "User";

    // ── Input fields ──
    private TextInputLayout tilFullName, tilEmail, tilRegPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etRegPassword, etConfirmPassword;

    // ── Buttons & footer ──
    private Button btnCreateAccount;
    private LinearLayout btnGoogle, btnFacebook;
    private TextView tvLogIn, tvTerms;

    // ── Firebase ──
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // ── Google Sign-In ──
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    // ── Facebook Login ──
    private CallbackManager facebookCallbackManager;

    // ── Loading dialog ──
    private AlertDialog loadingDialog;

    // ─────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Transparent status bar with dark icons
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        initGoogleSignIn();
        initFacebookLogin();
        setupTermsText();
        setClickListeners();


    }

    // ─────────────────────────────────────────────
    //  1. View Binding
    // ─────────────────────────────────────────────

    private void initViews() {
        tilFullName        = findViewById(R.id.tilFullName);
        tilEmail           = findViewById(R.id.tilEmail);
        tilRegPassword     = findViewById(R.id.tilRegPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName         = findViewById(R.id.etFullName);
        etEmail            = findViewById(R.id.etEmail);
        etRegPassword      = findViewById(R.id.etRegPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        btnGoogle        = findViewById(R.id.btnGoogle);
        btnFacebook      = findViewById(R.id.btnFacebook);
        tvLogIn          = findViewById(R.id.tvLogIn);
        tvTerms          = findViewById(R.id.tvTerms);
    }

    // ─────────────────────────────────────────────
    //  2. Google Sign-In Setup
    // ─────────────────────────────────────────────

    private void initGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        firebaseAuthWithGoogle(account);
                    } catch (ApiException e) {
                        dismissLoadingDialog();
                        Log.w(TAG, "Google sign-in failed", e);
                        Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // ─────────────────────────────────────────────
    //  3. Facebook Login Setup
    // ─────────────────────────────────────────────

    private void initFacebookLogin() {
        facebookCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(facebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        dismissLoadingDialog();
                        Toast.makeText(SignupActivity.this, "Facebook login cancelled", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        dismissLoadingDialog();
                        Log.w(TAG, "Facebook login error", error);
                        Toast.makeText(SignupActivity.this, "Facebook login failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  4. Click Listeners
    // ─────────────────────────────────────────────

    private void setClickListeners() {
        btnCreateAccount.setOnClickListener(v -> attemptRegister());

        btnGoogle.setOnClickListener(v -> signUpWithGoogle());

        btnFacebook.setOnClickListener(v -> signUpWithFacebook());

        tvLogIn.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    // ─────────────────────────────────────────────
    //  5. Email/Password Registration
    // ─────────────────────────────────────────────

    private void attemptRegister() {
        // Clear previous errors
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilRegPassword.setError(null);
        tilConfirmPassword.setError(null);

        String fullName    = getText(etFullName);
        String email       = getText(etEmail);
        String password    = getText(etRegPassword);
        String confirmPass = getText(etConfirmPassword);

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

        if (!isValid) return;

        // Navigate to email verification
        Intent intent = new Intent(SignupActivity.this, EmailVerificationActivity.class);
        intent.putExtra("email", email);
        intent.putExtra("fullName", fullName);
        intent.putExtra("password", password);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    // ─────────────────────────────────────────────
    //  6. Google OAuth Flow
    // ─────────────────────────────────────────────

    private void signUpWithGoogle() {
        showLoadingDialog("Signing up with Google…");
        // Sign out first to always show account picker
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveOAuthUserAndNavigate(user);
                        }
                    } else {
                        dismissLoadingDialog();
                        Log.w(TAG, "Firebase auth with Google failed", task.getException());
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  7. Facebook OAuth Flow
    // ─────────────────────────────────────────────

    private void signUpWithFacebook() {
        showLoadingDialog("Signing up with Facebook…");
        LoginManager.getInstance().logInWithReadPermissions(
                this, Arrays.asList("email", "public_profile"));
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveOAuthUserAndNavigate(user);
                        }
                    } else {
                        dismissLoadingDialog();
                        Log.w(TAG, "Firebase auth with Facebook failed", task.getException());
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  8. Save OAuth User & Navigate
    // ─────────────────────────────────────────────

    private void saveOAuthUserAndNavigate(FirebaseUser user) {
        String uid = user.getUid();

        // Check if user already exists in Firestore
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Returning user — go straight to main
                        dismissLoadingDialog();
                        navigateToMain();
                    } else {
                        // New user — create Firestore profile
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
                        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
                        userData.put("role", DEFAULT_ROLE);
                        userData.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    dismissLoadingDialog();
                                    navigateToSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    dismissLoadingDialog();
                                    Log.w(TAG, "Failed to save user data", e);
                                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    dismissLoadingDialog();
                    Log.w(TAG, "Failed to check existing user", e);
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                });
    }

    // ─────────────────────────────────────────────
    //  9. Navigation Helpers
    // ─────────────────────────────────────────────

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToSuccess() {
        Intent intent = new Intent(this, SignupSuccessActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ─────────────────────────────────────────────
    //  10. Terms & Services Spannable
    // ─────────────────────────────────────────────

    private void setupTermsText() {
        String full = getString(R.string.terms_text);
        SpannableString spannable = new SpannableString(full);
        int green = getColor(R.color.green_primary);

        applyClickableSpan(spannable, full, "Terms of Service", green);
        applyClickableSpan(spannable, full, "Privacy Policy", green);

        tvTerms.setText(spannable);
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
        tvTerms.setHighlightColor(Color.TRANSPARENT);
    }

    private void applyClickableSpan(SpannableString spannable, String full,
                                     String target, int color) {
        int start = full.indexOf(target);
        if (start < 0) return;

        spannable.setSpan(new ForegroundColorSpan(color),
                start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Toast.makeText(SignupActivity.this, target, Toast.LENGTH_SHORT).show();
            }
        }, start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // ─────────────────────────────────────────────
    //  11. Loading Dialog
    // ─────────────────────────────────────────────

    private void showLoadingDialog(String message) {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            TextView tvMessage = loadingDialog.findViewById(R.id.tvLoadingMessage);
            if (tvMessage != null) tvMessage.setText(message);
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

    // ─────────────────────────────────────────────
    //  12. Utility
    // ─────────────────────────────────────────────

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Forward result to Facebook SDK
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
}