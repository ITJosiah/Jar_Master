package com.example.aling_jar.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;

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

        // Batch ID + Supplier line
        String batchId = batch.getDocumentId() != null
                ? "#" + batch.getDocumentId().substring(0, Math.min(4, batch.getDocumentId().length())).toUpperCase()
                : "#N/A";
        String supplier = batch.getName() != null ? batch.getName().split(" ")[0] : "";
        holder.tvBatchIdSupplier.setText(batchId + " • " + supplier);

        // Product name
        holder.tvProductName.setText(batch.getName() != null ? batch.getName() : "Unknown Product");

        // Expiry date
        if (batch.getExpiryDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvExpiryDate.setText("Exp: " + sdf.format(batch.getExpiryDate().toDate()));
        } else {
            holder.tvExpiryDate.setText("No expiry set");
        }

        // Status badge + left border color
        String status = batch.getStatus() != null ? batch.getStatus() : "FRESH";
        holder.tvStatus.setText(status);

        switch (status.toUpperCase()) {
            case "SELLING FAST":
                holder.tvStatus.setTextColor(0xFFFF9800);
                holder.itemView.setBackgroundResource(R.drawable.bg_batch_border_orange);
                break;
            case "CRITICAL":
                holder.tvStatus.setTextColor(0xFFF44336);
                holder.itemView.setBackgroundResource(R.drawable.bg_batch_border_red);
                break;
            case "FRESH":
            default:
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
        TextView tvBatchIdSupplier, tvProductName, tvExpiryDate, tvStatus;
        ImageView btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchIdSupplier = itemView.findViewById(R.id.tvBatchIdSupplier);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvExpiryDate = itemView.findViewById(R.id.tvExpiryDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
