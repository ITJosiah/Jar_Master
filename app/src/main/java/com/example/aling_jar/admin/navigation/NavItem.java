package com.example.aling_jar.admin.navigation;

import com.example.aling_jar.R;

/**
 * Enum defining each bottom-navigation destination.
 * <p>
 * Adding a new tab only requires a new enum constant — the custom nav bar
 * iterates over {@code NavItem.values()} to create items dynamically (Open/Closed principle).
 */
public enum NavItem {

    OVERVIEW(R.drawable.ic_overview, "OVERVIEW", R.id.nav_overview),
    INVENTORY(R.drawable.ic_inventory, "INVENTORY", R.id.nav_inventory),
    ORDERS(R.drawable.ic_orders, "ORDERS", R.id.nav_orders),
    PROFILE(R.drawable.profile_ic, "PROFILE", R.id.nav_settings);

    private final int iconRes;
    private final String label;
    private final int menuId;

    NavItem(int iconRes, String label, int menuId) {
        this.iconRes = iconRes;
        this.label = label;
        this.menuId = menuId;
    }

    public int getIconRes() { return iconRes; }
    public String getLabel() { return label; }
    public int getMenuId() { return menuId; }

    /**
     * Resolve a NavItem from a legacy menu-item ID (used by navigateToBatches / navigateToOrders).
     */
    public static NavItem fromMenuId(int menuId) {
        for (NavItem item : values()) {
            if (item.menuId == menuId) return item;
        }
        return OVERVIEW;
    }
}
