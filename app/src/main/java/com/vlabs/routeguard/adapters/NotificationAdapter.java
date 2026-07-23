package com.vlabs.routeguard.adapters;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.NotificationModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationModel> notifications = new ArrayList<>();
    private OnNotificationClickListener clickListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationModel notification);
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.clickListener = listener;
    }

    public void setNotifications(List<NotificationModel> newNotifications) {
        notifications.clear();
        if (newNotifications != null) {
            notifications.addAll(newNotifications);
        }
        notifyDataSetChanged();
    }

    public void markAllAsRead() {
        for (NotificationModel notification : notifications) {
            notification.setRead(true);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel item = notifications.get(position);
        holder.bind(item, clickListener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        MaterialCardView cvIconContainer;
        ImageView ivNotificationIcon;
        TextView tvNotificationTitle;
        TextView tvNotificationTime;
        TextView tvNotificationMessage;
        View vUnreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            cvIconContainer = itemView.findViewById(R.id.cvIconContainer);
            ivNotificationIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvNotificationTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvNotificationTime = itemView.findViewById(R.id.tvNotificationTime);
            tvNotificationMessage = itemView.findViewById(R.id.tvNotificationMessage);
            vUnreadDot = itemView.findViewById(R.id.vUnreadDot);
        }

        public void bind(NotificationModel notification, OnNotificationClickListener listener) {
            tvNotificationTitle.setText(notification.getTitle());
            tvNotificationMessage.setText(notification.getMessage());

            // Unread vs Read card styling
            boolean isUnread = !notification.isRead();
            if (isUnread) {
                cardView.setCardElevation(6f * itemView.getContext().getResources().getDisplayMetrics().density);
                cardView.setCardBackgroundColor(Color.parseColor("#F0FDF4")); // Light green background
                tvNotificationTitle.setTypeface(null, Typeface.BOLD);
                if (vUnreadDot != null) vUnreadDot.setVisibility(View.VISIBLE);
            } else {
                cardView.setCardElevation(2f * itemView.getContext().getResources().getDisplayMetrics().density);
                cardView.setCardBackgroundColor(Color.WHITE);
                tvNotificationTitle.setTypeface(null, Typeface.NORMAL);
                if (vUnreadDot != null) vUnreadDot.setVisibility(View.GONE);
            }

            // Timestamp formatting (Relative vs Absolute Date for old items > 7 days)
            long timestamp = notification.getTimestamp();
            long now = System.currentTimeMillis();
            long SEVEN_DAYS_MS = 7 * 24 * 3600 * 1000L;

            if (timestamp > 0) {
                if (now - timestamp > SEVEN_DAYS_MS) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    tvNotificationTime.setText(sdf.format(new Date(timestamp)));
                } else {
                    CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                            timestamp,
                            now,
                            DateUtils.MINUTE_IN_MILLIS
                    );
                    tvNotificationTime.setText(timeAgo);
                }
            } else {
                tvNotificationTime.setText("Just now");
            }

            // Type-specific icon and icon container color coding
            setupNotificationTypeIcon(notification.getType());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }

        private void setupNotificationTypeIcon(String typeStr) {
            if (typeStr == null) typeStr = "";
            typeStr = typeStr.toUpperCase();

            int iconRes = R.drawable.ic_notifications;
            int bgBgColor = Color.parseColor("#D1FAE5"); // Light green
            int iconTint = ContextCompat.getColor(itemView.getContext(), R.color.rg_primary); // Safety Green

            switch (typeStr) {
                case "POLICE_CHECKING":
                case "WARNING":
                    iconRes = R.drawable.ic_police;
                    bgBgColor = Color.parseColor("#FEE2E2"); // Light red
                    iconTint = Color.parseColor("#DC2626"); // Alert red
                    break;
                case "ACCIDENT":
                    iconRes = R.drawable.ic_accident;
                    bgBgColor = Color.parseColor("#FEE2E2"); // Light red
                    iconTint = Color.parseColor("#DC2626"); // Alert red
                    break;
                case "ROAD_CONSTRUCTION":
                    iconRes = R.drawable.ic_construction;
                    bgBgColor = Color.parseColor("#FEF3C7"); // Light orange
                    iconTint = Color.parseColor("#D97706"); // Warning orange
                    break;
                case "HEAVY_TRAFFIC":
                case "TRAFFIC":
                    iconRes = R.drawable.ic_traffic;
                    bgBgColor = Color.parseColor("#FEF3C7"); // Light orange
                    iconTint = Color.parseColor("#D97706"); // Warning orange
                    break;
                case "INFO":
                case "INFORMATION":
                    iconRes = R.drawable.ic_notifications;
                    bgBgColor = Color.parseColor("#DBEAFE"); // Light blue
                    iconTint = ContextCompat.getColor(itemView.getContext(), R.color.rg_secondary); // Secondary blue
                    break;
                case "CONFIRMATION":
                case "COMMUNITY":
                default:
                    iconRes = R.drawable.ic_report;
                    bgBgColor = Color.parseColor("#D1FAE5"); // Light green
                    iconTint = ContextCompat.getColor(itemView.getContext(), R.color.rg_primary); // Safety Green
                    break;
            }

            ivNotificationIcon.setImageResource(iconRes);
            ivNotificationIcon.setColorFilter(iconTint);
            if (cvIconContainer != null) {
                cvIconContainer.setCardBackgroundColor(bgBgColor);
            }
        }
    }
}
