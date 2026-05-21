package com.example.aling_jar.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String DEFAULT_ROLE = "User";

    // ── Views ──
    private TextInputLayout tilUsername, tilPassword;
    private TextInputEditText etUsername, etPassword;
    private CheckBox cbRememberMe;
    private Button btnSignIn;
    private LinearLayout btnGoogle, btnFacebook;
    private TextView tvForgotPassword, tvRequestAccess;

    // ── SharedPreferences ──
    private static final String PREFS_NAME  = "AlingMasterPrefs";
    private static final String KEY_USERNAME = "saved_username";
    private static final String KEY_REMEMBER = "remember_me";

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
        initGoogleSignIn();
        initFacebookLogin();
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
        btnGoogle        = findViewById(R.id.btnGoogle);
        btnFacebook      = findViewById(R.id.btnFacebook);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRequestAccess  = findViewById(R.id.tvRequestAccess);
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
                        Toast.makeText(LoginActivity.this, "Facebook login cancelled", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        dismissLoadingDialog();
                        Log.w(TAG, "Facebook login error", error);
                        Toast.makeText(LoginActivity.this, "Facebook login failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  4. Load Remembered Username
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
    //  5. Click Listeners
    // ─────────────────────────────────────────────

    private void setClickListeners() {
        // Sign In
        btnSignIn.setOnClickListener(v -> attemptLogin());

        // OAuth buttons
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        btnFacebook.setOnClickListener(v -> signInWithFacebook());

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
    //  6. Email/Password Login
    // ─────────────────────────────────────────────

    private void attemptLogin() {
        tilUsername.setError(null);
        tilPassword.setError(null);

        String email = etUsername.getText() != null
                ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null
                ? etPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            tilUsername.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilUsername.setError("Enter a valid email address");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (!isValid) return;

        // Save Remember Me
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

        // Firebase Auth
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
    //  7. Google OAuth Flow
    // ─────────────────────────────────────────────

    private void signInWithGoogle() {
        showLoadingDialog("Signing in with Google…");
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
                            ensureUserExistsAndNavigate(user);
                        }
                    } else {
                        dismissLoadingDialog();
                        Log.w(TAG, "Firebase auth with Google failed", task.getException());
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  8. Facebook OAuth Flow
    // ─────────────────────────────────────────────

    private void signInWithFacebook() {
        showLoadingDialog("Signing in with Facebook…");
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
                            ensureUserExistsAndNavigate(user);
                        }
                    } else {
                        dismissLoadingDialog();
                        Log.w(TAG, "Firebase auth with Facebook failed", task.getException());
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────
    //  9. User Profile & Navigation
    // ─────────────────────────────────────────────

    /**
     * For OAuth logins, check if the user already has a Firestore profile.
     * If not, create one with default role, then navigate to main.
     */
    private void ensureUserExistsAndNavigate(FirebaseUser user) {
        String uid = user.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Existing user — read role and navigate
                        String role = document.getString("role");
                        dismissLoadingDialog();
                        onLoginSuccess(role != null ? role : "Unknown");
                    } else {
                        // First-time OAuth user — create profile
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("fullName", user.getDisplayName() != null ? user.getDisplayName() : "");
                        userData.put("email", user.getEmail() != null ? user.getEmail() : "");
                        userData.put("role", DEFAULT_ROLE);
                        userData.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(uid)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    dismissLoadingDialog();
                                    onLoginSuccess(DEFAULT_ROLE);
                                })
                                .addOnFailureListener(e -> {
                                    dismissLoadingDialog();
                                    Log.w(TAG, "Failed to create user profile", e);
                                    // Still navigate — auth succeeded
                                    onLoginSuccess(DEFAULT_ROLE);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    dismissLoadingDialog();
                    onLoginSuccess("Unknown");
                });
    }

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
        Intent intent;
        if ("Admin".equalsIgnoreCase(role)) {
            intent = new Intent(LoginActivity.this, com.example.aling_jar.admin.AdminActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, com.example.aling_jar.user.UserActivity.class);
        }
        intent.putExtra("role", role);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ─────────────────────────────────────────────
    //  10. Loading Dialog
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Forward result to Facebook SDK
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
