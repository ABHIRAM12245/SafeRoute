package com.vlabs.routeguard.adapters;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.VerificationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MyReportsAdapter extends RecyclerView.Adapter<MyReportsAdapter.ReportViewHolder> {

    public interface OnReportActionListener {
        void onReportClick(RoadReport report);
        void onShareClick(RoadReport report);
        void onViewOnMapClick(RoadReport report);
        void onDeleteClick(RoadReport report);
    }

    private final Context context;
    private final List<RoadReport> reports = new ArrayList<>();
    private final OnReportActionListener listener;

    public MyReportsAdapter(Context context, OnReportActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setReports(List<RoadReport> newReports) {
        reports.clear();
        if (newReports != null) {
            reports.addAll(newReports);
        }
        notifyDataSetChanged();
    }

    public void removeReport(String reportId) {
        for (int i = 0; i < reports.size(); i++) {
            if (reports.get(i).getReportId() != null && reports.get(i).getReportId().equals(reportId)) {
                reports.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        RoadReport report = reports.get(position);
        holder.bind(report);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    class ReportViewHolder extends RecyclerView.ViewHolder {

        private final FrameLayout flPhotoContainer;
        private final ImageView ivReportPhoto;
        private final ImageView ivCategoryIcon;
        private final TextView tvCategoryTitle;
        private final TextView tvStatusBadge;
        private final ImageButton ibOverflowMenu;
        private final TextView tvDescription;
        private final TextView tvLocation;
        private final TextView tvTime;
        private final TextView tvConfirmationCount;
        private final TextView tvCommentCount;
        private final TextView tvTrustScore;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            flPhotoContainer = itemView.findViewById(R.id.flPhotoContainer);
            ivReportPhoto = itemView.findViewById(R.id.ivReportPhoto);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryTitle = itemView.findViewById(R.id.tvCategoryTitle);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            ibOverflowMenu = itemView.findViewById(R.id.ibOverflowMenu);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvConfirmationCount = itemView.findViewById(R.id.tvConfirmationCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            tvTrustScore = itemView.findViewById(R.id.tvTrustScore);
        }

        public void bind(RoadReport report) {
            // Whole card click
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onReportClick(report);
            });

            // Image with Placeholder fallback when photo unavailable
            String imageUrl = report.getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                flPhotoContainer.setVisibility(View.VISIBLE);
                ivReportPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                ivReportPhoto.setColorFilter(null);
                Glide.with(context)
                        .load(imageUrl.trim())
                        .placeholder(R.drawable.ic_report)
                        .error(R.drawable.ic_report)
                        .into(ivReportPhoto);
            } else {
                // Display polished placeholder container when report photo is unavailable
                flPhotoContainer.setVisibility(View.VISIBLE);
                ivReportPhoto.setImageResource(R.drawable.ic_report);
                ivReportPhoto.setScaleType(ImageView.ScaleType.CENTER);
                ivReportPhoto.setBackgroundColor(ContextCompat.getColor(context, R.color.rg_surface_variant));
                ivReportPhoto.setColorFilter(ContextCompat.getColor(context, R.color.rg_on_surface_variant));
            }

            // Category
            setupCategory(report.getType());

            // Description
            if (report.getDescription() != null && !report.getDescription().trim().isEmpty()) {
                tvDescription.setText(report.getDescription().trim());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Relative Time
            tvTime.setText("🕒 " + DateUtils.getRelativeTimeSpanString(report.getTimestamp()));

            // Location
            formatLocation(report.getLatitude(), report.getLongitude());

            // Status Chip Badge (VERIFIED, UNVERIFIED, DISPUTED, EXPIRED)
            boolean isExpired = report.getExpiresAt() > 0 && (report.getExpiresAt() - System.currentTimeMillis() <= 0);
            if (isExpired) {
                tvStatusBadge.setText("EXPIRED");
                tvStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_chip_expired));
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.rg_on_surface_variant));
            } else {
                VerificationStatus status = report.getVerificationStatus();
                if (status == VerificationStatus.VERIFIED) {
                    tvStatusBadge.setText("VERIFIED");
                    tvStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_chip_verified));
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.rg_on_primary_container));
                } else if (status == VerificationStatus.DISPUTED) {
                    tvStatusBadge.setText("DISPUTED");
                    tvStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_chip_disputed));
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                } else {
                    tvStatusBadge.setText("UNVERIFIED");
                    tvStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_chip_unverified));
                    tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.rg_on_tertiary_container));
                }
            }

            // Metrics Row: Confirmations, Comments, Trust Score (Color Coded Green / Orange / Red)
            tvConfirmationCount.setText("👍 " + report.getActiveConfirmations());
            tvCommentCount.setText("💬 " + report.getCommentCount());
            if (tvTrustScore != null) {
                int score = report.getConfidenceScore();
                tvTrustScore.setText("⭐ " + score);
                if (score >= 70) {
                    tvTrustScore.setTextColor(ContextCompat.getColor(context, R.color.rg_primary)); // Safety Green
                } else if (score >= 40) {
                    tvTrustScore.setTextColor(ContextCompat.getColor(context, R.color.rg_tertiary)); // Warning Orange
                } else {
                    tvTrustScore.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark)); // Alert Red
                }
            }

            // 3-Dot Overflow Menu
            ibOverflowMenu.setOnClickListener(v -> showPopupMenu(v, report));
        }

        private void setupCategory(ReportType type) {
            if (type == null) type = ReportType.OTHER;
            switch (type) {
                case POLICE_CHECKING:
                    tvCategoryTitle.setText(R.string.police_checking);
                    ivCategoryIcon.setImageResource(R.drawable.ic_police);
                    ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
                    break;
                case ACCIDENT:
                    tvCategoryTitle.setText(R.string.accident);
                    ivCategoryIcon.setImageResource(R.drawable.ic_accident);
                    ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark));
                    break;
                case HEAVY_TRAFFIC:
                    tvCategoryTitle.setText(R.string.heavy_traffic);
                    ivCategoryIcon.setImageResource(R.drawable.ic_traffic);
                    ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
                    break;
                case ROAD_CONSTRUCTION:
                    tvCategoryTitle.setText(R.string.road_construction);
                    ivCategoryIcon.setImageResource(R.drawable.ic_construction);
                    ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_orange_light));
                    break;
                case ANIMAL_ON_ROAD:
                    tvCategoryTitle.setText(R.string.animal_on_road);
                    ivCategoryIcon.setImageResource(R.drawable.ic_animal);
                    ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
                    break;
                case WATER_LOGGING:
                    tvCategoryTitle.setText(R.string.water_logging);
                    ivCategoryIcon.setImageResource(R.drawable.ic_water_logging);
                    ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_blue_light));
                    break;
                case OTHER:
                default:
                    tvCategoryTitle.setText(R.string.other);
                    ivCategoryIcon.setImageResource(R.drawable.ic_other);
                    ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray));
                    break;
            }
        }

        private void formatLocation(double lat, double lon) {
            String fallback = String.format(Locale.getDefault(), "📍 Lat: %.4f, Lon: %.4f", lat, lon);
            tvLocation.setText(fallback);

            // Asynchronous reverse geocoding
            new Thread(() -> {
                try {
                    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder sb = new StringBuilder();
                        if (address.getMaxAddressLineIndex() >= 0) {
                            sb.append(address.getAddressLine(0));
                        } else {
                            if (address.getLocality() != null) sb.append(address.getLocality());
                            if (address.getAdminArea() != null) {
                                if (sb.length() > 0) sb.append(", ");
                                sb.append(address.getAdminArea());
                            }
                        }
                        String locStr = sb.toString();
                        if (!locStr.isEmpty()) {
                            final String finalLocStr = "📍 " + locStr;
                            new Handler(Looper.getMainLooper()).post(() -> tvLocation.setText(finalLocStr));
                        }
                    }
                } catch (Exception ignored) {
                    // Fallback string is already displayed
                }
            }).start();
        }

        private void showPopupMenu(View anchorView, RoadReport report) {
            PopupMenu popupMenu = new PopupMenu(context, anchorView);
            popupMenu.inflate(R.menu.menu_my_report_item);
            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_share) {
                    if (listener != null) listener.onShareClick(report);
                    return true;
                } else if (id == R.id.action_view_on_map) {
                    if (listener != null) listener.onViewOnMapClick(report);
                    return true;
                } else if (id == R.id.action_delete) {
                    if (listener != null) listener.onDeleteClick(report);
                    return true;
                }
                return false;
            });
            popupMenu.show();
        }
    }
}
