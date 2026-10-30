package com.vlabs.routeguard.repositories;

import android.content.Context;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vlabs.routeguard.models.ReportStatus;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.RoadReportDraft;
import com.vlabs.routeguard.utils.FirestoreCollections;
import com.vlabs.routeguard.utils.FirestoreFields;
import com.vlabs.routeguard.utils.ReportDefaults;
import com.vlabs.routeguard.managers.ReportLifecycleManager;
import com.vlabs.routeguard.managers.UploadManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Hazard Report Creation Repository.
 *
 * Coordinates the full persistence pipeline for new road hazard reports, spanning
 * photo upload, local device storage caching, expiration calculation, and Cloud Firestore writing.
 *
 * Asynchronous Data Flow:
 * 1. Validation: Checks required parameters (geographic coordinates, hazard category, valid reporterId).
 * 2. Media Upload: If a photograph is attached, invokes {@link UploadManager#upload(PhotoAttachment, StorageRepository.UploadCallback)}
 *    to upload the compressed image to Cloudinary REST storage. If network is offline, gracefully degrades
 *    by using the local file URI as a fallback.
 * 3. Lifecycle Calculation: Computes the hazard's expiration epoch timestamp using {@link ReportLifecycleManager#calculateExpiration(com.vlabs.routeguard.models.ReportType, long)}
 *    based on category-specific time-to-live (TTL) rules.
 * 4. Two-Tier Storage Strategy:
 *    - Step A: Immediately persists the {@link RoadReport} to {@link com.vlabs.routeguard.managers.LocalReportManager}
 *      and dispatches {@link ReportSubmissionCallback#onSuccess()} so the UI finishes instantly without waiting on network latency.
 *    - Step B: Asynchronously submits the complete field dictionary (coordinates, type, reporter email, photo URLs,
 *      initial confidence score 80) to Cloud Firestore under {@code reports/{reportId}}.
 */
public class ReportCreationRepository extends BaseRepository {

    private final Context context;
    private final FirebaseFirestore db;
    private final ReportLifecycleManager lifecycleManager;
    private final UploadManager uploadManager;

    /**
     * Callback interface reporting the outcome of a report creation operation.
     */
    public interface ReportSubmissionCallback {
        void onSuccess();
        void onError(String error);
    }

    public ReportCreationRepository(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
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
                    android.util.Log.d("ReportCreationRepository", "imageUrl before saveReport: " + downloadUrl);
                    saveReport(draft, reporterId, downloadUrl, publicId, callback);
                }

                @Override
                public void onFailure(Exception e) {
                    android.util.Log.w("ReportCreationRepository", "Photo upload failed/offline, saving with local photo reference: " + e.getMessage());
                    String localUri = draft.getPhotoAttachment().getUri() != null ? draft.getPhotoAttachment().getUri().toString() : null;
                    saveReport(draft, reporterId, localUri, null, callback);
                }
            });
        } else {
            saveReport(draft, reporterId, null, null, callback);
        }
    }

    private void saveReport(RoadReportDraft draft, String reporterId, String imageUrl, String publicId, ReportSubmissionCallback callback) {
        DocumentReference newReportRef = db.collection(FirestoreCollections.REPORTS).document();
        String reportId = newReportRef.getId();

        long createdAt = System.currentTimeMillis();
        long expiresAt = lifecycleManager.calculateExpiration(draft.getCategory(), createdAt);

        // Generate base64 thumbnail if photo attachment is present and not already a remote URL
        String embeddedPhoto = imageUrl;
        if (draft.getPhotoAttachment() != null && draft.getPhotoAttachment().getUri() != null && context != null) {
            String base64Thumbnail = encodeImageToBase64(draft.getPhotoAttachment().getUri(), context);
            if (base64Thumbnail != null && !base64Thumbnail.isEmpty()) {
                embeddedPhoto = base64Thumbnail;
            }
        }

        // 1. Construct RoadReport domain model
        RoadReport roadReport = new RoadReport(
                reportId,
                draft.getCategory(),
                draft.getLocation().getLatitude(),
                draft.getLocation().getLongitude(),
                draft.getDescription() != null ? draft.getDescription() : "",
                createdAt,
                reporterId,
                expiresAt
        );
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            roadReport.setImageUrl(imageUrl.trim());
        } else if (embeddedPhoto != null && !embeddedPhoto.trim().isEmpty()) {
            roadReport.setImageUrl(embeddedPhoto.trim());
        }
        if (publicId != null && !publicId.trim().isEmpty()) {
            roadReport.setCloudinaryPublicId(publicId.trim());
        }
        roadReport.setConfidenceScore(80);

        // 2. Persist to local file storage immediately (Zero database dependency!)
        com.vlabs.routeguard.managers.LocalReportManager.getInstance().saveReport(roadReport);
        android.util.Log.d("ReportCreationRepository", "Report saved to local storage file: " + reportId);

        // 3. Immediately dispatch success to UI!
        callback.onSuccess();

        // 4. Asynchronously attempt to sync to Cloud Firestore in the background
        Map<String, Object> reportData = new HashMap<>();
        reportData.put(FirestoreFields.REPORT_ID, reportId);
        reportData.put(FirestoreFields.TYPE, draft.getCategory().name());
        reportData.put(FirestoreFields.LAT, draft.getLocation().getLatitude());
        reportData.put(FirestoreFields.LON, draft.getLocation().getLongitude());
        if (draft.getDescription() != null && !draft.getDescription().isEmpty()) {
            reportData.put(FirestoreFields.DESCRIPTION, draft.getDescription());
        }

        // Add user email / gmail so it clearly displays in Cloud Firestore
        String userEmail = com.vlabs.routeguard.managers.SessionManager.getInstance().getEmail();
        if ((userEmail == null || userEmail.trim().isEmpty()) && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            userEmail = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getEmail();
        }
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            reportData.put("email", userEmail.trim());
            reportData.put("gmail", userEmail.trim());
            reportData.put("reporterEmail", userEmail.trim());
        }

        // Add photo / imageUrl
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            reportData.put(FirestoreFields.IMAGE_URL, imageUrl.trim());
            reportData.put("photoUrl", imageUrl.trim());
            reportData.put("photo", imageUrl.trim());
        }
        if (embeddedPhoto != null && !embeddedPhoto.trim().isEmpty()) {
            reportData.put("photo", embeddedPhoto.trim());
            if (!reportData.containsKey(FirestoreFields.IMAGE_URL)) {
                reportData.put(FirestoreFields.IMAGE_URL, embeddedPhoto.trim());
            }
        }
        if (publicId != null && !publicId.trim().isEmpty()) {
            reportData.put(FirestoreFields.CLOUDINARY_PUBLIC_ID, publicId.trim());
        }
        reportData.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
        reportData.put(FirestoreFields.REPORTER_ID, reporterId);
        reportData.put("userId", reporterId);
        reportData.put(FirestoreFields.STATUS, ReportStatus.ACTIVE.name());
        reportData.put(FirestoreFields.RELIABILITY, ReportDefaults.INITIAL_RELIABILITY);
        reportData.put(FirestoreFields.EXPIRES_AT, new com.google.firebase.Timestamp(new java.util.Date(expiresAt)));

        newReportRef.set(reportData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ReportCreationRepository", "Report synchronized to Firestore in background: " + reportId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.w("ReportCreationRepository", "Firestore background sync skipped/failed (non-fatal): " + e.getMessage());
                });
    }

    private String encodeImageToBase64(android.net.Uri uri, Context ctx) {
        if (uri == null || ctx == null) return null;
        try (java.io.InputStream is = ctx.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (bitmap == null) return null;

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float maxDimension = 320f;
            if (width > maxDimension || height > maxDimension) {
                float scale = Math.min(maxDimension / width, maxDimension / height);
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, Math.round(width * scale), Math.round(height * scale), true);
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/jpeg;base64," + android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
        } catch (Throwable t) {
            android.util.Log.w("ReportCreationRepository", "Could not encode thumbnail base64: " + t.getMessage());
            return null;
        }
    }
}
