package com.vlabs.routeguard.managers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.HomeActivity;
import com.vlabs.routeguard.models.NotificationModel;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.repositories.NotificationRepository;
import com.vlabs.routeguard.utils.FilterPreferences;
import com.vlabs.routeguard.utils.IntentConstants;
import com.vlabs.routeguard.utils.LocationUtils;
import com.vlabs.routeguard.workers.TravelModeWorker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TravelModeManager {

    private static final String TAG = "TravelModeManager";
    private static final String PREFS_NAME = "TravelModePrefs";
    private static final String PREF_TRAVEL_MODE_ENABLED = "travelModeEnabled";
    private static final String PREF_STARTED_AT = "travelModeStartedAt";
    private static final String PREF_EXPIRES_AT = "travelModeExpiresAt";
    private static final String PREF_NOTIFIED_IDS = "notifiedReportIds";

    public static final long TRAVEL_MODE_DURATION_MS = 3 * 60 * 60 * 1000L; // 3 hours
    private static final String WORK_NAME = "TravelModeBackgroundWork";
    private static final String CHANNEL_ID = "travel_mode_reports";
    private static final int MAX_NOTIFIED_CACHE_SIZE = 100;

    private static TravelModeManager instance;

    public static synchronized TravelModeManager getInstance() {
        if (instance == null) {
            instance = new TravelModeManager();
        }
        return instance;
    }

    private SharedPreferences getPrefs(Context context) {
        if (context == null) return null;
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isTravelModeActive(Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (prefs == null) return false;
        boolean enabled = prefs.getBoolean(PREF_TRAVEL_MODE_ENABLED, false);
        long expiresAt = prefs.getLong(PREF_EXPIRES_AT, 0);

        if (enabled && System.currentTimeMillis() >= expiresAt) {
            // Auto turn OFF after 3 hours
            disableTravelMode(context);
            return false;
        }

        return enabled;
    }

    public long getTravelModeExpiresAt(Context context) {
        SharedPreferences prefs = getPrefs(context);
        return prefs != null ? prefs.getLong(PREF_EXPIRES_AT, 0) : 0;
    }

    public void enableTravelMode(Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (prefs == null) return;
        long now = System.currentTimeMillis();
        long expiresAt = now + TRAVEL_MODE_DURATION_MS;

        prefs.edit()
                .putBoolean(PREF_TRAVEL_MODE_ENABLED, true)
                .putLong(PREF_STARTED_AT, now)
                .putLong(PREF_EXPIRES_AT, expiresAt)
                .apply();

        scheduleBackgroundWorker(context);
    }

    public void disableTravelMode(Context context) {
        SharedPreferences prefs = getPrefs(context);
        if (prefs != null) {
            prefs.edit()
                    .putBoolean(PREF_TRAVEL_MODE_ENABLED, false)
                    .remove(PREF_STARTED_AT)
                    .remove(PREF_EXPIRES_AT)
                    .remove(PREF_NOTIFIED_IDS) // Refinement #5: Clear notified report cache
                    .apply();
        }

        cancelBackgroundWorker(context);
    }

    public void stopTravelModeOnLogout(Context context) {
        // Refinement #5: On logout: Cancel worker, disable travel mode, clear notified report cache
        if (context != null) {
            disableTravelMode(context);
        }
    }

    public void handleDeviceReboot(Context context) {
        // Refinement #8: Handle device reboot by validating expiry and rescheduling WorkManager only if active
        if (context != null && isTravelModeActive(context)) {
            scheduleBackgroundWorker(context);
        } else if (context != null) {
            disableTravelMode(context);
        }
    }

    private void scheduleBackgroundWorker(Context context) {
        if (context == null) return;
        try {
            // Refinement #2: PeriodicWorkRequest minimum interval is 15 minutes
            PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                    TravelModeWorker.class,
                    15, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWork
            );
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling background work", e);
        }
    }

    private void cancelBackgroundWorker(Context context) {
        if (context == null) return;
        try {
            WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling background work", e);
        }
    }

    /**
     * Business logic for filtering and notifying nearby reports.
     * Shared by both foreground location updates and background WorkManager execution.
     */
    public void evaluateAndNotifyReports(Context context, double userLat, double userLon, List<RoadReport> reports, String currentUserId) {
        if (!isTravelModeActive(context) || reports == null || reports.isEmpty()) {
            return;
        }

        FilterPreferences filterPrefs = new FilterPreferences(context);
        Set<String> selectedCategories = filterPrefs.loadFilters(); // Default: All Categories (Refinement #7)
        float radiusKm = filterPrefs.getNotificationRadiusKm();

        Set<String> notifiedIds = getNotifiedIds(context);
        long now = System.currentTimeMillis();

        for (RoadReport report : reports) {
            if (report == null || report.getReportId() == null) continue;

            String reportId = report.getReportId();

            // Requirement 4 checks:
            // 1. Report is not expired
            if (report.getExpiresAt() > 0 && report.getExpiresAt() <= now) {
                continue;
            }

            // 2. Report category matches user preferences
            String typeName = report.getType() != null ? report.getType().name() : "OTHER";
            if (!selectedCategories.contains(typeName)) {
                continue;
            }

            // 3. Report is within selected radius (Refinement #6: Shared distance calculation utility)
            float distanceKm = LocationUtils.calculateDistanceKm(userLat, userLon, report.getLatitude(), report.getLongitude());
            if (distanceKm > radiusKm) {
                continue;
            }

            // 4. Report has not already been notified (Requirement 9)
            if (notifiedIds.contains(reportId)) {
                continue;
            }

            // Trigger notification!
            sendAlertNotification(context, report, currentUserId);
            addNotifiedId(context, notifiedIds, reportId);
        }
    }

    private Set<String> getNotifiedIds(Context context) {
        Set<String> set = getPrefs(context).getStringSet(PREF_NOTIFIED_IDS, null);
        return set != null ? new HashSet<>(set) : new HashSet<>();
    }

    private void addNotifiedId(Context context, Set<String> currentNotifiedIds, String newReportId) {
        currentNotifiedIds.add(newReportId);

        // Refinement #4: Limit notifiedReportIds size to avoid unbounded growth
        if (currentNotifiedIds.size() > MAX_NOTIFIED_CACHE_SIZE) {
            // Remove oldest items to cap at MAX_NOTIFIED_CACHE_SIZE
            int removeCount = currentNotifiedIds.size() - MAX_NOTIFIED_CACHE_SIZE;
            Set<String> trimmed = new HashSet<>();
            int count = 0;
            for (String id : currentNotifiedIds) {
                if (count++ >= removeCount) {
                    trimmed.add(id);
                }
            }
            currentNotifiedIds = trimmed;
        }

        getPrefs(context).edit().putStringSet(PREF_NOTIFIED_IDS, new HashSet<>(currentNotifiedIds)).apply();
    }

    private void sendAlertNotification(Context context, RoadReport report, String currentUserId) {
        createNotificationChannel(context);

        String category = report.getType() != null ? report.getType().name() : "OTHER";
        String title = "Travel Mode Alert: " + category;
        String message = report.getDescription() != null && !report.getDescription().isEmpty()
                ? report.getDescription()
                : "Road hazard reported near your route.";

        Intent intent = new Intent(context, HomeActivity.class);
        intent.putExtra(IntentConstants.EXTRA_REPORT_ID, report.getReportId());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                report.getReportId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // 1. Save NotificationModel into Firestore first if user is logged in
        if (currentUserId != null && !currentUserId.isEmpty()) {
            NotificationModel notificationModel = new NotificationModel(
                    null,
                    title,
                    message,
                    System.currentTimeMillis(),
                    false,
                    category,
                    report.getReportId(),
                    currentUserId
            );
            new NotificationRepository().saveNotification(notificationModel, null);
        }

        // 2. Display Android system notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(report.getReportId().hashCode(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    private void createNotificationChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Travel Mode Alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications for nearby road reports during Travel Mode");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
