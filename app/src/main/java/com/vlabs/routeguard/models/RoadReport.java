package com.vlabs.routeguard.models;

public class RoadReport {
    private String reportId;
    private ReportType type;
    private double latitude;
    private double longitude;
    private String description;
    private long timestamp;
    private String reporterId;

    public RoadReport() {
        // Empty constructor for potential future Firestore usage
    }

    public RoadReport(String reportId, ReportType type, double latitude, double longitude, String description, long timestamp) {
        this.reportId = reportId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.timestamp = timestamp;
    }

    public RoadReport(String reportId, ReportType type, double latitude, double longitude, String description, long timestamp, String reporterId) {
        this.reportId = reportId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.timestamp = timestamp;
        this.reporterId = reporterId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }
}
