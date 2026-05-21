package com.example.aling_jar.data;

import com.example.aling_jar.data.model.Batch;
import com.example.aling_jar.data.model.CartItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class that manages the user's shopping cart state.
 */
public class CartManager {
    private static CartManager instance;
    private final Map<String, CartItem> cartItems = new HashMap<>();

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    /**
     * Adds an item to the cart based on a Batch object and specified quantity.
     * Groups by product name, flavor profile, ingredient, and size.
     */
    public void addItem(Batch batch, int quantity) {
        String key = getProductKey(batch.getProductName(), batch.getFlavorProfile(), batch.getIngredient(), batch.getSize());
        if (cartItems.containsKey(key)) {
            CartItem existing = cartItems.get(key);
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem(
                    batch.getProductName(),
                    batch.getFlavorProfile(),
                    batch.getIngredient(),
                    batch.getSize(),
                    quantity,
                    batch.getPrice(),
                    batch.getPhotoUrl()
            );
            cartItems.put(key, newItem);
        }
    }

    // Overload for single additions (fallback)
    public void addItem(Batch batch) {
        addItem(batch, 1);
    }

    public void updateQuantity(CartItem item, int newQuantity) {
        String key = getProductKey(item.getProductName(), item.getFlavorProfile(), item.getIngredient(), item.getSize());
        if (newQuantity <= 0) {
            cartItems.remove(key);
        } else {
            CartItem existing = cartItems.get(key);
            if (existing != null) {
                existing.setQuantity(newQuantity);
            }
        }
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    public int getItemCount() {
        int total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getQuantity();
        }
        return total;
    }

    public double getTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getTotalPrice();
        }
        return total;
    }

    public void clear() {
        cartItems.clear();
    }

    private String getProductKey(String name, String flavor, String ingredient, String size) {
        return (name != null ? name : "Unknown") + "|" + 
               (flavor != null ? flavor : "Standard") + "|" + 
               (ingredient != null ? ingredient : "Standard") + "|" +
               (size != null ? size : "UnknownSize");
    }
}
