package com.example.aling_jar.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BatchAdapter extends RecyclerView.Adapter<BatchAdapter.BatchViewHolder> {

    private List<Batch> batchList;
    private OnBatchClickListener listener;

    public interface OnBatchClickListener {
        void onEditClick(Batch batch, int position);
        void onDeleteClick(Batch batch, int position);
    }

    public BatchAdapter(List<Batch> batchList, OnBatchClickListener listener) {
        this.batchList = batchList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_batch, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        Batch batch = batchList.get(position);

        // Batch ID display
        String batchId = batch.getBatchId() != null ? batch.getBatchId() :
                (batch.getDocumentId() != null ? batch.getDocumentId().substring(0, Math.min(8, batch.getDocumentId().length())).toUpperCase() : "N/A");
        holder.tvBatchNumber.setText("BATCH #" + batchId);

        // Product name
        String productName = batch.getName() != null ? batch.getName() : "Unknown Product";
        String size = batch.getSize() != null ? " · " + batch.getSize() : "";
        holder.tvProductName.setText(productName + size);

        // Flavor profile and Ingredient (Classic / Spicy / Pork / Tinapa)
        if (holder.tvFlavorProfile != null) {
            String flavor = batch.getFlavorProfile();
            String ingredient = batch.getIngredient();
            
            StringBuilder flavorText = new StringBuilder();
            if (flavor != null && !flavor.isEmpty()) {
                flavorText.append(flavor);
            }
            if (ingredient != null && !ingredient.isEmpty()) {
                if (flavorText.length() > 0) flavorText.append(" · ");
                flavorText.append(ingredient);
            }

            if (flavorText.length() > 0) {
                holder.tvFlavorProfile.setVisibility(android.view.View.VISIBLE);
                holder.tvFlavorProfile.setText(flavorText.toString());
                // Tint spicy differently for quick visual distinction
                holder.tvFlavorProfile.setTextColor(
                        "Spicy".equalsIgnoreCase(flavor) ? 0xFFE53935 : 0xFF666666);
            } else {
                holder.tvFlavorProfile.setVisibility(android.view.View.GONE);
            }
        }

        // Price
        if (holder.tvPrice != null) {
            holder.tvPrice.setText(String.format(Locale.getDefault(), "\u20B1%.2f", batch.getPrice()));
        }

        // Quantity / Jars
        holder.tvUnits.setText(batch.getQuantity() + " Jars");

        // Expiry date: always computed as createdAt + 91 days
        if (batch.getCreatedAt() != null) {
            java.util.Calendar expCal = java.util.Calendar.getInstance();
            expCal.setTime(batch.getCreatedAt().toDate());
            expCal.add(java.util.Calendar.DAY_OF_YEAR, 91);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.tvExpiryDate.setText("Exp: " + sdf.format(expCal.getTime()));
        } else {
            holder.tvExpiryDate.setText("Exp: No date");
        }

        // Logged time
        if (batch.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            holder.tvLoggedTime.setText("Logged: " + sdf.format(batch.getCreatedAt().toDate()));
        } else {
            holder.tvLoggedTime.setVisibility(View.GONE);
        }

        // Status badge
        String status = batch.getStatus() != null ? batch.getStatus() : "FRESH";
        holder.tvStatus.setText(status);

        switch (status.toUpperCase()) {
            case "30 DAYS OLD":
                holder.tvStatus.setText("30 DAYS");
                holder.tvStatus.setTextColor(0xFFFF9800);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_orange);
                break;
            case "CRITICAL":
                holder.tvStatus.setText("Near Expiry");
                holder.tvStatus.setTextColor(0xFFF44336);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_red);
                break;
            case "FRESH":
            default:
                holder.tvStatus.setText("FRESH");
                holder.tvStatus.setTextColor(0xFF4CAF50);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
                break;
        }

        // Button clicks
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(batch, position);
        });

        holder.btnSpoilage.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(batch, position);
        });
    }

    @Override
    public int getItemCount() {
        return batchList != null ? batchList.size() : 0;
    }

    public void updateList(List<Batch> newList) {
        this.batchList = newList;
        notifyDataSetChanged();
    }

    static class BatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvBatchNumber, tvProductName, tvFlavorProfile, tvUnits, tvExpiryDate, tvLoggedTime, tvStatus, tvPrice;
        View btnEdit, btnSpoilage;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchNumber   = itemView.findViewById(R.id.tvBatchNumber);
            tvProductName   = itemView.findViewById(R.id.tvProductName);
            tvFlavorProfile = itemView.findViewById(R.id.tvFlavorProfile);
            tvUnits         = itemView.findViewById(R.id.tvUnits);
            tvExpiryDate    = itemView.findViewById(R.id.tvExpiryDate);
            tvLoggedTime    = itemView.findViewById(R.id.tvLoggedTime);
            tvStatus        = itemView.findViewById(R.id.tvStatus);
            tvPrice         = itemView.findViewById(R.id.tvPrice);
            btnEdit         = itemView.findViewById(R.id.btnEdit);
            btnSpoilage     = itemView.findViewById(R.id.btnSpoilage);
        }
    }
}
