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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminOrdersAdapter extends RecyclerView.Adapter<AdminOrdersAdapter.OrderCardHolder> {

    private List<Order> displayList = new ArrayList<>();
    private OnOrderActionListener listener;
    private int currentTabIndex = 0;

    public interface OnOrderActionListener {
        void onConfirmClick(Order order, int position);
        void onDeclineClick(Order order, int position);
        void onDeliverClick(Order order, int position);
    }

    public AdminOrdersAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderCardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_admin, parent, false);
        return new OrderCardHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderCardHolder holder, int position) {
        Order order = displayList.get(position);
        holder.bind(order, currentTabIndex, position);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public void setItems(List<Order> items, int tabIndex) {
        this.currentTabIndex = tabIndex;
        this.displayList = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    class OrderCardHolder extends RecyclerView.ViewHolder {
        View containerAvatar;
        ImageView imgCustomer;
        TextView tvCustomerName, tvOrderMeta, tvAddress, tvOrderItems, tvOrderTotal, tvItemCount, tvStatusBadge;
        View layoutActions;
        View btnConfirm, btnDecline, btnDeliver;

        OrderCardHolder(View itemView) {
            super(itemView);
            containerAvatar = itemView.findViewById(R.id.containerAvatar);
            imgCustomer = itemView.findViewById(R.id.imgCustomer);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderMeta = itemView.findViewById(R.id.tvOrderMeta);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvOrderItems = itemView.findViewById(R.id.tvOrderItems);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnDecline = itemView.findViewById(R.id.btnDecline);
            btnDeliver = itemView.findViewById(R.id.btnDeliver);
        }

        void bind(Order order, int tabIndex, int position) {
            tvCustomerName.setText(order.getCustomerName() != null ? order.getCustomerName() : "Unknown");
            tvOrderTotal.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotal()));

            String orderNum = order.getOrderId() != null ? order.getOrderId()
                    : (order.getDocumentId() != null && order.getDocumentId().length() >= 4
                    ? "#" + order.getDocumentId().substring(order.getDocumentId().length() - 4)
                    : "#----");
            String timeStr = "";
            if (order.getCreatedAt() != null) {
                try {
                    timeStr = new SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(order.getCreatedAt().toDate());
                } catch (Exception ignored) { }
            }
            tvOrderMeta.setText(orderNum + (timeStr.isEmpty() ? "" : " • " + timeStr));

            String addr = order.getAddress();
            if (addr != null && !addr.trim().isEmpty()) {
                tvAddress.setText("Address: " + addr);
                tvAddress.setVisibility(View.VISIBLE);
            } else {
                tvAddress.setVisibility(View.GONE);
            }

            String items = order.getItems();
            int itemCount = 1;
            if (items != null && !items.isEmpty()) {
                try {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\s*[x×]", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(items);
                    if (m.find()) {
                        itemCount = Math.max(1, Integer.parseInt(m.group(1)));
                    } else if (items.matches("^\\d+\\s*$")) {
                        itemCount = Math.max(1, Integer.parseInt(items.trim()));
                    } else {
                        int commas = items.replaceAll("[^,]", "").length();
                        itemCount = Math.max(1, commas + 1);
                    }
                } catch (Exception ignored) { }
            }
            tvItemCount.setText(itemCount + (itemCount == 1 ? " item" : " items"));
            boolean isProcessing = tabIndex != 0; // if not pending, it's processing or done
            tvItemCount.setVisibility(isProcessing ? View.GONE : View.VISIBLE);
            
            if (items != null && !items.isEmpty()) {
                String formattedItems = "• " + items.replace(", ", "\n• ");
                tvOrderItems.setText(formattedItems);
                tvOrderItems.setVisibility(View.VISIBLE);
            } else {
                tvOrderItems.setVisibility(View.GONE);
            }
            
            tvStatusBadge.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
            tvStatusBadge.setText(order.getStatus() != null ? order.getStatus().toUpperCase() : "PENDING");

            if (tabIndex == 0) {
                // Pending
                layoutActions.setVisibility(View.VISIBLE);
                btnConfirm.setVisibility(View.VISIBLE);
                btnDecline.setVisibility(View.VISIBLE);
                btnDeliver.setVisibility(View.GONE);
                containerAvatar.setBackgroundResource(R.drawable.bg_badge_green);
                imgCustomer.setColorFilter(0xFF2E7D32);
                tvOrderTotal.setTextColor(0xFF4CAF50);
            } else if (tabIndex == 1) {
                // Confirmed
                layoutActions.setVisibility(View.VISIBLE);
                btnConfirm.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                btnDeliver.setVisibility(View.VISIBLE);
                containerAvatar.setBackgroundResource(R.drawable.bg_input_rounded);
                imgCustomer.setColorFilter(0xFF78909C);
                tvOrderTotal.setTextColor(0xFF546E7A);
            } else {
                // Delivery or History
                layoutActions.setVisibility(View.GONE);
                containerAvatar.setBackgroundResource(R.drawable.bg_input_rounded);
                imgCustomer.setColorFilter(0xFF78909C);
                tvOrderTotal.setTextColor(0xFF546E7A);
            }

            btnConfirm.setOnClickListener(v -> {
                if (listener != null) listener.onConfirmClick(order, position);
            });
            btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDeclineClick(order, position);
            });
            btnDeliver.setOnClickListener(v -> {
                if (listener != null) listener.onDeliverClick(order, position);
            });
        }
    }
}
