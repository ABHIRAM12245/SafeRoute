package com.vlabs.routeguard.repositories;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.vlabs.routeguard.models.NotificationModel;
import com.vlabs.routeguard.utils.FirestoreCollections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationRepository extends BaseRepository {

    private static final String TAG = "NotificationRepository";
    private final FirebaseFirestore db;

    public interface NotificationsCallback {
        void onSuccess(List<NotificationModel> notifications);
        void onError(String errorMsg);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String errorMsg);
    }

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Save/Persist a NotificationModel to Firestore.
     */
    public void saveNotification(NotificationModel notification) {
        saveNotification(notification, null);
    }

    public interface UnreadCountCallback {
        void onSuccess(int unreadCount);
        void onError(String errorMsg);
    }

    /**
     * Save/Persist a NotificationModel to Firestore with duplicate prevention check.
     */
    public void saveNotification(NotificationModel notification, SimpleCallback callback) {
        if (notification == null) {
            if (callback != null) callback.onError("Invalid notification");
            return;
        }

        String userId = notification.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                userId = user.getUid();
                notification.setUserId(userId);
            }
        }

        if (userId == null || userId.trim().isEmpty()) {
            Log.w(TAG, "Cannot save notification: User ID is missing");
            if (callback != null) callback.onError("Invalid user ID");
            return;
        }

        String reportId = notification.getReportId();
        String type = notification.getType();

        // 1. Prevent duplicate notifications for same reportId + type within a 10-minute time window
        if (reportId != null && !reportId.trim().isEmpty() && type != null && !type.trim().isEmpty()) {
            long TEN_MINUTES_MS = 10 * 60 * 1000L;
            long cutoffTimestamp = System.currentTimeMillis() - TEN_MINUTES_MS;

            db.collection(FirestoreCollections.NOTIFICATIONS)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("reportId", reportId.trim())
                    .whereEqualTo("type", type.trim())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        boolean duplicateExists = false;
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                NotificationModel model = mapDocumentToNotification(doc);
                                if (model != null && !model.isDeleted() && model.getTimestamp() >= cutoffTimestamp) {
                                    duplicateExists = true;
                                    break;
                                }
                            }
                        }

                        if (duplicateExists) {
                            Log.d(TAG, "Recent duplicate notification (within 10 mins) detected for reportId '" + reportId + "' and type '" + type + "'. Skipping save.");
                            if (callback != null) callback.onSuccess();
                            return;
                        }

                        executeSaveNotification(notification, callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error verifying duplicate notification, proceeding with save", e);
                        executeSaveNotification(notification, callback);
                    });
        } else {
            executeSaveNotification(notification, callback);
        }
    }

    private void executeSaveNotification(NotificationModel notification, SimpleCallback callback) {
        com.google.firebase.firestore.DocumentReference docRef;
        if (notification.getId() != null && !notification.getId().trim().isEmpty()) {
            docRef = db.collection(FirestoreCollections.NOTIFICATIONS).document(notification.getId().trim());
        } else {
            docRef = db.collection(FirestoreCollections.NOTIFICATIONS).document();
            notification.setId(docRef.getId());
        }

        long timestamp = notification.getTimestamp() > 0 ? notification.getTimestamp() : System.currentTimeMillis();
        notification.setTimestamp(timestamp);

        Map<String, Object> data = new HashMap<>();
        data.put("id", notification.getId());
        data.put("title", notification.getTitle() != null ? notification.getTitle() : "");
        data.put("message", notification.getMessage() != null ? notification.getMessage() : "");
        data.put("timestamp", timestamp);
        data.put("isRead", notification.isRead());
        data.put("type", notification.getType() != null ? notification.getType() : "GENERAL");
        data.put("reportId", notification.getReportId());
        data.put("userId", notification.getUserId());
        data.put("isDeleted", notification.isDeleted());

        docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification saved to Firestore: " + notification.getId() + " for user " + notification.getUserId());
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save notification: " + notification.getId(), e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Real-time snapshot listener for unread notification count.
     */
    public com.google.firebase.firestore.ListenerRegistration listenToUnreadCount(String userId, UnreadCountCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            if (callback != null) callback.onSuccess(0);
            return null;
        }

        return db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error listening to unread notifications", e);
                        if (callback != null) callback.onError(e.getMessage());
                        return;
                    }

                    int unreadCount = 0;
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Boolean isRead = doc.getBoolean("isRead");
                            Boolean isDeleted = doc.getBoolean("isDeleted");
                            if ((isRead == null || !isRead) && (isDeleted == null || !isDeleted)) {
                                unreadCount++;
                            }
                        }
                    }
                    if (callback != null) callback.onSuccess(unreadCount);
                });
    }

    /**
     * Query notifications where userId == currentUser.
     * Client-side filtering ensures non-deleted notifications are returned without requiring complex composite index.
     */
    public void fetchNotifications(String userId, NotificationsCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError("Invalid user");
            return;
        }

        Log.d("NotificationDebug", "Current UID: " + userId);

        db.collection(FirestoreCollections.NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<NotificationModel> list = new ArrayList<>();
                    int returnedCount = queryDocumentSnapshots != null ? queryDocumentSnapshots.size() : 0;
                    Log.d("NotificationDebug", "Documents returned: " + returnedCount);

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            NotificationModel notification = mapDocumentToNotification(doc);
                            if (notification != null && !notification.isDeleted()) {
                                list.add(notification);
                            }
                        }
                        // Sort by timestamp descending
                        list.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                    }
                    Log.d("NotificationDebug", "Documents after filtering: " + list.size());
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching notifications for user " + userId, e);
                    callback.onError("Failed to load notifications.");
                });
    }

    /**
     * Mark single notification as read in Firestore
     */
    public void markAsRead(String notificationId, SimpleCallback callback) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            if (callback != null) callback.onError("Invalid notification ID");
            return;
        }

        db.collection(FirestoreCollections.NOTIFICATIONS)
                .document(notificationId.trim())
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification read: " + notificationId, e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Batch update only unread notifications to isRead = true
     */
    public void markAllAsRead(List<NotificationModel> unreadNotifications, SimpleCallback callback) {
        if (unreadNotifications == null || unreadNotifications.isEmpty()) {
            if (callback != null) callback.onSuccess();
            return;
        }

        WriteBatch batch = db.batch();
        int count = 0;

        for (NotificationModel notification : unreadNotifications) {
            if (notification != null && !notification.isRead() && notification.getId() != null) {
                batch.update(db.collection(FirestoreCollections.NOTIFICATIONS).document(notification.getId()), "isRead", true);
                count++;
            }
        }

        if (count == 0) {
            if (callback != null) callback.onSuccess();
            return;
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error batch updating notifications as read", e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Soft delete notification with isDeleted=true and deletedAt timestamp
     */
    public void softDeleteNotification(String notificationId, SimpleCallback callback) {
        if (notificationId == null || notificationId.trim().isEmpty()) {
            if (callback != null) callback.onError("Invalid notification ID");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("isDeleted", true);
        updates.put("deletedAt", FieldValue.serverTimestamp());

        db.collection(FirestoreCollections.NOTIFICATIONS)
                .document(notificationId.trim())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error soft deleting notification: " + notificationId, e);
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    private NotificationModel mapDocumentToNotification(DocumentSnapshot doc) {
        try {
            if (!doc.exists()) return null;

            String id = doc.getId();
            String title = doc.getString("title");
            String message = doc.getString("message");
            String type = doc.getString("type");
            String reportId = doc.getString("reportId");
            String userId = doc.getString("userId");
            Boolean isRead = doc.getBoolean("isRead");
            Boolean isDeleted = doc.getBoolean("isDeleted");

            long timestamp = 0L;
            if (doc.contains("createdAt")) {
                Object createdAtObj = doc.get("createdAt");
                if (createdAtObj instanceof Timestamp) {
                    timestamp = ((Timestamp) createdAtObj).toDate().getTime();
                } else if (createdAtObj instanceof Long) {
                    timestamp = (Long) createdAtObj;
                }
            } else if (doc.contains("timestamp")) {
                Object tsObj = doc.get("timestamp");
                if (tsObj instanceof Timestamp) {
                    timestamp = ((Timestamp) tsObj).toDate().getTime();
                } else if (tsObj instanceof Long) {
                    timestamp = (Long) tsObj;
                }
            }

            NotificationModel model = new NotificationModel();
            model.setId(id);
            model.setTitle(title != null ? title : "");
            model.setMessage(message != null ? message : "");
            model.setType(type != null ? type : "GENERAL");
            model.setReportId(reportId);
            model.setUserId(userId);
            model.setRead(isRead != null && isRead);
            model.setDeleted(isDeleted != null && isDeleted);
            model.setTimestamp(timestamp);

            return model;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notification doc " + doc.getId(), e);
            return null;
        }
    }
}
