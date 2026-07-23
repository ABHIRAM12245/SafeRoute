package com.vlabs.routeguard.managers;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.utils.FirestoreCollections;

import java.util.List;

public class CascadeCleanupManager {

    private static final String TAG = "CascadeCleanupManager";
    private final FirebaseFirestore db;

    public CascadeCleanupManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface CleanupCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void cleanupExpiredReport(RoadReport report) {
        cleanupExpiredReport(report, null);
    }

    public void cleanupExpiredReport(RoadReport report, CleanupCallback callback) {
        if (report == null || report.getReportId() == null) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("Report or report ID is null"));
            return;
        }
        
        String reportId = report.getReportId();
        Log.d(TAG, "Starting cascade cleanup for report: " + reportId);

        // 1. Prepare to delete the main report document
        WriteBatch batch = db.batch();
        batch.delete(db.collection(FirestoreCollections.REPORTS).document(reportId));

        // Note: Firestore does not automatically delete subcollections when a document is deleted.
        // We must query and delete them individually.
        
        // 2. Delete Comments subcollection
        deleteSubcollection(db.collection(FirestoreCollections.REPORTS).document(reportId).collection(FirestoreCollections.COMMENTS));

        // 3. Delete Votes subcollection
        deleteSubcollection(db.collection(FirestoreCollections.REPORTS).document(reportId).collection("votes"));

        // 4. Delete Confirmations subcollection
        deleteSubcollection(db.collection(FirestoreCollections.REPORTS).document(reportId).collection("confirmations"));

        // 5. Commit the batch for the main report document
        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Successfully deleted main report document: " + reportId);
            if (callback != null) callback.onSuccess();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to delete main report document: " + reportId, e);
            if (callback != null) callback.onFailure(e);
        });

        // 6. Delete Notifications referencing this report
        deleteNotificationsForReport(reportId);
    }

    private void deleteSubcollection(CollectionReference collection) {
        collection.limit(100).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    batch.delete(doc.getReference());
                }
                batch.commit().addOnSuccessListener(aVoid -> {
                    // If there are more than 100, recurse.
                    if (queryDocumentSnapshots.size() == 100) {
                        deleteSubcollection(collection);
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "Failed to delete subcollection chunk", e));
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to query subcollection for deletion", e));
    }

    private void deleteNotificationsForReport(String reportId) {
        // Notifications are currently triggered locally via HomeActivity tracking.
        // We will do a generic cleanup here if any backend notifications exist in the future.
        // But we must remove the local NotificationCompat.
        // Since we don't have Context here, the HomeActivity automatically cancels notifications 
        // for reports that disappear from the active list.
    }
}
