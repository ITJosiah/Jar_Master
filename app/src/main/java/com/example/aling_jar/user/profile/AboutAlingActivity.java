package com.example.aling_jar.user.profile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aling_jar.R;
import com.google.android.material.appbar.MaterialToolbar;

public class AboutAlingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_aling);

        MaterialToolbar toolbar = findViewById(R.id.toolbarAboutAling);
        toolbar.setNavigationOnClickListener(v -> finish());
    }
}
