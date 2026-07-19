package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.User;

/**
 * Dedicated Firestore repository responsible for all user profile operations.
 *
 * Future implementation:
 * - users/{userId}
 * - saveUser()
 * - getUser()
 * - updateLastActive()
 * - updateProfile()
 */
public class UserFirestoreRepository {

    public interface GetUserCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public interface SaveUserCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface UpdateLastActiveCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    private final com.google.firebase.firestore.FirebaseFirestore db;

    public UserFirestoreRepository() {
        this.db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
    }

    /**
     * Saves a user to Firestore.
     *
     * @param user     The user model to save.
     * @param callback The callback for completion.
     */
    public void saveUser(User user, SaveUserCallback callback) {
        if (callback == null) return;

        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User and User ID cannot be null or empty"));
            return;
        }

        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("userId", user.getUserId());
        userData.put("displayName", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("photoUrl", user.getPhotoUrl());
        
        if (user.getCreatedAt() == 0) {
            userData.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        }
        
        userData.put("trustScore", user.getTrustScore());
        userData.put("reputationScore", user.getReputationScore());
        userData.put("isModerator", user.isModerator());
        userData.put("isBlocked", user.isBlocked());

        db.collection("users").document(user.getUserId())
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates the last active timestamp for a user.
     *
     * @param userId   The ID of the user.
     * @param callback The callback for completion.
     */
    public void updateLastActive(String userId, UpdateLastActiveCallback callback) {
        if (callback == null) return;

        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        db.collection("users").document(userId)
                .update("lastActiveAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Retrieves a user from Firestore.
     *
     * @param userId   The ID of the user to retrieve.
     * @param callback The callback for completion.
     */
    public void getUser(String userId, GetUserCallback callback) {
        if (callback == null) return;

        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = mapDocumentToUser(documentSnapshot);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(new Exception("Failed to parse user document"));
                        }
                    } else {
                        callback.onFailure(new Exception("User profile not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    private User mapDocumentToUser(com.google.firebase.firestore.DocumentSnapshot document) {
        try {
            if (!document.exists()) return null;

            User user = new User();
            user.setUserId(document.getString("userId"));
            user.setDisplayName(document.getString("displayName"));
            user.setEmail(document.getString("email"));
            user.setPhotoUrl(document.getString("photoUrl"));

            Object createdAtObj = document.get("createdAt");
            if (createdAtObj instanceof com.google.firebase.Timestamp) {
                user.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate().getTime());
            } else if (createdAtObj instanceof Long) {
                user.setCreatedAt((Long) createdAtObj);
            } else if (createdAtObj instanceof Double) {
                user.setCreatedAt(((Double) createdAtObj).longValue());
            }

            Object lastActiveAtObj = document.get("lastActiveAt");
            if (lastActiveAtObj instanceof com.google.firebase.Timestamp) {
                user.setLastActiveAt(((com.google.firebase.Timestamp) lastActiveAtObj).toDate().getTime());
            } else if (lastActiveAtObj instanceof Long) {
                user.setLastActiveAt((Long) lastActiveAtObj);
            } else if (lastActiveAtObj instanceof Double) {
                user.setLastActiveAt(((Double) lastActiveAtObj).longValue());
            }

            if (document.contains("trustScore") && document.getLong("trustScore") != null) {
                user.setTrustScore(document.getLong("trustScore").intValue());
            }
            if (document.contains("reputationScore") && document.getLong("reputationScore") != null) {
                user.setReputationScore(document.getLong("reputationScore").intValue());
            }
            
            Boolean isModerator = document.getBoolean("isModerator");
            user.setModerator(isModerator != null && isModerator);
            
            Boolean isBlocked = document.getBoolean("isBlocked");
            user.setBlocked(isBlocked != null && isBlocked);

            return user;
        } catch (Exception e) {
            android.util.Log.e("UserFirestoreRepo", "Error mapping user document", e);
            return null;
        }
    }
}
