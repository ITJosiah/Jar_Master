package com.example.aling_jar.admin;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.data.model.VariationStock;

import java.util.ArrayList;
import java.util.List;

public class VariationStockAdapter extends RecyclerView.Adapter<VariationStockAdapter.ViewHolder> {

    private List<VariationStock> variations;

    public VariationStockAdapter(List<VariationStock> variations) {
        this.variations = new ArrayList<>(variations);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_variation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VariationStock var = variations.get(position);
        
        holder.tvVarName.setText(var.getFlavorProfile() + " " + var.getProductName());
        holder.tvVarDetails.setText(var.getIngredient() + " • " + var.getSize());
        
        holder.tvVarStock.setText(var.getStock() + " Jars");
        
        if (var.getStock() == 0) {
            holder.tvVarStock.setTextColor(Color.parseColor("#F44336")); // Red for out of stock
        } else {
            holder.tvVarStock.setTextColor(Color.parseColor("#4CAF50")); // Green for in stock
        }
    }

    @Override
    public int getItemCount() {
        return variations != null ? variations.size() : 0;
    }

    public void updateList(List<VariationStock> newList) {
        this.variations = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVarName, tvVarDetails, tvVarStock;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVarName = itemView.findViewById(R.id.tvVarName);
            tvVarDetails = itemView.findViewById(R.id.tvVarDetails);
            tvVarStock = itemView.findViewById(R.id.tvVarStock);
        }
    }
}
