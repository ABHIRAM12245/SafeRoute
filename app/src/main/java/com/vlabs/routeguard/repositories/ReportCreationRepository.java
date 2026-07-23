package com.vlabs.routeguard.repositories;

import android.content.Context;

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

    public ReportCreationRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.lifecycleManager = new ReportLifecycleManager();
        this.uploadManager = new UploadManager(context);
    }

    public void submitReport(RoadReportDraft draft, String reporterId, ReportSubmissionCallback callback) {
        if (draft.getLocation() == null || draft.getCategory() == null || reporterId == null || reporterId.isEmpty()) {
            callback.onError("Validation failed: missing required fields.");
            return;
        }

        if (draft.getPhotoAttachment() != null) {
            uploadManager.upload(draft.getPhotoAttachment(), new StorageRepository.UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl, String publicId) {
                    android.util.Log.d("ReportCreationRepository", "imageUrl before saveToFirestore: " + downloadUrl);
                    saveToFirestore(draft, reporterId, downloadUrl, publicId, callback);
                }

                @Override
                public void onFailure(Exception e) {
                    callback.onError("Upload failed: " + e.getMessage());
                }
            });
        } else {
            saveToFirestore(draft, reporterId, null, null, callback);
        }
    }

    private void saveToFirestore(RoadReportDraft draft, String reporterId, String imageUrl, String publicId, ReportSubmissionCallback callback) {
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

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            android.util.Log.d("ReportCreationRepository", "imageUrl=" + imageUrl);
            reportData.put(FirestoreFields.IMAGE_URL, imageUrl.trim());
        }
        if (publicId != null && !publicId.trim().isEmpty()) {
            reportData.put(FirestoreFields.CLOUDINARY_PUBLIC_ID, publicId.trim());
        }
        reportData.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
        reportData.put(FirestoreFields.REPORTER_ID, reporterId);
        reportData.put(FirestoreFields.STATUS, ReportStatus.ACTIVE.name());
        reportData.put(FirestoreFields.RELIABILITY, ReportDefaults.INITIAL_RELIABILITY);

        long createdAt = System.currentTimeMillis();
        long expiresAt = lifecycleManager.calculateExpiration(draft.getCategory(), createdAt);
        reportData.put(FirestoreFields.EXPIRES_AT, new com.google.firebase.Timestamp(new java.util.Date(expiresAt)));

        newReportRef.set(reportData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ReportCreationRepository", "Document written successfully. imageUrl field check: " + reportData.get(FirestoreFields.IMAGE_URL));
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
