package com.example.aling_jar.user.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.admin.Order;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class UserOrderAdapter extends RecyclerView.Adapter<UserOrderAdapter.OrderViewHolder> {

    private final List<Order> orderList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault());
    private OnUserOrderActionListener listener;

    public interface OnUserOrderActionListener {
        void onCancelClick(Order order);
        void onOrderReceivedClick(Order order);
    }

    public UserOrderAdapter(List<Order> orderList, OnUserOrderActionListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderItems, tvOrderTotal, tvItemCount, tvStatusBadge;
        View layoutActions, btnCancelOrder, btnOrderReceived;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnCancelOrder = itemView.findViewById(R.id.btnCancelOrder);
            btnOrderReceived = itemView.findViewById(R.id.btnOrderReceived);
        }

        public void bind(Order order) {
            // Order ID
            String orderNum = order.getOrderId() != null ? order.getOrderId()
                    : (order.getDocumentId() != null && order.getDocumentId().length() >= 4
                    ? "Order #" + order.getDocumentId().substring(order.getDocumentId().length() - 4)
                    : "Order #----");
            tvOrderId.setText(orderNum);

            // Date
            if (order.getCreatedAt() != null) {
                tvOrderDate.setText(dateFormat.format(order.getCreatedAt().toDate()));
            } else {
                tvOrderDate.setText("");
            }

            // Items
            String items = order.getItems();
            if (items != null && !items.isEmpty()) {
                String formattedItems = "• " + items.replace(", ", "\n• ");
                tvOrderItems.setText(formattedItems);
                tvOrderItems.setVisibility(View.VISIBLE);
            } else {
                tvOrderItems.setVisibility(View.GONE);
            }

            // Total
            tvOrderTotal.setText(String.format(Locale.getDefault(), "₱%,.2f", order.getTotal()));

            // Item count
            int itemCount = 1;
            if (items != null && !items.isEmpty()) {
                try {
                    java.util.regex.Matcher m = java.util.regex.Pattern
                            .compile("(\\d+)\\s*[x×]", java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(items);
                    if (m.find()) {
                        itemCount = Math.max(1, Integer.parseInt(m.group(1)));
                    } else {
                        int commas = items.replaceAll("[^,]", "").length();
                        itemCount = Math.max(1, commas + 1);
                    }
                } catch (Exception ignored) { }
            }
            tvItemCount.setText(itemCount + (itemCount == 1 ? " item" : " items"));

            // Status badge & color theming
            String status = order.getStatus() != null ? order.getStatus() : "";
            tvStatusBadge.setText(status.toUpperCase());

            // Reset defaults
            layoutActions.setVisibility(View.GONE);
            btnCancelOrder.setVisibility(View.GONE);
            btnOrderReceived.setVisibility(View.GONE);

            if ("PENDING".equalsIgnoreCase(status)) {
                tvStatusBadge.setTextColor(0xFFE65100);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_orange);
                tvOrderTotal.setTextColor(0xFF4CAF50);
                tvItemCount.setVisibility(View.VISIBLE);

                layoutActions.setVisibility(View.VISIBLE);
                btnCancelOrder.setVisibility(View.VISIBLE);
            } else if ("CONFIRMED".equalsIgnoreCase(status)) {
                tvStatusBadge.setTextColor(0xFF1976D2);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blue);
                tvOrderTotal.setTextColor(0xFF546E7A);
                tvItemCount.setVisibility(View.GONE);
            } else if ("DELIVERY".equalsIgnoreCase(status) || "DELIVERING".equalsIgnoreCase(status)) {
                tvStatusBadge.setTextColor(0xFF1976D2);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blue);
                tvOrderTotal.setTextColor(0xFF546E7A);
                tvItemCount.setVisibility(View.GONE);

                layoutActions.setVisibility(View.VISIBLE);
                btnOrderReceived.setVisibility(View.VISIBLE);
            } else if ("CANCELLED".equalsIgnoreCase(status) || "DECLINED".equalsIgnoreCase(status)) {
                tvStatusBadge.setTextColor(0xFFD32F2F);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red);
                tvOrderTotal.setTextColor(0xFF546E7A);
                tvItemCount.setVisibility(View.GONE);
            } else if ("COMPLETED".equalsIgnoreCase(status) || "RECEIVED".equalsIgnoreCase(status)) {
                tvStatusBadge.setTextColor(0xFF2E7D32);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
                tvOrderTotal.setTextColor(0xFF546E7A);
                tvItemCount.setVisibility(View.GONE);
            } else {
                tvStatusBadge.setTextColor(0xFF78909C);
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blue);
                tvOrderTotal.setTextColor(0xFF546E7A);
                tvItemCount.setVisibility(View.GONE);
            }

            // Button clicks
            btnCancelOrder.setOnClickListener(v -> {
                if (listener != null) listener.onCancelClick(order);
            });
            btnOrderReceived.setOnClickListener(v -> {
                if (listener != null) listener.onOrderReceivedClick(order);
            });
        }
    }
}
