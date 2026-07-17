package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.TrustScore;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Firestore data layer for managing trust score operations.
 * Currently serves as a foundation for future remote database integration.
 */
public class TrustFirestoreRepository {

    private final FirebaseFirestore db;

    public TrustFirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Prepares to save a trust score to the remote database.
     * 
     * @param trustScore The TrustScore object to save.
     * @return true if the input is valid (stub successful save), false otherwise.
     */
    public boolean saveTrustScore(TrustScore trustScore) {
        // Simple input validation
        if (trustScore == null || trustScore.getUserId() == null || trustScore.getUserId().trim().isEmpty()) {
            return false;
        }
        
        try {
            db.collection("trust_scores")
              .document(trustScore.getUserId())
              .set(trustScore);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Prepares to retrieve a trust score for a specific user from the remote database.
     * 
     * @param userId The ID of the user whose trust score is being retrieved.
     * @return A stub TrustScore, or null if the user ID is invalid.
     */
    public TrustScore getTrustScore(String userId) {
        // Simple input validation
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        
        // TODO: Implement actual Firestore retrieval API logic here.
        // Returning a stub value for now.
        return new TrustScore(userId, 100, System.currentTimeMillis());
    }

    public interface GetTrustStatisticsCallback {
        void onSuccess(int positiveVotes, int negativeVotes, int validReports, int invalidReports);
        void onFailure(Exception e);
    }

    public interface ReadTrustScoreCallback {
        void onSuccess(TrustScore trustScore);
        void onFailure(Exception e);
    }

    public void readTrustScore(String userId, ReadTrustScoreCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Invalid userId"));
            return;
        }
        
        db.collection("trust_scores").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        TrustScore score = documentSnapshot.toObject(TrustScore.class);
                        callback.onSuccess(score);
                    } else {
                        callback.onFailure(new Exception("Trust score not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getTrustStatistics(String userId, GetTrustStatisticsCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Invalid userId"));
            return;
        }

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> votesTask = db.collectionGroup("votes")
                .whereEqualTo("reportOwnerId", userId)
                .get();

        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> reportsTask = db.collection("reports")
                .whereEqualTo("reporterId", userId)
                .get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(votesTask, reportsTask).addOnCompleteListener(task -> {
            int positiveVotes = 0;
            int negativeVotes = 0;
            int validReports = 0;
            int invalidReports = 0;

            if (votesTask.isSuccessful() && votesTask.getResult() != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : votesTask.getResult().getDocuments()) {
                    String voteType = doc.getString("voteType");
                    if ("UPVOTE".equals(voteType)) {
                        positiveVotes++;
                    } else if ("DOWNVOTE".equals(voteType)) {
                        negativeVotes++;
                    }
                }
            }

            if (reportsTask.isSuccessful() && reportsTask.getResult() != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : reportsTask.getResult().getDocuments()) {
                    String status = doc.getString("status");
                    if ("RESOLVED".equals(status)) {
                        validReports++;
                    } else if ("DISMISSED".equals(status)) {
                        invalidReports++;
                    }
                }
            }

            callback.onSuccess(positiveVotes, negativeVotes, validReports, invalidReports);
        });
    }
}
