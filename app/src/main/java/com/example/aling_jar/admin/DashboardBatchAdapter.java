package com.example.aling_jar.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.data.model.Batch;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DashboardBatchAdapter extends RecyclerView.Adapter<DashboardBatchAdapter.ViewHolder> {

    private List<Batch> batchList;
    private OnBatchActionListener listener;

    public interface OnBatchActionListener {
        void onEditClick(Batch batch, int position);
        void onDeleteClick(Batch batch, int position);
    }

    public DashboardBatchAdapter(List<Batch> batchList, OnBatchActionListener listener) {
        this.batchList = batchList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_batch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Batch batch = batchList.get(position);

        // Line 1: Batch ID
        String batchId = batch.getBatchId() != null ? batch.getBatchId() :
                (batch.getDocumentId() != null ? batch.getDocumentId().substring(0, Math.min(6, batch.getDocumentId().length())).toUpperCase() : "N/A");
        holder.tvBatchIdSupplier.setText("#" + batchId);

        // Line 2: Product Name (bold)
        holder.tvProductName.setText(batch.getName() != null ? batch.getName() : "Unknown Product");

        // Line 3: Size · Flavor · Ingredient
        String flavor = batch.getFlavorProfile() != null ? batch.getFlavorProfile() : "Classic";
        String ingredient = batch.getIngredient() != null ? batch.getIngredient() : ""; // Optional depending on how strict we want defaults
        String size   = batch.getSize()          != null ? batch.getSize()          : "";
        
        StringBuilder profileBuilder = new StringBuilder();
        if (!size.isEmpty()) profileBuilder.append(size).append(" · ");
        profileBuilder.append(flavor);
        if (!ingredient.isEmpty()) profileBuilder.append(" · ").append(ingredient);
        
        if (holder.tvSizeProfile != null) holder.tvSizeProfile.setText(profileBuilder.toString());

        // Dates
        StringBuilder dateBuilder = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeSdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        if (batch.getExpiryDate() != null) {
            dateBuilder.append("Exp: ").append(sdf.format(batch.getExpiryDate().toDate()));
        }

        if (batch.getCreatedAt() != null) {
            if (dateBuilder.length() > 0) dateBuilder.append("  •  ");
            dateBuilder.append("Logged: ").append(timeSdf.format(batch.getCreatedAt().toDate()));
        }

        if (dateBuilder.length() > 0) {
            holder.tvExpiryDate.setText(dateBuilder.toString());
        } else {
            holder.tvExpiryDate.setText("No dates available");
        }

        // Status badge + left border color
        String status = batch.getStatus() != null ? batch.getStatus() : "FRESH";
        holder.tvStatus.setText(status);

        switch (status.toUpperCase()) {
            case "30 DAYS OLD":
                holder.tvStatus.setText("30 DAYS");
                holder.tvStatus.setTextColor(0xFFFF9800);
                holder.itemView.setBackgroundResource(R.drawable.bg_batch_border_orange);
                break;
            case "CRITICAL":
                holder.tvStatus.setText("Near Expiry");
                holder.tvStatus.setTextColor(0xFFF44336);
                holder.itemView.setBackgroundResource(R.drawable.bg_batch_border_red);
                break;
            case "FRESH":
            default:
                holder.tvStatus.setText("FRESH");
                holder.tvStatus.setTextColor(0xFF4CAF50);
                holder.itemView.setBackgroundResource(R.drawable.bg_batch_border_green);
                break;
        }

        // Button clicks
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(batch, position);
        });

        holder.btnDelete.setOnClickListener(v -> {
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBatchIdSupplier, tvProductName, tvSizeProfile, tvExpiryDate, tvStatus;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchIdSupplier = itemView.findViewById(R.id.tvBatchIdSupplier);
            tvProductName     = itemView.findViewById(R.id.tvProductName);
            tvSizeProfile     = itemView.findViewById(R.id.tvSizeProfile);
            tvExpiryDate      = itemView.findViewById(R.id.tvExpiryDate);
            tvStatus          = itemView.findViewById(R.id.tvStatus);
            btnEdit           = itemView.findViewById(R.id.btnEdit);
            btnDelete         = itemView.findViewById(R.id.btnDelete);
        }
    }
}
