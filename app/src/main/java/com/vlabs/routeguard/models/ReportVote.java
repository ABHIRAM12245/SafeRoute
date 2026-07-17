package com.vlabs.routeguard.models;

import com.vlabs.routeguard.managers.VotingManager.VoteType;

/**
 * Data model representing a user's vote on a report.
 */
public class ReportVote {

    private String voteId;
    private String reportId;
    private String userId;
    private VoteType voteType;
    private long timestamp;
    private String reportOwnerId;

    public ReportVote() {
        // Default constructor required for serialization (e.g., Firestore)
    }

    public ReportVote(String voteId, String reportId, String userId, VoteType voteType, long timestamp) {
        this.voteId = voteId;
        this.reportId = reportId;
        this.userId = userId;
        this.voteType = voteType;
        this.timestamp = timestamp;
    }

    public ReportVote(String voteId, String reportId, String userId, VoteType voteType, long timestamp, String reportOwnerId) {
        this.voteId = voteId;
        this.reportId = reportId;
        this.userId = userId;
        this.voteType = voteType;
        this.timestamp = timestamp;
        this.reportOwnerId = reportOwnerId;
    }

    public String getVoteId() {
        return voteId;
    }

    public void setVoteId(String voteId) {
        this.voteId = voteId;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(VoteType voteType) {
        this.voteType = voteType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReportOwnerId() {
        return reportOwnerId;
    }

    public void setReportOwnerId(String reportOwnerId) {
        this.reportOwnerId = reportOwnerId;
    }
}
