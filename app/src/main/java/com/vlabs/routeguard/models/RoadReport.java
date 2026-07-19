package com.vlabs.routeguard.models;

public class RoadReport {
    private String reportId;
    private ReportType type;
    private double latitude;
    private double longitude;
    private String description;
    private long timestamp;
    private String reporterId;
    private long expiresAt;
    
    // Aggregated fields for Confidence Engine
    private int upvotes = 0;
    private int downvotes = 0;
    private int activeConfirmations = 0;
    private int goneConfirmations = 0;
    private int confidenceScore = 50;

    public RoadReport() {
        // Empty constructor for potential future Firestore usage
    }

    public RoadReport(String reportId, ReportType type, double latitude, double longitude, String description, long timestamp, long expiresAt) {
        this.reportId = reportId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.timestamp = timestamp;
        this.expiresAt = expiresAt;
    }

    public RoadReport(String reportId, ReportType type, double latitude, double longitude, String description, long timestamp, String reporterId, long expiresAt) {
        this.reportId = reportId;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.timestamp = timestamp;
        this.reporterId = reporterId;
        this.expiresAt = expiresAt;
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

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

    public int getActiveConfirmations() { return activeConfirmations; }
    public void setActiveConfirmations(int activeConfirmations) { this.activeConfirmations = activeConfirmations; }

    public int getGoneConfirmations() { return goneConfirmations; }
    public void setGoneConfirmations(int goneConfirmations) { this.goneConfirmations = goneConfirmations; }

    public int getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
}
