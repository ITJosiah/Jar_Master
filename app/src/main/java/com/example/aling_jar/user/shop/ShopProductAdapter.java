package com.example.aling_jar.user.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.aling_jar.R;
import com.example.aling_jar.data.model.Batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for the user's product shop grid.
 * <p>
 * Responsibilities (Single Responsibility):
 * <ul>
 *   <li>Bind {@link Batch} data to shop product card views</li>
 *   <li>Load product images via Glide</li>
 *   <li>Delegate user interactions (add-to-cart, favorite) to host via listener</li>
 * </ul>
 */
public class ShopProductAdapter extends RecyclerView.Adapter<ShopProductAdapter.ProductViewHolder> {

    private List<Batch> productList;
    private List<String> savedItemIds = new ArrayList<>();
    private OnProductActionListener listener;

    // ─── Listener interface ────────────────────────────────────────

    /**
     * Callback for product-level user actions.
     * Implementing the Interface Segregation principle: only expose
     * the actions a consumer cares about.
     */
    public interface OnProductActionListener {
        void onAddToCartClick(@NonNull Batch batch, int position);
        void onFavoriteClick(@NonNull Batch batch, int position);
    }

    // ─── Constructor ───────────────────────────────────────────────

    public ShopProductAdapter(@NonNull List<Batch> productList,
                              @NonNull OnProductActionListener listener) {
        this.productList = new ArrayList<>(productList);
        this.listener = listener;
    }

    // ─── Adapter overrides ─────────────────────────────────────────

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Batch batch = productList.get(position);
        String itemId = batch.getProductName() + "_" + batch.getFlavorProfile();
        boolean isSaved = savedItemIds.contains(itemId);
        holder.bind(batch, position, isSaved, listener);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    // ─── Public API ────────────────────────────────────────────────

    /** Replace the entire dataset and refresh the list. */
    public void updateList(@NonNull List<Batch> newList) {
        this.productList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public void setSavedItemIds(List<String> savedItemIds) {
        this.savedItemIds = new ArrayList<>(savedItemIds);
        notifyDataSetChanged();
    }

    // ─── ViewHolder ────────────────────────────────────────────────

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivProductImage;
        private final ImageView ivFavorite;
        private final TextView tvProductName;
        private final TextView tvQuantityLabel;
        private final TextView tvPrice;
        private final ImageView ivAddToCart;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            ivFavorite     = itemView.findViewById(R.id.ivFavorite);
            tvProductName  = itemView.findViewById(R.id.tvProductName);
            tvQuantityLabel = itemView.findViewById(R.id.tvQuantityLabel);
            tvPrice        = itemView.findViewById(R.id.tvPrice);
            ivAddToCart    = itemView.findViewById(R.id.ivAddToCart);
        }

        void bind(@NonNull Batch batch, int position, boolean isSaved,
                  @NonNull OnProductActionListener listener) {
            // Product name (e.g. "Classic Laing")
            String pName = batch.getProductName() != null ? batch.getProductName() : "Product";
            String fName = batch.getFlavorProfile() != null ? batch.getFlavorProfile() : "Classic";
            tvProductName.setText(fName + " " + pName);

            // Variations descriptor (Removed per request)
            tvQuantityLabel.setVisibility(android.view.View.GONE);

            // Price
            tvPrice.setText(formatPrice(batch.getPrice()));

            // Product image via Glide
            loadProductImage(batch);

            // Heart state
            if (isSaved) {
                ivFavorite.setImageResource(R.drawable.ic_heart_filled);
                ivFavorite.setColorFilter(itemView.getContext().getColor(R.color.user_heart_red));
            } else {
                ivFavorite.setImageResource(R.drawable.ic_heart_outline);
                ivFavorite.setColorFilter(null);
            }

            // Click listeners
            ivAddToCart.setOnClickListener(v -> listener.onAddToCartClick(batch, position));
            ivFavorite.setOnClickListener(v -> listener.onFavoriteClick(batch, position));
        }

        /**
         * Loads the product image via Glide.
         * Cloudinary returns plain HTTPS URLs, so no resolution is needed.
         */
        private void loadProductImage(Batch batch) {
            String photoUrl = batch.getPhotoUrl();
            int defaultRes = R.drawable.ic_placeholder_transparent;
            
            String pName = batch.getProductName() != null ? batch.getProductName().toLowerCase() : "";
            if (pName.contains("sinantol")) {
                defaultRes = R.drawable.img_sinantol_default;
            } else if (pName.contains("laing")) {
                defaultRes = R.drawable.img_laing_default;
            }

            if (photoUrl != null && !photoUrl.isEmpty() && !photoUrl.startsWith("http")) {
                // If it's a local resource name or placeholder, we skip Glide or handle it
            }

            Glide.with(ivProductImage.getContext())
                    .load(photoUrl != null && photoUrl.startsWith("http") ? photoUrl : defaultRes)
                    .transform(new CenterCrop(), new RoundedCorners(32))
                    .placeholder(defaultRes)
                    .error(defaultRes)
                    .into(ivProductImage);
        }

        private String formatPrice(double price) {
            return String.format(Locale.getDefault(), "₱%,.2f", price);
        }

        // Used for variations now, not individual batch sizes
        private String formatQuantityLabel(long quantity) {
            return "Variations Available";
        }
    }
}
