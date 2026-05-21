package com.example.aling_jar.user.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aling_jar.R;
import com.example.aling_jar.data.CartManager;
import com.example.aling_jar.data.model.CartItem;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final List<CartItem> items;
    private final OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onQuantityChanged();
    }

    public CartAdapter(List<CartItem> items, OnCartChangeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_product, parent, false);
        return new CartViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage, btnMinus, btnPlus;
        TextView tvProductName, tvFlavor, tvPrice;
        EditText etQuantity;
        TextWatcher activeWatcher;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvFlavor = itemView.findViewById(R.id.tvFlavor);
            etQuantity = itemView.findViewById(R.id.etQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }

        void bind(CartItem item, OnCartChangeListener listener) {
            // Remove previous active watcher before binding to avoid triggers on recycled views
            if (activeWatcher != null) {
                etQuantity.removeTextChangedListener(activeWatcher);
            }

            tvProductName.setText(item.getProductName());
            StringBuilder details = new StringBuilder();
            if (item.getSize() != null && !item.getSize().isEmpty()) {
                details.append(item.getSize());
            }
            if (item.getFlavorProfile() != null && !item.getFlavorProfile().isEmpty()) {
                if (details.length() > 0) details.append(" · ");
                details.append(item.getFlavorProfile());
            }
            if (item.getIngredient() != null && !item.getIngredient().isEmpty()) {
                if (details.length() > 0) details.append(" · ");
                details.append(item.getIngredient());
            }
            tvFlavor.setText(details.toString());
            etQuantity.setText(String.valueOf(item.getQuantity()));
            tvPrice.setText(String.format(Locale.getDefault(), "₱%,.0f", item.getTotalPrice()));

            int defaultRes = R.drawable.ic_laing; // fallback default
            String pName = item.getProductName() != null ? item.getProductName().toLowerCase() : "";
            if (pName.contains("sinantol")) {
                defaultRes = R.drawable.img_sinantol_default;
            } else if (pName.contains("laing")) {
                defaultRes = R.drawable.img_laing_default;
            }

            Glide.with(itemView.getContext())
                    .load(item.getPhotoUrl() != null && item.getPhotoUrl().startsWith("http") ? item.getPhotoUrl() : defaultRes)
                    .placeholder(defaultRes)
                    .error(defaultRes)
                    .into(ivProductImage);

            btnPlus.setOnClickListener(v -> {
                int newQty = item.getQuantity() + 1;
                CartManager.getInstance().updateQuantity(item, newQty);
                etQuantity.setText(String.valueOf(newQty));
                tvPrice.setText(String.format(Locale.getDefault(), "₱%,.0f", item.getTotalPrice()));
                listener.onQuantityChanged();
            });

            btnMinus.setOnClickListener(v -> {
                int newQty = item.getQuantity() - 1;
                CartManager.getInstance().updateQuantity(item, newQty);
                if (newQty > 0) {
                    etQuantity.setText(String.valueOf(newQty));
                    tvPrice.setText(String.format(Locale.getDefault(), "₱%,.0f", item.getTotalPrice()));
                }
                listener.onQuantityChanged();
            });

            // Create new text watcher bound to this specific CartItem
            activeWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(android.text.Editable s) {
                    String input = s.toString().trim();
                    if (input.isEmpty()) return;
                    try {
                        int val = Integer.parseInt(input);
                        if (val == item.getQuantity()) return;
                        if (val >= 1) {
                            CartManager.getInstance().updateQuantity(item, val);
                            tvPrice.setText(String.format(Locale.getDefault(), "₱%,.0f", item.getTotalPrice()));
                            listener.onQuantityChanged();
                        }
                    } catch (NumberFormatException ignored) {}
                }
            };
            etQuantity.addTextChangedListener(activeWatcher);
        }
    }
}
