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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {

    private static final String TAG = "RouteGuard";
    private final FirebaseFirestore db;
    private final ReportLifecycleManager lifecycleManager;
    private ListenerRegistration reportsListener;

    public FirestoreRepository() {
        db = FirebaseFirestore.getInstance();
        lifecycleManager = new ReportLifecycleManager();
        
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
                !document.contains(FirestoreFields.LON) || !document.contains(FirestoreFields.DESCRIPTION) ||
                !document.contains(FirestoreFields.TIMESTAMP)) {
                Log.w(TAG, "Skipping document " + document.getId() + " due to missing fields.");
                return null;
            }

            String typeString = document.getString(FirestoreFields.TYPE);
            Double lat = document.getDouble(FirestoreFields.LAT);
            Double lon = document.getDouble(FirestoreFields.LON);
            String description = document.getString(FirestoreFields.DESCRIPTION);
            Long timestamp = null;
            java.util.Date date = document.getDate(FirestoreFields.TIMESTAMP, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE);
            if (date != null) {
                timestamp = date.getTime();
            } else {
                timestamp = System.currentTimeMillis();
            }

            // Additional null checks to ensure type safety
            if (typeString == null || lat == null || lon == null || description == null) {
                Log.w(TAG, "Skipping document " + document.getId() + " due to null field values.");
                return null;
            }

            String reporterId = getSafeString(document, FirestoreFields.REPORTER_ID, "");

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
            }

            if (parsedExpiresAt != 0L) {
                if (lifecycleManager.isExpired(parsedExpiresAt)) {
                    Log.d(TAG, "Skipping expired document " + document.getId());
                    return null;
                }
            } else {
                parsedExpiresAt = lifecycleManager.calculateExpiration(type, timestamp);
                if (lifecycleManager.isExpired(parsedExpiresAt)) {
                    Log.d(TAG, "Skipping expired legacy document " + document.getId());
                    return null;
                }
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

            finalReport.setUpvotes(getSafeInt(document, "upvotes", 0));
            finalReport.setDownvotes(getSafeInt(document, "downvotes", 0));
            finalReport.setActiveConfirmations(getSafeInt(document, "activeConfirmations", 0));
            finalReport.setGoneConfirmations(getSafeInt(document, "goneConfirmations", 0));

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
