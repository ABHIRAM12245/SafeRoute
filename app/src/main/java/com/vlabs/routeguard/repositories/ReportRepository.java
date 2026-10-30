package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.managers.LocalReportManager;
import com.vlabs.routeguard.models.RoadReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportRepository extends BaseRepository {

    public interface ReportCallback {
        void onSuccess(List<RoadReport> reports);
        void onError(String error);
    }

    public interface SingleReportCallback {
        void onSuccess(RoadReport report);
        void onError(String error);
    }

    private final FirestoreRepository firestoreRepository;

    public ReportRepository() {
        this.firestoreRepository = new FirestoreRepository();
    }

    public void fetchRoadReports(ReportCallback callback) {
        firestoreRepository.fetchReports(new ReportCallback() {
            @Override
            public void onSuccess(List<RoadReport> reports) {
                callback.onSuccess(mergeWithLocalReports(reports));
            }

            @Override
            public void onError(String error) {
                // If Firestore fails (e.g. permission or network), fallback to local reports!
                List<RoadReport> localOnly = LocalReportManager.getInstance().getAllReports();
                callback.onSuccess(localOnly);
            }
        });
    }

    public void fetchUserReports(String userId, ReportCallback callback) {
        firestoreRepository.fetchUserReports(userId, new ReportCallback() {
            @Override
            public void onSuccess(List<RoadReport> reports) {
                callback.onSuccess(mergeWithLocalUserReports(reports, userId));
            }

            @Override
            public void onError(String error) {
                // If Firestore fails, fallback to local reports created by user!
                List<RoadReport> localOnly = LocalReportManager.getInstance().getReportsByUserId(userId);
                callback.onSuccess(localOnly);
            }
        });
    }

    public void fetchReportById(String reportId, SingleReportCallback callback) {
        RoadReport local = LocalReportManager.getInstance().getReportById(reportId);
        if (local != null) {
            callback.onSuccess(local);
            return;
        }

        firestoreRepository.fetchReportById(reportId, callback);
    }

    public void startRoadReportsListener(ReportCallback callback) {
        firestoreRepository.startReportsListener(new ReportCallback() {
            @Override
            public void onSuccess(List<RoadReport> reports) {
                callback.onSuccess(mergeWithLocalReports(reports));
            }

            @Override
            public void onError(String error) {
                // If Firestore listener fails, emit local reports
                List<RoadReport> localOnly = LocalReportManager.getInstance().getAllReports();
                callback.onSuccess(localOnly);
            }
        });
    }

    public void stopRoadReportsListener() {
        firestoreRepository.stopReportsListener();
    }

    private List<RoadReport> mergeWithLocalReports(List<RoadReport> remoteReports) {
        Map<String, RoadReport> map = new HashMap<>();

        if (remoteReports != null) {
            for (RoadReport r : remoteReports) {
                if (r != null && r.getReportId() != null) {
                    map.put(r.getReportId(), r);
                }
            }
        }

        List<RoadReport> localReports = LocalReportManager.getInstance().getAllReports();
        if (localReports != null) {
            for (RoadReport r : localReports) {
                if (r != null && r.getReportId() != null) {
                    map.put(r.getReportId(), r);
                }
            }
        }

        List<RoadReport> result = new ArrayList<>(map.values());
        result.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
        return result;
    }

    private List<RoadReport> mergeWithLocalUserReports(List<RoadReport> remoteReports, String userId) {
        Map<String, RoadReport> map = new HashMap<>();

        if (remoteReports != null) {
            for (RoadReport r : remoteReports) {
                if (r != null && r.getReportId() != null) {
                    map.put(r.getReportId(), r);
                }
            }
        }

        List<RoadReport> localReports = LocalReportManager.getInstance().getReportsByUserId(userId);
        if (localReports != null) {
            for (RoadReport r : localReports) {
                if (r != null && r.getReportId() != null) {
                    map.put(r.getReportId(), r);
                }
            }
        }

        List<RoadReport> result = new ArrayList<>(map.values());
        result.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));
        return result;
    }
}
