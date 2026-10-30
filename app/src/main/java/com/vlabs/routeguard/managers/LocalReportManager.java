package com.vlabs.routeguard.managers;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.VerificationStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages file-based road reports stored directly on device storage.
 * Guarantees that reports can be submitted, saved, and retrieved locally
 * without depending on external database permissions.
 */
public class LocalReportManager {

    private static final String TAG = "LocalReportManager";
    private static final String REPORTS_FILE_NAME = "local_reports.json";

    private static LocalReportManager instance;

    private LocalReportManager() {
    }

    public static synchronized LocalReportManager getInstance() {
        if (instance == null) {
            instance = new LocalReportManager();
        }
        return instance;
    }

    private Context getContext() {
        try {
            return FirebaseApp.getInstance().getApplicationContext();
        } catch (Exception e) {
            Log.w(TAG, "Context unavailable from FirebaseApp: " + e.getMessage());
            return null;
        }
    }

    /**
     * Saves or updates a RoadReport in the local file.
     *
     * @param report The road report to persist.
     */
    public synchronized void saveReport(RoadReport report) {
        if (report == null || report.getReportId() == null || report.getReportId().trim().isEmpty()) {
            return;
        }

        List<RoadReport> reports = loadReportsFromFile();
        int existingIndex = -1;
        for (int i = 0; i < reports.size(); i++) {
            if (report.getReportId().equals(reports.get(i).getReportId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            reports.set(existingIndex, report);
        } else {
            reports.add(0, report); // Add at top
        }

        saveReportsToFile(reports);
        Log.d(TAG, "Saved report locally: " + report.getReportId() + " (" + report.getType() + ")");
    }

    /**
     * Retrieves all locally stored reports.
     */
    public synchronized List<RoadReport> getAllReports() {
        return loadReportsFromFile();
    }

    /**
     * Retrieves all locally stored reports created by a specific user.
     */
    public synchronized List<RoadReport> getReportsByUserId(String userId) {
        List<RoadReport> userReports = new ArrayList<>();
        if (userId == null || userId.trim().isEmpty()) return userReports;

        for (RoadReport r : loadReportsFromFile()) {
            if (userId.equals(r.getReporterId())) {
                userReports.add(r);
            }
        }
        return userReports;
    }

    /**
     * Finds a locally stored report by ID.
     */
    public synchronized RoadReport getReportById(String reportId) {
        if (reportId == null || reportId.trim().isEmpty()) return null;

        for (RoadReport r : loadReportsFromFile()) {
            if (reportId.equals(r.getReportId())) {
                return r;
            }
        }
        return null;
    }

    /**
     * Deletes a report from the local file.
     */
    public synchronized boolean deleteReport(String reportId) {
        if (reportId == null || reportId.trim().isEmpty()) return false;

        List<RoadReport> reports = loadReportsFromFile();
        boolean removed = reports.removeIf(r -> reportId.equals(r.getReportId()));
        if (removed) {
            saveReportsToFile(reports);
            Log.d(TAG, "Deleted report locally: " + reportId);
        }
        return removed;
    }

    public static JSONObject reportToJson(RoadReport report) {
        JSONObject json = new JSONObject();
        if (report == null) return json;
        try {
            json.put("reportId", report.getReportId());
            if (report.getType() != null) {
                json.put("type", report.getType().name());
            }
            json.put("latitude", report.getLatitude());
            json.put("longitude", report.getLongitude());
            json.put("description", report.getDescription() != null ? report.getDescription() : "");
            json.put("timestamp", report.getTimestamp());
            json.put("reporterId", report.getReporterId() != null ? report.getReporterId() : "");
            json.put("expiresAt", report.getExpiresAt());
            json.put("imageUrl", report.getImageUrl() != null ? report.getImageUrl() : "");
            json.put("cloudinaryPublicId", report.getCloudinaryPublicId() != null ? report.getCloudinaryPublicId() : "");
            json.put("confidenceScore", report.getConfidenceScore());
            json.put("upvotes", report.getUpvotes());
            json.put("downvotes", report.getDownvotes());
            json.put("activeConfirmations", report.getActiveConfirmations());
            json.put("goneConfirmations", report.getGoneConfirmations());
            json.put("commentCount", report.getCommentCount());
            if (report.getVerificationStatus() != null) {
                json.put("verificationStatus", report.getVerificationStatus().name());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error serializing report to JSON: " + e.getMessage());
        }
        return json;
    }

    public static RoadReport reportFromJson(JSONObject json) {
        if (json == null) return null;
        try {
            String reportId = json.optString("reportId");
            if (reportId.isEmpty()) return null;

            ReportType type = ReportType.OTHER;
            try {
                type = ReportType.valueOf(json.optString("type", "OTHER"));
            } catch (Exception ignored) {
            }

            double lat = json.optDouble("latitude", 0.0);
            double lon = json.optDouble("longitude", 0.0);
            String desc = json.optString("description", "");
            long ts = json.optLong("timestamp", System.currentTimeMillis());
            String reporterId = json.optString("reporterId", "");
            long exp = json.optLong("expiresAt", ts + (2 * 60 * 60 * 1000));

            RoadReport report = new RoadReport(reportId, type, lat, lon, desc, ts, reporterId, exp);
            report.setImageUrl(json.optString("imageUrl", ""));
            report.setCloudinaryPublicId(json.optString("cloudinaryPublicId", ""));
            report.setConfidenceScore(json.optInt("confidenceScore", 50));
            report.setUpvotes(json.optInt("upvotes", 0));
            report.setDownvotes(json.optInt("downvotes", 0));
            report.setActiveConfirmations(json.optInt("activeConfirmations", 0));
            report.setGoneConfirmations(json.optInt("goneConfirmations", 0));
            report.setCommentCount(json.optInt("commentCount", 0));

            String statusStr = json.optString("verificationStatus", "UNVERIFIED");
            try {
                report.setVerificationStatus(VerificationStatus.valueOf(statusStr));
            } catch (Exception ignored) {
            }

            return report;
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing report from JSON: " + e.getMessage());
            return null;
        }
    }

    private List<RoadReport> loadReportsFromFile() {
        List<RoadReport> list = new ArrayList<>();
        Context ctx = getContext();
        if (ctx == null) return list;

        File file = new File(ctx.getFilesDir(), REPORTS_FILE_NAME);
        if (!file.exists()) return list;

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                RoadReport r = reportFromJson(array.getJSONObject(i));
                if (r != null) {
                    list.add(r);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading local reports file: " + e.getMessage());
        }

        return list;
    }

    private void saveReportsToFile(List<RoadReport> reports) {
        Context ctx = getContext();
        if (ctx == null) return;

        File file = new File(ctx.getFilesDir(), REPORTS_FILE_NAME);
        try {
            JSONArray array = new JSONArray();
            for (RoadReport r : reports) {
                array.put(reportToJson(r));
            }
            try (FileOutputStream fos = new FileOutputStream(file, false)) {
                fos.write(array.toString(2).getBytes(StandardCharsets.UTF_8));
                fos.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving local reports file: " + e.getMessage());
        }
    }
}
