package com.example.aling_jar.auth;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.aling_jar.R;


public class SplashActivity extends AppCompatActivity {


    private ImageView ivLogo;
    private TextView tvAppName;
    private TextView tvTagline;
    private TextView tvProgressPercent;
    private ProgressBar progressBar;
    private LinearLayout progressContainer;


    private static final long FADE_IN_DURATION  = 600L;
    private static final long PROGRESS_DURATION = 3800L;
    private static final long NAVIGATE_DELAY_MS = 4200L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        initViews();
        styleAppName();
        startEntranceAnimations();
        startProgressAnimation();
        scheduleNavigation();


    }

    private void initViews() {
        ivLogo            = findViewById(R.id.ivLogo);
        tvAppName         = findViewById(R.id.tvAppName);
        tvTagline         = findViewById(R.id.tvTagline);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBar       = findViewById(R.id.progressBar);
        progressContainer = findViewById(R.id.progressContainer);
    }




    private void styleAppName() {
        String fullText = getString(R.string.app_name_display);
        SpannableString spannable = new SpannableString(fullText);
        int greenColor = getColor(R.color.green_primary);

        int alingStart = fullText.indexOf("Aling");
        if (alingStart >= 0) {
            spannable.setSpan(
                    new ForegroundColorSpan(greenColor),
                    alingStart,
                    fullText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        tvAppName.setText(spannable);
    }


    private void startEntranceAnimations() {
        View[] views = { ivLogo, tvAppName, tvTagline, progressContainer };

        for (int i = 0; i < views.length; i++) {
            View view  = views[i];
            long delay = i * 150L;

            view.setTranslationY(30f);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
            fadeIn.setDuration(FADE_IN_DURATION);
            fadeIn.setStartDelay(delay);
            fadeIn.setInterpolator(new DecelerateInterpolator());

            ObjectAnimator slideUp = ObjectAnimator.ofFloat(view, "translationY", 30f, 0f);
            slideUp.setDuration(FADE_IN_DURATION);
            slideUp.setStartDelay(delay);
            slideUp.setInterpolator(new DecelerateInterpolator());

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(fadeIn, slideUp);
            animSet.start();
        }
    }


    private void startProgressAnimation() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(PROGRESS_DURATION);
        animator.setStartDelay(400L);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(anim -> {
            int value = (int) anim.getAnimatedValue();
            progressBar.setProgress(value);
            tvProgressPercent.setText(value + "%");
        });

        animator.start();
    }



    private void scheduleNavigation() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, NAVIGATE_DELAY_MS);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ivLogo != null) {
            Glide.with(getApplicationContext()).clear(ivLogo);
        }
    }
}
