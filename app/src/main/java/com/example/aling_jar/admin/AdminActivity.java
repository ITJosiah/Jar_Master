package com.example.aling_jar.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.aling_jar.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    // Fragments
    private final AdminDashboardFragment dashboardFragment = new AdminDashboardFragment();
    private final AdminBatchesFragment batchesFragment = new AdminBatchesFragment();
    private final AdminOrdersFragment ordersFragment = new AdminOrdersFragment();
    private final AdminSettingsFragment settingsFragment = new AdminSettingsFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Light status bar with white/light background
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        bottomNav = findViewById(R.id.bottomNavigationView);

        // Load default fragment
        activeFragment = dashboardFragment;
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.fragmentContainer, ordersFragment, "orders").hide(ordersFragment)
                .add(R.id.fragmentContainer, batchesFragment, "batches").hide(batchesFragment)
                .add(R.id.fragmentContainer, dashboardFragment, "dashboard")
                .commit();

        // bottom nav listener
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_overview) {
                switchFragment(dashboardFragment);
            } else if (id == R.id.nav_inventory) {
                switchFragment(batchesFragment);
            } else if (id == R.id.nav_orders) {
                switchFragment(ordersFragment);
            } else if (id == R.id.nav_settings) {
                switchFragment(settingsFragment);
                return false;
            }
            return true;
        });
    }

    private void switchFragment(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }

    /**
     * Called from DashboardFragment "VIEW ALL" to navigate to Batches tab
     */
    public void navigateToBatches() {
        bottomNav.setSelectedItemId(R.id.nav_inventory);
    }

    /**
     * Called externally to navigate to Orders tab
     */
    public void navigateToOrders() {
        bottomNav.setSelectedItemId(R.id.nav_orders);
    }
}
