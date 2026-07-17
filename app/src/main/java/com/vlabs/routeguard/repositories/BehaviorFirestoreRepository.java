package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.BehaviorMetrics;

/**
 * Dedicated Firestore repository responsible for retrieving raw metrics required by the Behavior Engine.
 *
 * Future metrics will be collected from:
 * - Votes Given
 * - Votes Received
 * - Reports Created
 * - Report Status
 * - Daily Activity
 * - Account Age
 */
public class BehaviorFirestoreRepository {

    public interface GetBehaviorMetricsCallback {
        void onSuccess(BehaviorMetrics metrics);
        void onFailure(Exception e);
    }

    public interface ReadBehaviorResultCallback {
        void onSuccess(com.vlabs.routeguard.models.BehaviorResult result);
        void onFailure(Exception e);
    }

    public interface SaveBehaviorResultCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public BehaviorFirestoreRepository() {
        // Default constructor
    }

    /**
     * Retrieves behavior metrics for a specific user.
     *
     * @param userId The ID of the user.
     * @param callback The callback to receive the metrics or an error.
     */
    public void getBehaviorMetrics(String userId, GetBehaviorMetricsCallback callback) {
        if (callback == null) return;

        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        // TODO: Implement Firestore reads in future milestones
        callback.onSuccess(new BehaviorMetrics());
    }

    public void readBehaviorResult(String userId, ReadBehaviorResultCallback callback) {
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("behavior_analysis").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.vlabs.routeguard.models.BehaviorResult result = documentSnapshot.toObject(com.vlabs.routeguard.models.BehaviorResult.class);
                        callback.onSuccess(result);
                    } else {
                        callback.onFailure(new Exception("Behavior result not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void saveBehaviorResult(String userId, com.vlabs.routeguard.models.BehaviorResult result, SaveBehaviorResultCallback callback) {
        if (callback == null) return;
        if (userId == null || userId.trim().isEmpty() || result == null) {
            callback.onFailure(new IllegalArgumentException("Invalid input"));
            return;
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("userId", userId);
        data.put("riskScore", result.getRiskScore());
        data.put("riskLevel", result.getRiskLevel() != null ? result.getRiskLevel().name() : null);
        
        java.util.List<String> flagStrings = new java.util.ArrayList<>();
        if (result.getFlags() != null) {
            for (com.vlabs.routeguard.models.BehaviorFlag flag : result.getFlags()) {
                flagStrings.add(flag.name());
            }
        }
        data.put("flags", flagStrings);
        data.put("summary", result.getSummary());
        data.put("analyzedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("behavior_analysis").document(userId)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
}
