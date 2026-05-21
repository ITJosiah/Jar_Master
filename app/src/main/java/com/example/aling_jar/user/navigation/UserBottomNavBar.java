package com.example.aling_jar.user.navigation;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.aling_jar.R;

import java.util.EnumMap;
import java.util.Map;

/**
 * Custom bottom navigation bar for the user module.
 * <p>
 * Mirrors the admin's pill-style nav tray design but without the FAB.
 * <p>
 * Responsibilities (Single Responsibility):
 * <ul>
 *   <li>Render nav items from {@link UserNavItem} enum</li>
 *   <li>Manage active/inactive visual states</li>
 *   <li>Expose badge API for notifications</li>
 * </ul>
 * <p>
 * Fragment switching is delegated to the host via {@link OnNavItemSelectedListener}
 * (Dependency Inversion).
 */
public class UserBottomNavBar extends FrameLayout {

    // ─── Listener interface ────────────────────────────────────────

    /** Callback when a navigation item is tapped. */
    public interface OnNavItemSelectedListener {
        void onNavItemSelected(@NonNull UserNavItem item);
    }

    // ─── State ─────────────────────────────────────────────────────
    private UserNavItem selectedItem = UserNavItem.SHOP;
    private OnNavItemSelectedListener navListener;

    /** Maps each UserNavItem → its inflated root view inside the tray */
    private final Map<UserNavItem, View> itemViews = new EnumMap<>(UserNavItem.class);

    // ─── Colors (resolved once) ────────────────────────────────────
    private int activeIconColor;
    private int activeTextColor;
    private int inactiveIconColor;
    private int inactiveTextColor;

    // ─── Constructors ──────────────────────────────────────────────
    public UserBottomNavBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public UserBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public UserBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ─── Initialisation ────────────────────────────────────────────

    private void init(Context context) {
        resolveColors(context);
        inflateLayout(context);
        buildNavItems(context);
    }

    private void resolveColors(Context ctx) {
        activeIconColor   = ContextCompat.getColor(ctx, R.color.nav_item_active_icon);
        activeTextColor   = ContextCompat.getColor(ctx, R.color.nav_item_active_text);
        inactiveIconColor = ContextCompat.getColor(ctx, R.color.nav_item_inactive_icon);
        inactiveTextColor = ContextCompat.getColor(ctx, R.color.nav_item_inactive_text);
    }

    private void inflateLayout(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_user_bottom_nav, this, true);
    }

    /**
     * Creates one view for each {@link UserNavItem} and adds it to the tray container.
     */
    private void buildNavItems(Context context) {
        LinearLayout tray = findViewById(R.id.navTrayContainer);
        LayoutInflater inflater = LayoutInflater.from(context);

        for (UserNavItem item : UserNavItem.values()) {
            View itemView = inflater.inflate(R.layout.layout_nav_item, tray, false);

            ImageView icon  = itemView.findViewById(R.id.navItemIcon);
            TextView  label = itemView.findViewById(R.id.navItemLabel);

            icon.setImageResource(item.getIconRes());
            label.setText(item.getLabel());

            itemView.setOnClickListener(v -> selectItem(item));

            tray.addView(itemView);
            itemViews.put(item, itemView);
        }

        // Apply initial selection
        applySelectionState();
    }

    // ─── Public API ────────────────────────────────────────────────

    public void setOnNavItemSelectedListener(@Nullable OnNavItemSelectedListener listener) {
        this.navListener = listener;
    }

    /**
     * Programmatically select a tab.
     */
    public void setSelectedItem(@NonNull UserNavItem item) {
        if (item == selectedItem) return;
        selectItem(item);
    }

    /**
     * Show or update the badge count on a given nav item.
     * Pass {@code count <= 0} to hide the badge.
     */
    public void setBadgeCount(@NonNull UserNavItem item, int count) {
        View itemView = itemViews.get(item);
        if (itemView == null) return;

        TextView badge = itemView.findViewById(R.id.navItemBadge);
        if (badge == null) return;

        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(VISIBLE);
        } else {
            badge.setVisibility(GONE);
        }
    }

    /** Returns the currently selected UserNavItem. */
    public UserNavItem getSelectedItem() {
        return selectedItem;
    }

    // ─── Internal ──────────────────────────────────────────────────

    private void selectItem(UserNavItem item) {
        selectedItem = item;
        applySelectionState();
        if (navListener != null) navListener.onNavItemSelected(item);
    }

    /**
     * Applies active/inactive visual styles to all item views.
     */
    private void applySelectionState() {
        for (Map.Entry<UserNavItem, View> entry : itemViews.entrySet()) {
            boolean isActive = entry.getKey() == selectedItem;
            View itemView    = entry.getValue();

            styleItem(itemView, isActive);
        }
    }

    private void styleItem(View itemView, boolean active) {
        ImageView icon  = itemView.findViewById(R.id.navItemIcon);
        TextView  label = itemView.findViewById(R.id.navItemLabel);

        if (active) {
            itemView.setBackgroundResource(R.drawable.bg_nav_item_active);
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(activeIconColor));
            label.setTextColor(activeTextColor);
        } else {
            itemView.setBackground(null);
            ImageViewCompat.setImageTintList(icon, ColorStateList.valueOf(inactiveIconColor));
            label.setTextColor(inactiveTextColor);
        }
    }
}
