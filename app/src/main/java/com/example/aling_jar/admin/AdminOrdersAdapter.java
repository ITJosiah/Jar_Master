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

public class AdminOrdersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SECTION_HEADER = 0;
    private static final int TYPE_ORDER_CARD = 1;

    private List<Object> displayList = new ArrayList<>();
    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onConfirmClick(Order order, int position);
        void onDeclineClick(Order order, int position);
    }

    public AdminOrdersAdapter(OnOrderActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = displayList.get(position);
        return item instanceof SectionHeader ? TYPE_SECTION_HEADER : TYPE_ORDER_CARD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SECTION_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_section_header, parent, false);
            return new SectionHeaderHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_order_admin, parent, false);
            return new OrderCardHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SectionHeaderHolder) {
            SectionHeader h = (SectionHeader) displayList.get(position);
            ((SectionHeaderHolder) holder).bind(h);
        } else {
            OrderCardItem item = (OrderCardItem) displayList.get(position);
            ((OrderCardHolder) holder).bind(item.order, item.isProcessing, position);
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public void setItems(List<Object> items) {
        this.displayList = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class SectionHeader {
        String title;
        String badge;
        boolean showBadge;

        SectionHeader(String title, String badge, boolean showBadge) {
            this.title = title;
            this.badge = badge;
            this.showBadge = showBadge;
        }
    }

    static class OrderCardItem {
        Order order;
        boolean isProcessing;

        OrderCardItem(Order order, boolean isProcessing) {
            this.order = order;
            this.isProcessing = isProcessing;
        }
    }

    static class SectionHeaderHolder extends RecyclerView.ViewHolder {
        TextView tvSectionTitle, tvSectionBadge;

        SectionHeaderHolder(View itemView) {
            super(itemView);
            tvSectionTitle = itemView.findViewById(R.id.tvSectionTitle);
            tvSectionBadge = itemView.findViewById(R.id.tvSectionBadge);
        }

        void bind(SectionHeader h) {
            tvSectionTitle.setText(h.title);
            tvSectionBadge.setText(h.badge);
            tvSectionBadge.setVisibility(h.showBadge ? View.VISIBLE : View.GONE);
        }
    }

    class OrderCardHolder extends RecyclerView.ViewHolder {
        View containerAvatar;
        ImageView imgCustomer;
        TextView tvCustomerName, tvOrderMeta, tvAddress, tvOrderTotal, tvItemCount, tvStatusBadge;
        View layoutActions;
        View btnConfirm, btnDecline;

        OrderCardHolder(View itemView) {
            super(itemView);
            containerAvatar = itemView.findViewById(R.id.containerAvatar);
            imgCustomer = itemView.findViewById(R.id.imgCustomer);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderMeta = itemView.findViewById(R.id.tvOrderMeta);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }

        void bind(Order order, boolean isProcessing, int position) {
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
            tvItemCount.setVisibility(isProcessing ? View.GONE : View.VISIBLE);
            tvStatusBadge.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
            tvStatusBadge.setText("PREPARING");

            if (isProcessing) {
                containerAvatar.setBackgroundResource(R.drawable.bg_input_rounded);
                imgCustomer.setColorFilter(0xFF78909C);
                tvOrderTotal.setTextColor(0xFF546E7A);
                layoutActions.setVisibility(View.GONE);
            } else {
                containerAvatar.setBackgroundResource(R.drawable.bg_badge_green);
                imgCustomer.setColorFilter(0xFF2E7D32);
                tvOrderTotal.setTextColor(0xFF4CAF50);
                layoutActions.setVisibility(View.VISIBLE);
            }

            btnConfirm.setOnClickListener(v -> {
                if (listener != null) listener.onConfirmClick(order, position);
            });
            btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDeclineClick(order, position);
            });
        }
    }
}
