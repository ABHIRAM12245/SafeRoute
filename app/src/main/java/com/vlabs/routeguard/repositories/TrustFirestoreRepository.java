package com.vlabs.routeguard.repositories;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.vlabs.routeguard.models.TrustScore;
import com.vlabs.routeguard.models.VerificationStatus;
import com.vlabs.routeguard.utils.FirestoreCollections;

import java.util.HashMap;
import java.util.Map;

public class TrustFirestoreRepository {

    private static final String TAG = "TrustFirestoreRepo";
    private final FirebaseFirestore db;

    public interface GetTrustStatisticsCallback {
        void onSuccess(int positiveVotes, int negativeVotes, int validReports, int invalidReports);
        void onFailure(Exception e);
    }

    public interface ReadTrustScoreCallback {
        void onSuccess(TrustScore trustScore);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public TrustFirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public boolean saveTrustScore(TrustScore trustScore) {
        if (trustScore == null || trustScore.getUserId() == null || trustScore.getUserId().trim().isEmpty()) {
            return false;
        }

        try {
            db.collection("trust_scores")
                    .document(trustScore.getUserId())
                    .set(trustScore);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving trust score", e);
            return false;
        }
    }

    public void updateUserProfileTrust(String userId, int trustScore, String userLevel, SimpleCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("Invalid user ID"));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("trustScore", trustScore);
        if (userLevel != null) {
            updates.put("userLevel", userLevel);
        }

        db.collection("users").document(userId.trim())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile trust fields", e);
                    if (callback != null) callback.onFailure(e);
                });
    }

    public void updateReportVerificationStatus(String reportId, VerificationStatus status, SimpleCallback callback) {
        if (reportId == null || reportId.trim().isEmpty() || status == null) {
            if (callback != null) callback.onFailure(new IllegalArgumentException("Invalid parameters"));
            return;
        }

        db.collection(FirestoreCollections.REPORTS).document(reportId.trim())
                .update("verificationStatus", status.name())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating report verification status", e);
                    if (callback != null) callback.onFailure(e);
                });
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

        com.google.android.gms.tasks.Task<QuerySnapshot> votesTask = db.collectionGroup("votes")
                .whereEqualTo("reportOwnerId", userId)
                .get();

        com.google.android.gms.tasks.Task<QuerySnapshot> reportsTask = db.collection(FirestoreCollections.REPORTS)
                .whereEqualTo("reporterId", userId)
                .get();

        com.google.android.gms.tasks.Tasks.whenAllComplete(votesTask, reportsTask).addOnCompleteListener(task -> {
            int positiveVotes = 0;
            int negativeVotes = 0;
            int validReports = 0;
            int invalidReports = 0;

            if (votesTask.isSuccessful() && votesTask.getResult() != null) {
                for (DocumentSnapshot doc : votesTask.getResult().getDocuments()) {
                    String voteType = doc.getString("voteType");
                    if ("UPVOTE".equals(voteType) || "STILL_EXISTS".equals(voteType)) {
                        positiveVotes++;
                    } else if ("DOWNVOTE".equals(voteType) || "NOT_FOUND".equals(voteType)) {
                        negativeVotes++;
                    }
                }
            }

            if (reportsTask.isSuccessful() && reportsTask.getResult() != null) {
                for (DocumentSnapshot doc : reportsTask.getResult().getDocuments()) {
                    String vStatus = doc.getString("verificationStatus");
                    if ("VERIFIED".equals(vStatus)) {
                        validReports++;
                    } else if ("DISPUTED".equals(vStatus)) {
                        invalidReports++;
                    }
                }
            }

            callback.onSuccess(positiveVotes, negativeVotes, validReports, invalidReports);
        });
    }
}
