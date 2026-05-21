package com.example.aling_jar.user;

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
import com.example.aling_jar.user.navigation.UserBottomNavBar;
import com.example.aling_jar.user.navigation.UserNavItem;
import com.example.aling_jar.user.orders.UserOrdersFragment;
import com.example.aling_jar.user.profile.UserProfileFragment;
import com.example.aling_jar.user.saved.UserSavedFragment;
import com.example.aling_jar.user.shop.UserShopFragment;
import com.example.aling_jar.user.cart.UserCartFragment;

/**
 * Host activity for the user-facing screens.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Fragment switching based on {@link UserNavItem} selection</li>
 *   <li>Wiring the custom bottom nav bar listeners</li>
 *   <li>Managing edge-to-edge display and window insets</li>
 * </ul>
 * <p>
 * Mirrors {@code AdminActivity} structure for consistency across the codebase.
 */
public class UserActivity extends AppCompatActivity {

    private UserBottomNavBar bottomNavBar;

    // Fragments (lazy initialization avoids unnecessary creation)
    private final UserShopFragment shopFragment = new UserShopFragment();
    private final UserOrdersFragment ordersFragment = new UserOrdersFragment();
    private final UserCartFragment cartFragment = new UserCartFragment();
    private final UserSavedFragment savedFragment = new UserSavedFragment();
    private final UserProfileFragment profileFragment = new UserProfileFragment();
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

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
        bottomNavBar = findViewById(R.id.userBottomNavBar);
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.userRoot);
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
        activeFragment = shopFragment;
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                .add(R.id.fragmentContainer, savedFragment, "saved").hide(savedFragment)
                .add(R.id.fragmentContainer, cartFragment, "cart").hide(cartFragment)
                .add(R.id.fragmentContainer, ordersFragment, "orders").hide(ordersFragment)
                .add(R.id.fragmentContainer, shopFragment, "shop")
                .commit();
    }

    private void setupBottomNav() {
        bottomNavBar.setOnNavItemSelectedListener(this::onNavItemSelected);
    }

    // ─── Navigation ─────────────────────────────────────────────────

    private void onNavItemSelected(UserNavItem item) {
        switch (item) {
            case SHOP:    switchFragment(shopFragment);    break;
            case ORDERS:  switchFragment(ordersFragment);  break;
            case CART:    switchFragment(cartFragment);    break;
            case SAVED:   switchFragment(savedFragment);   break;
            case PROFILE: switchFragment(profileFragment); break;
        }
    }

    private void switchFragment(Fragment target) {
        if (target == activeFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit();
        activeFragment = target;
    }

    // ─── Public helpers (for future use by child fragments) ─────────

    /** Programmatically switch to the Shop tab. */
    public void navigateToShop() {
        bottomNavBar.setSelectedItem(UserNavItem.SHOP);
    }

    /** Programmatically switch to the Orders tab. */
    public void navigateToOrders() {
        bottomNavBar.setSelectedItem(UserNavItem.ORDERS);
    }
    
    /** Programmatically switch to the Cart tab. */
    public void navigateToCart() {
        bottomNavBar.setSelectedItem(UserNavItem.CART);
    }

    /** Programmatically switch to the Saved tab. */
    public void navigateToSaved() {
        bottomNavBar.setSelectedItem(UserNavItem.SAVED);
    }

    /** Programmatically switch to the Profile tab. */
    public void navigateToProfile() {
        bottomNavBar.setSelectedItem(UserNavItem.PROFILE);
    }

    /** Update the badge on the Orders tab. */
    public void updateOrdersBadge(int count) {
        bottomNavBar.setBadgeCount(UserNavItem.ORDERS, count);
    }
    
    /** Update the badge on the Cart tab. */
    public void updateCartBadge(int count) {
        bottomNavBar.setBadgeCount(UserNavItem.CART, count);
    }
}
