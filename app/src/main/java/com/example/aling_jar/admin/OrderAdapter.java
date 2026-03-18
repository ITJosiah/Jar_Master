package com.example.aling_jar.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onConfirmClick(Order order, int position);
        void onDeclineClick(Order order, int position);
    }

    public OrderAdapter(List<Order> orderList, OnOrderActionListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);

        // Customer name
        holder.tvCustomerName.setText(order.getCustomerName() != null ?
                order.getCustomerName() : "Unknown Customer");

        // Order items
        holder.tvOrderItems.setText(order.getItems() != null ?
                order.getItems() : "No items");

        // Total
        holder.tvOrderTotal.setText(String.format("₱%.2f", order.getTotal()));

        // Time ago
        if (order.getCreatedAt() != null) {
            long diffMs = System.currentTimeMillis() - order.getCreatedAt().toDate().getTime();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
            long hours = TimeUnit.MILLISECONDS.toHours(diffMs);
            long days = TimeUnit.MILLISECONDS.toDays(diffMs);

            if (minutes < 1) {
                holder.tvOrderTime.setText("Just now");
            } else if (minutes < 60) {
                holder.tvOrderTime.setText(minutes + " mins ago");
            } else if (hours < 24) {
                holder.tvOrderTime.setText(hours + "h ago");
            } else {
                holder.tvOrderTime.setText(days + "d ago");
            }
        } else {
            holder.tvOrderTime.setText("");
        }

        // Button clicks
        holder.btnConfirm.setOnClickListener(v -> {
            if (listener != null) listener.onConfirmClick(order, position);
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) listener.onDeclineClick(order, position);
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public void updateList(List<Order> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvOrderTime, tvOrderItems, tvOrderTotal;
        View btnConfirm, btnDecline;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderTime = itemView.findViewById(R.id.tvOrderTime);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
