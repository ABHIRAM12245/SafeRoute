package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.TrustScore;
import com.vlabs.routeguard.models.VerificationStatus;
import com.vlabs.routeguard.utils.TrustRules;

public class TrustManager {

    private int currentTrustScore;

    public TrustManager() {
        this.currentTrustScore = TrustRules.DEFAULT_TRUST_SCORE;
    }

    /**
     * Validates whether a user can vote on a specific report.
     * Prevents self-voting and invalid IDs.
     */
    public boolean canUserVote(String currentUserId, String reportOwnerId) {
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            return false;
        }
        if (reportOwnerId != null && reportOwnerId.trim().equals(currentUserId.trim())) {
            return false; // Prevent self-voting
        }
        return true;
    }

    /**
     * Calculates the verification status for a road report.
     */
    public VerificationStatus calculateReportVerificationStatus(RoadReport report, int positiveVotes, int negativeVotes) {
        if (report == null) {
            return VerificationStatus.UNVERIFIED;
        }

        // Expiration check
        long now = System.currentTimeMillis();
        if (report.getExpiresAt() > 0 && report.getExpiresAt() <= now) {
            return VerificationStatus.EXPIRED;
        }

        int netVotes = positiveVotes - negativeVotes;
        if (netVotes >= TrustRules.VERIFIED_VOTE_THRESHOLD) {
            return VerificationStatus.VERIFIED;
        } else if (negativeVotes - positiveVotes >= TrustRules.DISPUTED_VOTE_THRESHOLD) {
            return VerificationStatus.DISPUTED;
        }

        return VerificationStatus.UNVERIFIED;
    }

    /**
     * Calculates user trust score based on user activity metrics using TrustRules constants.
     */
    public TrustScore calculateTrustScore(int positiveVotes, int negativeVotes, int validReports, int invalidReports) {
        int score = TrustRules.DEFAULT_TRUST_SCORE
                + (positiveVotes * TrustRules.UPVOTE_WEIGHT)
                - (negativeVotes * TrustRules.DOWNVOTE_WEIGHT)
                + (validReports * TrustRules.VERIFIED_REPORT_BONUS)
                - (invalidReports * TrustRules.DISPUTED_REPORT_PENALTY);

        score = Math.max(TrustRules.MIN_TRUST_SCORE, Math.min(TrustRules.MAX_TRUST_SCORE, score));
        this.currentTrustScore = score;

        return new TrustScore(null, score, System.currentTimeMillis());
    }

    /**
     * Computes the user level/rank string from a given trust score.
     */
    public String calculateUserLevel(int trustScore) {
        if (trustScore <= TrustRules.THRESHOLD_NOVICE_MAX) {
            return TrustRules.LEVEL_NOVICE;
        } else if (trustScore <= TrustRules.THRESHOLD_CONTRIBUTOR_MAX) {
            return TrustRules.LEVEL_CONTRIBUTOR;
        } else if (trustScore <= TrustRules.THRESHOLD_TRUSTED_MAX) {
            return TrustRules.LEVEL_TRUSTED;
        } else {
            return TrustRules.LEVEL_MASTER;
        }
    }

    public int getTrustScore() {
        return currentTrustScore;
    }
}
