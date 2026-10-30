package com.vlabs.routeguard.notifications;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;
import com.vlabs.routeguard.adapters.NotificationAdapter;
import com.vlabs.routeguard.fragments.ReportDetailsBottomSheet;
import com.vlabs.routeguard.models.NotificationModel;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.repositories.NotificationRepository;
import com.vlabs.routeguard.repositories.ReportRepository;
import com.vlabs.routeguard.utils.SnackbarUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * In-App Notification Center Activity.
 *
 * Provides a categorized, chronological feed of community hazard warnings, upvote alerts,
 * and discussion comments delivered to the active user.
 *
 * Core Features & Execution Flow:
 * 1. Chronological Date Bucketing:
 *    - Partitions notifications retrieved from Cloud Firestore into three distinct chronological groups:
 *      Today (within 24 hours), Yesterday (24-48 hours), and Older (>48 hours), managed by separate adapters.
 * 2. Real-Time Interaction:
 *    - Tapping a notification invokes {@link #onNotificationClick(NotificationModel)}, which updates its read state
 *      via {@link NotificationRepository#markAsRead(String, NotificationRepository.NotificationActionCallback)}.
 *    - If the notification links to a valid {@code reportId}, loads the corresponding hazard report and
 *      presents it inside a {@link ReportDetailsBottomSheet}.
 * 3. Bulk Management:
 *    - Supports "Mark All as Read" batch updates across all active notification documents.
 * 4. Swipe-to-Refresh:
 *    - Pull-to-refresh integration via {@link SwipeRefreshLayout} querying {@code notifications/{notificationId}}.
 */
public class NotificationActivity extends BaseActivity implements NotificationAdapter.OnNotificationClickListener {

    private NotificationRepository notificationRepository;
    private ReportRepository reportRepository;

    private NotificationAdapter adapterToday;
    private NotificationAdapter adapterYesterday;
    private NotificationAdapter adapterOlder;

    private RecyclerView rvNotificationsToday;
    private RecyclerView rvNotificationsYesterday;
    private RecyclerView rvNotificationsOlder;

    private TextView tvSectionToday;
    private TextView tvSectionYesterday;
    private TextView tvSectionOlder;

    private SwipeRefreshLayout swipeRefreshLayout;
    private View llEmptyState;
    private ProgressBar progressNotifications;
    private MaterialButton btnMarkAllRead;

    private final List<NotificationModel> allToday = new ArrayList<>();
    private final List<NotificationModel> allYesterday = new ArrayList<>();
    private final List<NotificationModel> allOlder = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationRepository = new NotificationRepository();
        reportRepository = new ReportRepository();

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        llEmptyState = findViewById(R.id.llEmptyState);
        progressNotifications = findViewById(R.id.progressNotifications);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        tvSectionToday = findViewById(R.id.tvSectionToday);
        tvSectionYesterday = findViewById(R.id.tvSectionYesterday);
        tvSectionOlder = findViewById(R.id.tvSectionOlder);

        rvNotificationsToday = findViewById(R.id.rvNotificationsToday);
        rvNotificationsYesterday = findViewById(R.id.rvNotificationsYesterday);
        rvNotificationsOlder = findViewById(R.id.rvNotificationsOlder);

        setupRecyclerViews();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.rg_primary);
            swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
        }

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        }

        loadNotifications();
    }

    private void setupRecyclerViews() {
        adapterToday = new NotificationAdapter();
        adapterYesterday = new NotificationAdapter();
        adapterOlder = new NotificationAdapter();

        adapterToday.setOnNotificationClickListener(this);
        adapterYesterday.setOnNotificationClickListener(this);
        adapterOlder.setOnNotificationClickListener(this);

        if (rvNotificationsToday != null) {
            rvNotificationsToday.setLayoutManager(new LinearLayoutManager(this));
            rvNotificationsToday.setAdapter(adapterToday);
        }

        if (rvNotificationsYesterday != null) {
            rvNotificationsYesterday.setLayoutManager(new LinearLayoutManager(this));
            rvNotificationsYesterday.setAdapter(adapterYesterday);
        }

        if (rvNotificationsOlder != null) {
            rvNotificationsOlder.setLayoutManager(new LinearLayoutManager(this));
            rvNotificationsOlder.setAdapter(adapterOlder);
        }
    }

    private void loadNotifications() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (progressNotifications != null) progressNotifications.setVisibility(View.GONE);
            updateEmptyState();
            return;
        }

        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
            if (progressNotifications != null) progressNotifications.setVisibility(View.VISIBLE);
        }

        notificationRepository.fetchNotifications(currentUser.getUid(), new NotificationRepository.NotificationsCallback() {
            @Override
            public void onSuccess(List<NotificationModel> notifications) {
                runOnUiThread(() -> {
                    // Adjustment #5: Always stop SwipeRefreshLayout animation in both success and failure
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (progressNotifications != null) progressNotifications.setVisibility(View.GONE);

                    processNotifications(notifications);
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> {
                    // Adjustment #5: Always stop SwipeRefreshLayout animation
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    if (progressNotifications != null) progressNotifications.setVisibility(View.GONE);

                    Toast.makeText(NotificationActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            }
        });
    }

    private void processNotifications(List<NotificationModel> notifications) {
        allToday.clear();
        allYesterday.clear();
        allOlder.clear();

        int unreadCount = 0;
        int readCount = 0;

        if (notifications != null && !notifications.isEmpty()) {
            long now = System.currentTimeMillis();
            long ONE_DAY = 24 * 3600 * 1000L;
            long TWO_DAYS = 2 * ONE_DAY;

            for (NotificationModel notification : notifications) {
                if (notification.isRead()) {
                    readCount++;
                } else {
                    unreadCount++;
                }

                long diff = now - notification.getTimestamp();
                if (diff < ONE_DAY) {
                    allToday.add(notification);
                } else if (diff < TWO_DAYS) {
                    allYesterday.add(notification);
                } else {
                    allOlder.add(notification);
                }
            }
        }

        adapterToday.setNotifications(allToday);
        adapterYesterday.setNotifications(allYesterday);
        adapterOlder.setNotifications(allOlder);

        int adapterItemCount = adapterToday.getItemCount() + adapterYesterday.getItemCount() + adapterOlder.getItemCount();
        boolean isEmpty = allToday.isEmpty() && allYesterday.isEmpty() && allOlder.isEmpty();

        updateSectionsVisibility();
    }

    private void updateSectionsVisibility() {
        if (tvSectionToday != null) {
            tvSectionToday.setVisibility(allToday.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (rvNotificationsToday != null) {
            rvNotificationsToday.setVisibility(allToday.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (tvSectionYesterday != null) {
            tvSectionYesterday.setVisibility(allYesterday.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (rvNotificationsYesterday != null) {
            rvNotificationsYesterday.setVisibility(allYesterday.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (tvSectionOlder != null) {
            tvSectionOlder.setVisibility(allOlder.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (rvNotificationsOlder != null) {
            rvNotificationsOlder.setVisibility(allOlder.isEmpty() ? View.GONE : View.VISIBLE);
        }

        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = allToday.isEmpty() && allYesterday.isEmpty() && allOlder.isEmpty();
        if (llEmptyState != null) {
            llEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (btnMarkAllRead != null) {
            btnMarkAllRead.setEnabled(!isEmpty);
            btnMarkAllRead.setAlpha(isEmpty ? 0.5f : 1.0f);
        }
    }

    @Override
    public void onNotificationClick(NotificationModel notification) {
        if (notification == null) return;

        // Adjustment #3: Open ReportDetailsBottomSheet immediately. Mark notification as read asynchronously.
        String reportId = notification.getReportId();

        if (reportId != null && !reportId.trim().isEmpty()) {
            ArrayList<String> ids = new ArrayList<>();
            ids.add(reportId.trim());

            ReportDetailsBottomSheet bottomSheet = ReportDetailsBottomSheet.newInstance(ids, Double.NaN, Double.NaN);
            bottomSheet.show(getSupportFragmentManager(), "ReportDetailsBottomSheet");

            // Adjustment #2: Verify report existence to handle deleted/permission/network errors
            reportRepository.fetchReportById(reportId.trim(), new ReportRepository.SingleReportCallback() {
                @Override
                public void onSuccess(RoadReport report) {
                    // Report is valid and active
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (error != null && error.toLowerCase(java.util.Locale.US).contains("permission")) {
                            SnackbarUtils.showError(findViewById(android.R.id.content), "Permission denied.");
                        } else if (error != null && (error.toLowerCase(java.util.Locale.US).contains("connection") || error.toLowerCase(java.util.Locale.US).contains("network"))) {
                            SnackbarUtils.showWarning(findViewById(android.R.id.content), "Network failure: " + error);
                        } else {
                            // Expired or deleted report (Requirement 9)
                            SnackbarUtils.showInfo(findViewById(android.R.id.content), "This report is no longer available.");
                            removeNotificationFromUI(notification);
                        }
                    });
                }
            });
        }

        // Asynchronously mark notification as read in Firestore
        if (!notification.isRead() && notification.getId() != null) {
            notification.setRead(true);
            refreshAdapterForNotification(notification);

            notificationRepository.markAsRead(notification.getId(), null);
        }
    }

    private void removeNotificationFromUI(NotificationModel notification) {
        allToday.remove(notification);
        allYesterday.remove(notification);
        allOlder.remove(notification);

        adapterToday.setNotifications(allToday);
        adapterYesterday.setNotifications(allYesterday);
        adapterOlder.setNotifications(allOlder);

        updateSectionsVisibility();
    }

    private void refreshAdapterForNotification(NotificationModel notification) {
        adapterToday.notifyDataSetChanged();
        adapterYesterday.notifyDataSetChanged();
        adapterOlder.notifyDataSetChanged();
    }

    private void markAllAsRead() {
        // Adjustment #4: Mark All As Read should batch update ONLY unread notifications
        List<NotificationModel> unreadList = new ArrayList<>();
        for (NotificationModel n : allToday) if (!n.isRead()) unreadList.add(n);
        for (NotificationModel n : allYesterday) if (!n.isRead()) unreadList.add(n);
        for (NotificationModel n : allOlder) if (!n.isRead()) unreadList.add(n);

        if (unreadList.isEmpty()) {
            SnackbarUtils.showInfo(findViewById(android.R.id.content), "All notifications are already read.");
            return;
        }

        com.vlabs.routeguard.utils.DialogUtils.showConfirmation(this, "Mark All as Read", "Are you sure you want to mark all notifications as read?", "Mark Read", () -> {
            // Optimistically update UI
            for (NotificationModel n : unreadList) n.setRead(true);
            adapterToday.notifyDataSetChanged();
            adapterYesterday.notifyDataSetChanged();
            adapterOlder.notifyDataSetChanged();

            // Batch update Firestore via NotificationRepository (Adjustment #7)
            notificationRepository.markAllAsRead(unreadList, new NotificationRepository.SimpleCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> SnackbarUtils.showSuccess(findViewById(android.R.id.content), "All notifications marked as read."));
                }

                @Override
                public void onError(String errorMsg) {
                    runOnUiThread(() -> SnackbarUtils.showError(findViewById(android.R.id.content), "Failed to update all read status."));
                }
            });
        });
    }
}
