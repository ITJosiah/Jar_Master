package com.example.aling_jar.actvities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aling_jar.R;

public class SignupSuccessActivity extends AppCompatActivity {

    private Button btnGetStarted;
    private TextView tvGoToDashboard;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_success);

        // Transparent status bar
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        role = getIntent().getStringExtra("role");

        initViews();
        setClickListeners();
        handleBackPress();
    }

    private void initViews() {
        btnGetStarted   = findViewById(R.id.btnGetStarted);
        tvGoToDashboard = findViewById(R.id.tvGoToDashboard);
    }

    private void setClickListeners() {
        // "Get Started" → go to Login screen
        btnGetStarted.setOnClickListener(v -> {
            Intent intent = new Intent(SignupSuccessActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // "Go to Dashboard" → go to MainActivity with the user's role
        tvGoToDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(SignupSuccessActivity.this, MainActivity.class);
            intent.putExtra("role", role);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void handleBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent going back to verification screen
                // Instead, go to login
                Intent intent = new Intent(SignupSuccessActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
    }
}
