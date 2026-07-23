package com.vlabs.routeguard.repositories;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.utils.FirestoreCollections;
import com.vlabs.routeguard.utils.FirestoreFields;
import com.vlabs.routeguard.managers.ReportLifecycleManager;
import com.vlabs.routeguard.managers.CascadeCleanupManager;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {

    private static final String TAG = "RouteGuard";
    private final FirebaseFirestore db;
    private final ReportLifecycleManager lifecycleManager;
    private final CascadeCleanupManager cleanupManager;
    private ListenerRegistration reportsListener;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        lifecycleManager = new ReportLifecycleManager();
        cleanupManager = new CascadeCleanupManager();
        
        // Configure offline persistence
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }

    // Future overload: public void fetchReports(int limit, ReportRepository.ReportCallback callback) {}
    // Future overload: public void fetchReports(int limit, DocumentSnapshot startAfter, ReportRepository.ReportCallback callback) {}

    public void fetchReports(ReportRepository.ReportCallback callback) {
        Log.d(TAG, "Fetching reports from Firestore...");
        db.collection(FirestoreCollections.REPORTS)
                .orderBy(FirestoreFields.TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RoadReport> reports = new ArrayList<>();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            RoadReport report = mapDocumentToReport(document);
                            if (report != null) {
                                reports.add(report);
                            }
                        }
                    } else {
                        Log.d(TAG, "Reports collection is empty.");
                    }
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching reports", e);
                    callback.onError("Failed to load reports. Please check your connection.");
                });
    }

    public void fetchUserReports(String userId, ReportRepository.ReportCallback callback) {
        Log.d(TAG, "Fetching reports for user " + userId + "...");
        db.collection(FirestoreCollections.REPORTS)
                .whereEqualTo(FirestoreFields.REPORTER_ID, userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<RoadReport> reports = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        Log.d("MY_REPORTS_TRACE", "Firestore returned " + queryDocumentSnapshots.size() + " documents for user " + userId);
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            RoadReport report = mapDocumentToReport(document);
                            if (report != null) {
                                reports.add(report);
                            }
                        }
                        // Sort in-memory by timestamp descending
                        java.util.Collections.sort(reports, (r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
                    } else {
                        Log.d("MY_REPORTS_TRACE", "User reports collection returned 0 documents for user " + userId);
                    }
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user reports", e);
                    callback.onError("Failed to load your reports. Please check your connection.");
                });
    }

    public void fetchReportById(String reportId, ReportRepository.SingleReportCallback callback) {
        db.collection(FirestoreCollections.REPORTS).document(reportId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    RoadReport report = mapDocumentToReport(documentSnapshot);
                    if (report != null) {
                        callback.onSuccess(report);
                    } else {
                        callback.onError("Report not found or invalid.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching report " + reportId, e);
                    callback.onError("Failed to load report. Please check your connection.");
                });
    }

    public void startReportsListener(ReportRepository.ReportCallback callback) {
        if (reportsListener != null) {
            reportsListener.remove();
        }
        
        Log.d(TAG, "Starting reports listener...");
        reportsListener = db.collection(FirestoreCollections.REPORTS)
                .orderBy(FirestoreFields.TIMESTAMP, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for reports", error);
                        callback.onError("Failed to listen for reports. Please check your connection.");
                        return;
                    }

                    List<RoadReport> reports = new ArrayList<>();
                    if (value != null && !value.isEmpty()) {
                        for (DocumentSnapshot document : value.getDocuments()) {
                            RoadReport report = mapDocumentToReport(document);
                            if (report != null) {
                                reports.add(report);
                            }
                        }
                    } else {
                        Log.d(TAG, "Reports collection is empty (listener).");
                    }
                    callback.onSuccess(reports);
                });
    }

    public void stopReportsListener() {
        if (reportsListener != null) {
            Log.d(TAG, "Stopping reports listener...");
            reportsListener.remove();
            reportsListener = null;
        }
    }

    private RoadReport mapDocumentToReport(DocumentSnapshot document) {
        try {
            if (!document.exists()) return null;

            // Validate required fields
            if (!document.contains(FirestoreFields.TYPE) || !document.contains(FirestoreFields.LAT) ||
                !document.contains(FirestoreFields.LON)) {
                Log.w(TAG, "Skipping document " + document.getId() + " due to missing fields.");
                return null;
            }

            String typeString = document.getString(FirestoreFields.TYPE);
            Double lat = document.getDouble(FirestoreFields.LAT);
            Double lon = document.getDouble(FirestoreFields.LON);
            String description = getSafeString(document, FirestoreFields.DESCRIPTION, "");
            Long timestamp = null;
            Object tsObj = document.get(FirestoreFields.TIMESTAMP);
            if (tsObj instanceof com.google.firebase.Timestamp) {
                timestamp = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
            } else if (tsObj instanceof Long) {
                timestamp = (Long) tsObj;
            } else if (tsObj instanceof Double) {
                timestamp = ((Double) tsObj).longValue();
            } else if (tsObj instanceof Number) {
                timestamp = ((Number) tsObj).longValue();
            } else {
                java.util.Date date = document.getDate(FirestoreFields.TIMESTAMP, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
                if (date != null) {
                    timestamp = date.getTime();
                } else {
                    timestamp = 0L;
                }
            }

            // Additional null checks to ensure type safety
            if (typeString == null || lat == null || lon == null) {
                Log.w(TAG, "Skipping document " + document.getId() + " due to null field values.");
                return null;
            }

            String reporterId = getSafeString(document, FirestoreFields.REPORTER_ID, "");
            if (reporterId.isEmpty()) {
                reporterId = getSafeString(document, "userId", "");
            }
            if (reporterId.isEmpty()) {
                reporterId = getSafeString(document, "reporter_id", "");
            }

            ReportType type;
            try {
                type = ReportType.valueOf(typeString);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Skipping document " + document.getId() + " due to invalid ReportType: " + typeString);
                return null;
            }

            long parsedExpiresAt = 0L;
            Object expiresAtObj = document.get(FirestoreFields.EXPIRES_AT);
            if (expiresAtObj instanceof com.google.firebase.Timestamp) {
                parsedExpiresAt = ((com.google.firebase.Timestamp) expiresAtObj).toDate().getTime();
            } else if (expiresAtObj instanceof Long) {
                parsedExpiresAt = (Long) expiresAtObj;
            } else if (expiresAtObj instanceof Double) {
                parsedExpiresAt = ((Double) expiresAtObj).longValue();
            } else if (expiresAtObj instanceof Number) {
                parsedExpiresAt = ((Number) expiresAtObj).longValue();
            }

            if (parsedExpiresAt <= 0L && timestamp != null && timestamp > 0L) {
                parsedExpiresAt = lifecycleManager.calculateExpiration(type, timestamp);
            }

            long now = System.currentTimeMillis();
            boolean isExpired = (parsedExpiresAt > 0L && now >= parsedExpiresAt);

            Log.d("LIFECYCLE_EXPIRY_TRACE", "========================================");
            Log.d("LIFECYCLE_EXPIRY_TRACE", "Report ID: " + document.getId());
            Log.d("LIFECYCLE_EXPIRY_TRACE", "Report Type: " + type);
            Log.d("LIFECYCLE_EXPIRY_TRACE", "createdAt (timestamp): " + timestamp);
            Log.d("LIFECYCLE_EXPIRY_TRACE", "expiresAt: " + parsedExpiresAt);
            Log.d("LIFECYCLE_EXPIRY_TRACE", "currentTime: " + now);
            Log.d("LIFECYCLE_EXPIRY_TRACE", "isExpired: " + isExpired);
            Log.d("LIFECYCLE_EXPIRY_TRACE", "========================================");

            if (isExpired) {
                Log.d(TAG, "Document " + document.getId() + " is EXPIRED! Triggering immediate cascade deletion.");
                RoadReport stub = new RoadReport();
                stub.setReportId(document.getId());
                cleanupManager.cleanupExpiredReport(stub);
                return null;
            }

            RoadReport finalReport = new RoadReport(
                    document.getId(),
                    type,
                    lat,
                    lon,
                    description,
                    timestamp,
                    reporterId,
                    parsedExpiresAt
            );
            String imageUrl = null;
            Object imgObj = document.get(FirestoreFields.IMAGE_URL);
            if (imgObj instanceof String) {
                imageUrl = (String) imgObj;
            }
            finalReport.setImageUrl(imageUrl);

            String publicId = getSafeString(document, FirestoreFields.CLOUDINARY_PUBLIC_ID, null);
            if (publicId != null) {
                finalReport.setCloudinaryPublicId(publicId);
            }

            finalReport.setUpvotes(getSafeInt(document, "upvotes", 0));
            finalReport.setDownvotes(getSafeInt(document, "downvotes", 0));
            finalReport.setActiveConfirmations(getSafeInt(document, "activeConfirmations", 0));
            finalReport.setGoneConfirmations(getSafeInt(document, "goneConfirmations", 0));
            finalReport.setCommentCount(getSafeInt(document, FirestoreFields.COMMENT_COUNT, 0));

            String vStatusStr = getSafeString(document, "verificationStatus", null);
            if (vStatusStr != null) {
                try {
                    finalReport.setVerificationStatus(com.vlabs.routeguard.models.VerificationStatus.valueOf(vStatusStr));
                } catch (Exception ignored) {}
            }

            // Calculate confidence on the fly
            finalReport.setConfidenceScore(com.vlabs.routeguard.managers.ConfidenceEngine.calculateConfidence(finalReport));

            return finalReport;

        } catch (Exception e) {
            Log.e(TAG, "Error parsing document " + document.getId(), e);
            return null; // Skip invalid documents
        }
    }

    private int getSafeInt(DocumentSnapshot document, String field, int defaultValue) {
        if (document.contains(field) && document.get(field) != null) {
            Long val = document.getLong(field);
            return val != null ? val.intValue() : defaultValue;
        }
        return defaultValue;
    }

    private long getSafeLong(DocumentSnapshot document, String field, long defaultValue) {
        if (document.contains(field) && document.get(field) != null) {
            Long val = document.getLong(field);
            return val != null ? val : defaultValue;
        }
        return defaultValue;
    }

    private double getSafeDouble(DocumentSnapshot document, String field, double defaultValue) {
        if (document.contains(field) && document.get(field) != null) {
            Double val = document.getDouble(field);
            return val != null ? val : defaultValue;
        }
        return defaultValue;
    }

    private boolean getSafeBoolean(DocumentSnapshot document, String field, boolean defaultValue) {
        if (document.contains(field) && document.get(field) != null) {
            Boolean val = document.getBoolean(field);
            return val != null ? val : defaultValue;
        }
        return defaultValue;
    }

    private String getSafeString(DocumentSnapshot document, String field, String defaultValue) {
        if (document.contains(field) && document.get(field) != null) {
            String val = document.getString(field);
            return val != null ? val : defaultValue;
        }
        return defaultValue;
    }
}
