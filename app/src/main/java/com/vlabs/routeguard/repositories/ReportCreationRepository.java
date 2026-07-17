package com.vlabs.routeguard.repositories;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vlabs.routeguard.models.ReportStatus;
import com.vlabs.routeguard.models.RoadReportDraft;
import com.vlabs.routeguard.utils.FirestoreCollections;
import com.vlabs.routeguard.utils.FirestoreFields;
import com.vlabs.routeguard.utils.ReportDefaults;
import com.vlabs.routeguard.managers.ReportLifecycleManager;
import com.vlabs.routeguard.managers.UploadManager;

import java.util.HashMap;
import java.util.Map;

public class ReportCreationRepository extends BaseRepository {

    private final FirebaseFirestore db;
    private final ReportLifecycleManager lifecycleManager;
    private final UploadManager uploadManager;

    public interface ReportSubmissionCallback {
        void onSuccess();
        void onError(String error);
    }

    public ReportCreationRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.lifecycleManager = new ReportLifecycleManager();
        this.uploadManager = new UploadManager();
    }

    public void submitReport(RoadReportDraft draft, String reporterId, ReportSubmissionCallback callback) {
        if (draft.getLocation() == null || draft.getCategory() == null || reporterId == null || reporterId.isEmpty()) {
            callback.onError("Validation failed: missing required fields.");
            return;
        }

        if (draft.getPhotoAttachment() != null) {
            uploadManager.upload(draft.getPhotoAttachment(), new StorageRepository.UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    saveToFirestore(draft, reporterId, downloadUrl, callback);
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onError("Upload failed: " + e.getMessage());
                }
            });
        } else {
            saveToFirestore(draft, reporterId, null, callback);
        }
    }

    private void saveToFirestore(RoadReportDraft draft, String reporterId, String imageUrl, ReportSubmissionCallback callback) {
        DocumentReference newReportRef = db.collection(FirestoreCollections.REPORTS).document();
        String reportId = newReportRef.getId();

        Map<String, Object> reportData = new HashMap<>();
        reportData.put(FirestoreFields.REPORT_ID, reportId);
        reportData.put(FirestoreFields.TYPE, draft.getCategory().name());
        reportData.put(FirestoreFields.LAT, draft.getLocation().getLatitude());
        reportData.put(FirestoreFields.LON, draft.getLocation().getLongitude());
        if (draft.getDescription() != null && !draft.getDescription().isEmpty()) {
            reportData.put(FirestoreFields.DESCRIPTION, draft.getDescription());
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            reportData.put(FirestoreFields.IMAGE_URL, imageUrl);
        }
        reportData.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
        reportData.put(FirestoreFields.REPORTER_ID, reporterId);
        reportData.put(FirestoreFields.STATUS, ReportStatus.ACTIVE.name());
        reportData.put(FirestoreFields.RELIABILITY, ReportDefaults.INITIAL_RELIABILITY);

        long createdAt = System.currentTimeMillis();
        long expiresAt = lifecycleManager.calculateExpiration(draft.getCategory(), createdAt);
        reportData.put(FirestoreFields.EXPIRES_AT, expiresAt);

        newReportRef.set(reportData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
