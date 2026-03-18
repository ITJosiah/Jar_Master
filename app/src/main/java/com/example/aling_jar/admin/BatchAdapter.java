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

        // Batch number
        String batchNumber = "BATCH #" + (batch.getDocumentId() != null ?
                batch.getDocumentId().substring(0, Math.min(8, batch.getDocumentId().length())).toUpperCase() : "N/A");
        holder.tvBatchNumber.setText(batchNumber);

        // Product name
        holder.tvProductName.setText(batch.getName() != null ? batch.getName() : "Unknown Product");

        // Quantity / Units
        holder.tvUnits.setText(batch.getQuantity() + " Units");

        // Expiry date
        if (batch.getExpiryDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.tvExpiryDate.setText(sdf.format(batch.getExpiryDate().toDate()));
        } else {
            holder.tvExpiryDate.setText("No date");
        }

        // Status badge
        String status = batch.getStatus() != null ? batch.getStatus() : "FRESH";
        holder.tvStatus.setText(status);

        switch (status.toUpperCase()) {
            case "SELLING FAST":
                holder.tvStatus.setTextColor(0xFFFF9800);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_orange);
                break;
            case "CRITICAL":
                holder.tvStatus.setTextColor(0xFFF44336);
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_red);
                break;
            case "FRESH":
            default:
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
        TextView tvBatchNumber, tvProductName, tvUnits, tvExpiryDate, tvStatus, tvPrice;
        View btnEdit, btnSpoilage;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvUnits = itemView.findViewById(R.id.tvUnits);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnSpoilage = itemView.findViewById(R.id.btnSpoilage);
        }
    }
}
