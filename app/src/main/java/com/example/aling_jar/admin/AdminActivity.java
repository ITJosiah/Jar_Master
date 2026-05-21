package com.example.aling_jar.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.example.aling_jar.R;
import com.example.aling_jar.admin.navigation.AdminBottomNavBar;
import com.example.aling_jar.admin.navigation.NavItem;

/**
 * Host activity for the admin dashboard.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Fragment switching based on {@link NavItem} selection</li>
 *   <li>Wiring the custom bottom nav bar listeners</li>
 *   <li>Managing full-screen flow visibility (hide nav during LogNewBatch)</li>
 * </ul>
 */
public class AdminActivity extends AppCompatActivity {

    private AdminBottomNavBar bottomNavBar;

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

        configureSystemBars();
        initViews();
        applyWindowInsets();
        setupFragments();
        setupBottomNav();
    }

    // ─── Setup ──────────────────────────────────────────────────────

    private void configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );
    }

    private void initViews() {
        bottomNavBar = findViewById(R.id.adminBottomNavBar);
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.adminRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, 0);

            // Offset bottom nav so it doesn't sit beneath the gesture bar
            bottomNavBar.setPadding(
                    bottomNavBar.getPaddingLeft(),
                    bottomNavBar.getPaddingTop(),
                    bottomNavBar.getPaddingRight(),
                    sysBars.bottom
            );
            return insets;
        });
    }

    private void setupFragments() {
        activeFragment = dashboardFragment;
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.fragmentContainer, ordersFragment, "orders").hide(ordersFragment)
                .add(R.id.fragmentContainer, batchesFragment, "batches").hide(batchesFragment)
                .add(R.id.fragmentContainer, dashboardFragment, "dashboard")
                .commit();
    }

    private void setupBottomNav() {
        // Nav item selection → fragment switch
        bottomNavBar.setOnNavItemSelectedListener(this::onNavItemSelected);

        // FAB → open log-new-batch flow
        bottomNavBar.setOnFabClickListener(this::openLogNewBatch);

        // Sync visibility when back-stack changes (hide nav in full-screen flows)
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncBottomNavVisibility);
    }

    // ─── Navigation ─────────────────────────────────────────────────

    private void onNavItemSelected(NavItem item) {
        switch (item) {
            case OVERVIEW:  switchFragment(dashboardFragment); break;
            case INVENTORY: switchFragment(batchesFragment);   break;
            case ORDERS:    switchFragment(ordersFragment);     break;
            case SETTINGS:  switchFragment(settingsFragment);   break;
        }
    }

    private void switchFragment(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().popBackStack();
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
        syncBottomNavVisibility();
    }

    // ─── Public navigation helpers (called from fragments) ──────────

    /** Navigate to the Batches/Inventory tab programmatically. */
    public void navigateToBatches() {
        bottomNavBar.setSelectedItem(NavItem.INVENTORY);
    }

    /** Navigate to the Orders tab programmatically. */
    public void navigateToOrders() {
        bottomNavBar.setSelectedItem(NavItem.ORDERS);
    }

    /** Open the full-screen Log New Batch form. */
    public void openLogNewBatch() {
        bottomNavBar.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, logNewBatchFragment, "log_new_batch")
                .addToBackStack("log_new_batch")
                .commit();
    }

    /** Open the full-screen Edit Batch form for an existing batch. */
    public void openEditBatch(@NonNull Batch batch) {
        bottomNavBar.setVisibility(View.GONE);
        AdminEditBatchFragment editFragment = AdminEditBatchFragment.newInstance(batch);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, editFragment, "edit_batch")
                .addToBackStack("edit_batch")
                .commit();
    }

    /**
     * Update the badge count on the Orders tab.
     * Called from {@link AdminDashboardFragment} when pending order count changes.
     */
    public void updateOrdersBadge(int count) {
        bottomNavBar.setBadgeCount(NavItem.ORDERS, count);
    }

    // ─── Internal ───────────────────────────────────────────────────

    private void syncBottomNavVisibility() {
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        boolean show = !(top instanceof AdminLogNewBatchFragment)
                && !(top instanceof AdminEditBatchFragment);
        bottomNavBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
