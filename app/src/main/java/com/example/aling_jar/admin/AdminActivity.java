package com.example.aling_jar.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
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
    private final AdminLogNewBatchFragment logNewBatchFragment = new AdminLogNewBatchFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Light status bar with white/light background
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );

        bottomNav = findViewById(R.id.bottomNavigationView);
        View root = findViewById(R.id.adminRoot);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Push all fragment content below status bar.
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, 0);

            // Ensure bottom nav isn't covered by gesture/nav bar.
            bottomNav.setPadding(
                    bottomNav.getPaddingLeft(),
                    bottomNav.getPaddingTop(),
                    bottomNav.getPaddingRight(),
                    sysBars.bottom
            );

            return insets;
        });

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
            }
            return true;
        });

        // Ensure bottom nav hides on full-screen flows
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncBottomNavVisibility);
    }

    private void switchFragment(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().popBackStack(); // leave any full-screen flow
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
        syncBottomNavVisibility();
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

    public void openLogNewBatch() {
        // Full-screen fragment on top of the current tab
        bottomNav.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, logNewBatchFragment, "log_new_batch")
                .addToBackStack("log_new_batch")
                .commit();
    }

    private void syncBottomNavVisibility() {
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        boolean show = !(top instanceof AdminLogNewBatchFragment);
        bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
