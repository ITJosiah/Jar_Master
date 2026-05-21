package com.example.aling_jar.user.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aling_jar.R;
import com.example.aling_jar.data.model.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList = new ArrayList<>();

    public void setNotifications(List<Notification> notifications) {
        this.notificationList = notifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvMessage, tvTime;
        ImageView ivIcon;
        View vUnreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            vUnreadIndicator = itemView.findViewById(R.id.vUnreadIndicator);
        }

        public void bind(Notification notification) {
            tvTitle.setText(notification.getTitle());
            tvMessage.setText(notification.getMessage());

            // Format timestamp roughly
            long now = System.currentTimeMillis();
            long diff = now - notification.getTimestamp();
            if (diff < 60 * 60 * 1000) {
                // less than an hour
                long mins = diff / (60 * 1000);
                tvTime.setText(Math.max(1, mins) + "m");
            } else if (diff < 24 * 60 * 60 * 1000) {
                // less than a day
                long hours = diff / (60 * 60 * 1000);
                tvTime.setText(hours + "h");
            } else {
                // days
                long days = diff / (24 * 60 * 60 * 1000);
                tvTime.setText(days + "d");
            }

            // Bold/Color styling based on unread vs read
            if (notification.isRead()) {
                Typeface regFont = ResourcesCompat.getFont(itemView.getContext(), R.font.inter_regular);
                Typeface medFont = ResourcesCompat.getFont(itemView.getContext(), R.font.inter_medium);
                tvTitle.setTypeface(medFont);
                tvMessage.setTypeface(regFont);
                
                tvTitle.setTextColor(ResourcesCompat.getColor(itemView.getResources(), R.color.text_secondary, null));
                tvMessage.setTextColor(ResourcesCompat.getColor(itemView.getResources(), R.color.text_hint, null));
                
                // Dim the green tint slightly for the icon to match the read style
                ivIcon.setAlpha(0.6f);
            } else {
                Typeface boldFont = ResourcesCompat.getFont(itemView.getContext(), R.font.inter_bold);
                Typeface semiboldFont = ResourcesCompat.getFont(itemView.getContext(), R.font.inter_semibold);
                tvTitle.setTypeface(boldFont);
                tvMessage.setTypeface(semiboldFont);
                
                tvTitle.setTextColor(ResourcesCompat.getColor(itemView.getResources(), R.color.text_primary, null));
                tvMessage.setTextColor(ResourcesCompat.getColor(itemView.getResources(), R.color.text_secondary, null));
                
                // Solid icon for unread
                ivIcon.setAlpha(1.0f);
            }

            vUnreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
        }
    }
}
