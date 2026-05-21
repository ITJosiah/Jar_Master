package com.example.aling_jar.user.navigation;

import com.example.aling_jar.R;

/**
 * Enum defining each user-facing bottom-navigation destination.
 * <p>
 * Adding a new tab only requires a new enum constant — the custom nav bar
 * iterates over {@code UserNavItem.values()} to create items dynamically
 * (Open/Closed principle).
 */
public enum UserNavItem {

    SHOP(R.drawable.shop_ic, "SHOP", R.id.nav_shop),
    CART(R.drawable.ic_cart, "CART", R.id.nav_cart),
    ORDERS(R.drawable.orders_ic, "ORDERS", R.id.nav_orders),
    SAVED(R.drawable.saved_ic, "SAVED", R.id.nav_saved),
    PROFILE(R.drawable.profile_ic, "PROFILE", R.id.nav_profile);

    private final int iconRes;
    private final String label;
    private final int menuId;

    UserNavItem(int iconRes, String label, int menuId) {
        this.iconRes = iconRes;
        this.label = label;
        this.menuId = menuId;
    }

    public int getIconRes() { return iconRes; }
    public String getLabel() { return label; }
    public int getMenuId() { return menuId; }

    /**
     * Resolve a UserNavItem from a menu-item ID.
     */
    public static UserNavItem fromMenuId(int menuId) {
        for (UserNavItem item : values()) {
            if (item.menuId == menuId) return item;
        }
        return SHOP;
    }
}
