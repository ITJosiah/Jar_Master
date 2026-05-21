package com.example.aling_jar.admin.navigation;

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
 * Custom bottom navigation bar with a modern pill-style active indicator,
 * notification badge support, and an inline FAB.
 * <p>
 * Responsibilities (Single Responsibility):
 * <ul>
 *   <li>Render nav items from {@link NavItem} enum</li>
 *   <li>Manage active/inactive visual states</li>
 *   <li>Expose badge API for the Orders tab</li>
 * </ul>
 * <p>
 * Fragment switching is delegated to the host via {@link OnNavItemSelectedListener}
 * (Dependency Inversion).
 */
public class AdminBottomNavBar extends FrameLayout {

    // ─── Listener interfaces ───────────────────────────────────────
    /**
     * Callback when a navigation item is tapped.
     */
    public interface OnNavItemSelectedListener {
        void onNavItemSelected(@NonNull NavItem item);
    }

    /**
     * Callback when the FAB is tapped.
     */
    public interface OnFabClickListener {
        void onFabClick();
    }

    // ─── State ─────────────────────────────────────────────────────
    private NavItem selectedItem = NavItem.OVERVIEW;
    private OnNavItemSelectedListener navListener;
    private OnFabClickListener fabListener;

    /** Maps each NavItem → its inflated root view inside the tray */
    private final Map<NavItem, View> itemViews = new EnumMap<>(NavItem.class);

    // ─── Colors (resolved once) ────────────────────────────────────
    private int activeIconColor;
    private int activeTextColor;
    private int inactiveIconColor;
    private int inactiveTextColor;

    // ─── Constructors ──────────────────────────────────────────────
    public AdminBottomNavBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public AdminBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AdminBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ─── Initialisation ────────────────────────────────────────────

    private void init(Context context) {
        resolveColors(context);
        inflateLayout(context);
        buildNavItems(context);
        wireFab();
    }

    private void resolveColors(Context ctx) {
        activeIconColor   = ContextCompat.getColor(ctx, R.color.nav_item_active_icon);
        activeTextColor   = ContextCompat.getColor(ctx, R.color.nav_item_active_text);
        inactiveIconColor = ContextCompat.getColor(ctx, R.color.nav_item_inactive_icon);
        inactiveTextColor = ContextCompat.getColor(ctx, R.color.nav_item_inactive_text);
    }

    private void inflateLayout(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_admin_bottom_nav, this, true);
    }

    /**
     * Creates one view for each {@link NavItem} and adds it to the tray container.
     */
    private void buildNavItems(Context context) {
        LinearLayout tray = findViewById(R.id.navTrayContainer);
        LayoutInflater inflater = LayoutInflater.from(context);
        NavItem[] values = NavItem.values();

        for (int i = 0; i < values.length; i++) {
            // Add center spacer (at index 2 for 4 items) to make room for FAB
            if (i == 2) {
                View spacer = new View(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, 1);
                params.weight = 1f; // Use 1.0f or 1f
                tray.addView(spacer, params);
            }

            NavItem item = values[i];
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

    private void wireFab() {
        View fab = findViewById(R.id.navFab);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                if (fabListener != null) fabListener.onFabClick();
            });
        }
    }

    // ─── Public API ────────────────────────────────────────────────

    public void setOnNavItemSelectedListener(@Nullable OnNavItemSelectedListener listener) {
        this.navListener = listener;
    }

    public void setOnFabClickListener(@Nullable OnFabClickListener listener) {
        this.fabListener = listener;
    }

    /**
     * Programmatically select a tab (e.g. navigateToBatches()).
     */
    public void setSelectedItem(@NonNull NavItem item) {
        if (item == selectedItem) return;
        selectItem(item);
    }

    /**
     * Show or update the badge count on a given nav item.
     * Pass {@code count <= 0} to hide the badge.
     */
    public void setBadgeCount(@NonNull NavItem item, int count) {
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

    /** Returns the currently selected NavItem. */
    public NavItem getSelectedItem() {
        return selectedItem;
    }

    // ─── Internal ──────────────────────────────────────────────────

    private void selectItem(NavItem item) {
        selectedItem = item;
        applySelectionState();
        if (navListener != null) navListener.onNavItemSelected(item);
    }

    /**
     * Applies active/inactive visual styles to all item views.
     */
    private void applySelectionState() {
        for (Map.Entry<NavItem, View> entry : itemViews.entrySet()) {
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
