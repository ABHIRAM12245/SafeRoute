package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.TrustScore;

/**
 * Domain repository for managing trust score operations.
 * Currently serves as a foundation for future data source integration.
 */
public class TrustRepository {

    private final TrustFirestoreRepository firestoreRepository;

    public TrustRepository() {
        this.firestoreRepository = new TrustFirestoreRepository();
    }

    /**
     * Saves a trust score.
     * 
     * @param trustScore The TrustScore object to save.
     * @return true if the input is valid and structurally saved, false otherwise.
     */
    public boolean saveTrustScore(TrustScore trustScore) {
        // Simple input validation
        if (trustScore == null || trustScore.getUserId() == null || trustScore.getUserId().trim().isEmpty()) {
            return false;
        }
        
        return firestoreRepository.saveTrustScore(trustScore);
    }

    public interface GetTrustScoreCallback {
        void onSuccess(TrustScore trustScore);
        void onFailure(Exception e);
    }

    public interface ReadTrustScoreCallback {
        void onSuccess(TrustScore trustScore);
        void onFailure(Exception e);
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

    /**
     * Retrieves a trust score for a specific user.
     * 
     * @param userId The ID of the user whose trust score is being retrieved.
     * @param callback The callback to return the computed TrustScore.
     */
    public void getTrustScore(String userId, GetTrustScoreCallback callback) {
        // Simple input validation
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Invalid user ID"));
            return;
        }
        
        getTrustStatistics(userId, new TrustFirestoreRepository.GetTrustStatisticsCallback() {
            @Override
            public void onSuccess(int positiveVotes, int negativeVotes, int validReports, int invalidReports) {
                com.vlabs.routeguard.managers.TrustManager trustManager = new com.vlabs.routeguard.managers.TrustManager();
                TrustScore trustScore = trustManager.calculateTrustScore(positiveVotes, negativeVotes, validReports, invalidReports);
                trustScore.setUserId(userId);
                saveTrustScore(trustScore);
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
}
