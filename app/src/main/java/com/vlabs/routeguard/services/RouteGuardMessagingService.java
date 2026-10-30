package com.vlabs.routeguard.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Firebase Cloud Messaging (FCM) Push Notification Service.
 *
 * Intercepts incoming cloud push messages dispatched to the client device, translates them
 * into localized notification channel alerts, and syncs notification entities into Cloud Firestore.
 *
 * Core Processing Pipeline:
 * 1. Payload Parsing: Extracts message title, body, reportId, and notification category (HAZARD vs COMMUNITY)
 *    from both notification payloads and data dictionaries.
 * 2. In-App Notification Store: Automatically persists a {@link com.vlabs.routeguard.models.NotificationModel}
 *    record into the Cloud Firestore {@code notifications} collection via {@link com.vlabs.routeguard.repositories.NotificationRepository}
 *    so it appears in the user's in-app notification activity feed.
 * 3. User Preference & DND Filtering:
 *    - Respects master toggle ({@link com.vlabs.routeguard.utils.AppPreferences#isNotificationsEnabled()}).
 *    - Evaluates sub-toggles for hazard alerts and general community updates.
 *    - Enforces Do Not Disturb (DND): Silences non-essential community updates while ensuring critical road hazard
 *      safety alerts are never suppressed.
 * 4. System Notification Dispatch:
 *    - Creates or reuses high-importance notification channel "routeguard_alerts".
 *    - Attaches a {@link android.app.PendingIntent} navigating directly to {@link com.vlabs.routeguard.activities.HomeActivity}
 *      with the relevant report ID.
 * 5. Token Rotation:
 *    - {@link #onNewToken(String)} automatically updates the active device token in {@code users/{userId}} via
 *      {@link com.vlabs.routeguard.repositories.UserFirestoreRepository#updateFcmToken(String, String)}.
 */
public class RouteGuardMessagingService extends FirebaseMessagingService {

    private static final String TAG = "RouteGuardMessaging";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        
        String title = null;
        String body = null;

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }
        
        // Apply defaults
        if (title == null || title.trim().isEmpty()) {
            title = "SafeRoute";
        }
        if (body == null || body.trim().isEmpty()) {
            body = "New community update available.";
        }

        String reportId = remoteMessage.getData().get("reportId");
        String type = remoteMessage.getData().get("type");
        if (type == null || type.trim().isEmpty()) {
            type = "COMMUNITY";
        }

        // Save NotificationModel into Firestore first
        String userId = remoteMessage.getData().get("userId");
        if (userId == null || userId.trim().isEmpty()) {
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getUid();
            }
        }

        if (userId != null && !userId.trim().isEmpty()) {
            com.vlabs.routeguard.models.NotificationModel notificationModel = new com.vlabs.routeguard.models.NotificationModel(
                    null,
                    title,
                    body,
                    System.currentTimeMillis(),
                    false,
                    type,
                    reportId,
                    userId
            );
            new com.vlabs.routeguard.repositories.NotificationRepository().saveNotification(notificationModel, null);
        }

        com.vlabs.routeguard.utils.AppPreferences appPrefs = new com.vlabs.routeguard.utils.AppPreferences(this);
        if (!appPrefs.isNotificationsEnabled()) {
            return;
        }

        boolean isHazard = "HAZARD".equalsIgnoreCase(type) || "TRAVEL_ALERT".equalsIgnoreCase(type)
                || title.toLowerCase().contains("hazard") || body.toLowerCase().contains("hazard");

        if (isHazard && !appPrefs.isHazardAlertsEnabled()) {
            return;
        }
        if (!isHazard && !appPrefs.isCommunityAlertsEnabled()) {
            return;
        }

        // Do Not Disturb suppresses non-essential community updates while preserving hazard safety alerts
        if (appPrefs.isDndEnabled() && !isHazard) {
            return;
        }

        String channelId = "routeguard_alerts";
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        android.app.NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
        if (channel == null) {
            channel = new android.app.NotificationChannel(channelId, "SafeRoute Alerts", android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        int defaults = 0;
        if (appPrefs.isSoundEnabled()) defaults |= androidx.core.app.NotificationCompat.DEFAULT_SOUND;
        if (appPrefs.isVibrationEnabled()) defaults |= androidx.core.app.NotificationCompat.DEFAULT_VIBRATE;

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(com.vlabs.routeguard.R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setDefaults(defaults);

        if (reportId != null && !reportId.trim().isEmpty()) {
            android.content.Intent intent = new android.content.Intent(this, com.vlabs.routeguard.activities.HomeActivity.class);
            intent.putExtra("reportId", reportId);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            int pendingFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE;
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, pendingFlags);
            builder.setContentIntent(pendingIntent);
        }

        try {
            androidx.core.app.NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Missing POST_NOTIFICATIONS permission", e);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        
        Log.d(TAG, "Refreshed token: " + token);
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            new com.vlabs.routeguard.repositories.UserFirestoreRepository().updateFcmToken(user.getUid(), token);
        }
    }
}
