package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.models.RoadReport;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        firestoreRepository.fetchReports(callback);
    }

    public void fetchReportById(String reportId, SingleReportCallback callback) {
        firestoreRepository.fetchReportById(reportId, callback);
    }

    public void startRoadReportsListener(ReportCallback callback) {
        firestoreRepository.startReportsListener(callback);
    }

    public void stopRoadReportsListener() {
        firestoreRepository.stopReportsListener();
    }
}
