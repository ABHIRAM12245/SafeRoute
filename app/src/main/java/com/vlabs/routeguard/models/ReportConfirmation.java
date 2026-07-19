package com.vlabs.routeguard.models;

public class ReportConfirmation {

    public enum Status {
        ACTIVE,
        GONE
    }

    private String reportId;
    private String userId;
    private Status status;
    private long timestamp;

    public ReportConfirmation() {} // Required for Firestore

    public ReportConfirmation(String reportId, String userId, Status status, long timestamp) {
        this.reportId = reportId;
        this.userId = userId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
