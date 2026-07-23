package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.managers.TrustManager;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.TrustScore;
import com.vlabs.routeguard.models.VerificationStatus;
import com.vlabs.routeguard.utils.TrustRules;

public class TrustRepository {

    private final TrustFirestoreRepository firestoreRepository;
    private final TrustManager trustManager;

    public interface GetTrustScoreCallback {
        void onSuccess(TrustScore trustScore);
        void onFailure(Exception e);
    }

    public interface ReadTrustScoreCallback {
        void onSuccess(TrustScore trustScore);
        void onFailure(Exception e);
    }

    public interface VoteSubmissionCallback {
        void onSuccess(VerificationStatus newVerificationStatus);
        void onError(String errorMsg);
    }

    public TrustRepository() {
        this.firestoreRepository = new TrustFirestoreRepository();
        this.trustManager = new TrustManager();
    }

    public boolean saveTrustScore(TrustScore trustScore) {
        if (trustScore == null || trustScore.getUserId() == null || trustScore.getUserId().trim().isEmpty()) {
            return false;
        }
        return firestoreRepository.saveTrustScore(trustScore);
    }

    public void readTrustScore(String userId, ReadTrustScoreCallback callback) {
        firestoreRepository.readTrustScore(userId, new TrustFirestoreRepository.ReadTrustScoreCallback() {
            @Override
            public void onSuccess(TrustScore trustScore) {
                callback.onSuccess(trustScore);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void getTrustScore(String userId, GetTrustScoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Invalid user ID"));
            return;
        }

        getTrustStatistics(userId, new TrustFirestoreRepository.GetTrustStatisticsCallback() {
            @Override
            public void onSuccess(int positiveVotes, int negativeVotes, int validReports, int invalidReports) {
                TrustScore trustScore = trustManager.calculateTrustScore(positiveVotes, negativeVotes, validReports, invalidReports);
                trustScore.setUserId(userId);
                saveTrustScore(trustScore);

                String level = trustManager.calculateUserLevel(trustScore.getScore());
                firestoreRepository.updateUserProfileTrust(userId, trustScore.getScore(), level, null);

                callback.onSuccess(trustScore);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void getTrustStatistics(String userId, TrustFirestoreRepository.GetTrustStatisticsCallback callback) {
        firestoreRepository.getTrustStatistics(userId, callback);
    }

    /**
     * Processes a vote on a report while enforcing self-voting checks and updating report verification and reporter trust.
     */
    public void processReportVote(RoadReport report, String currentUserId, boolean isStillExistsVote, VoteSubmissionCallback callback) {
        if (report == null || report.getReportId() == null) {
            callback.onError("Invalid report.");
            return;
        }

        // Protection against self-voting in Repository layer (Refinement #4)
        if (!trustManager.canUserVote(currentUserId, report.getReporterId())) {
            callback.onError("You cannot vote on your own report.");
            return;
        }

        int currentPos = report.getUpvotes() + (isStillExistsVote ? 1 : 0);
        int currentNeg = report.getDownvotes() + (!isStillExistsVote ? 1 : 0);

        VerificationStatus newStatus = trustManager.calculateReportVerificationStatus(report, currentPos, currentNeg);
        report.setVerificationStatus(newStatus);

        firestoreRepository.updateReportVerificationStatus(report.getReportId(), newStatus, new TrustFirestoreRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Update creator trust score if reporterId exists
                if (report.getReporterId() != null && !report.getReporterId().isEmpty()) {
                    recalculateUserTrust(report.getReporterId());
                }
                callback.onSuccess(newStatus);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onError("Failed to update report verification status: " + e.getMessage());
            }
        });
    }

    public void recalculateUserTrust(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;
        getTrustScore(userId, new GetTrustScoreCallback() {
            @Override
            public void onSuccess(TrustScore trustScore) {}
            @Override
            public void onFailure(Exception e) {}
        });
    }
}
