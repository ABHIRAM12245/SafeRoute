package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.User;

/**
 * Cloud Firestore User Profile Repository.
 *
 * Encapsulates all database operations targeting the Firestore {@code users} collection:
 * {@code users/{userId}}.
 *
 * Core Responsibilities & Data Flow:
 * - Profile Provisioning: {@link #ensureUserProfile(com.google.firebase.auth.FirebaseUser, SaveUserCallback)}
 *   checks if {@code users/{userId}} exists upon login; if missing, creates a default profile with initial
 *   reputation and trust scores (50/100) and server-stamped creation time.
 * - Profile Persistence: {@link #saveUser(User, SaveUserCallback)} writes or merges profile fields
 *   (displayName, email, photoUrl, trustScore, reputationScore, isModerator, isBlocked).
 * - Activity Tracking: {@link #updateLastActive(String, UpdateLastActiveCallback)} updates {@code lastActiveAt}
 *   using Firestore's atomic {@link com.google.firebase.firestore.FieldValue#serverTimestamp()}.
 * - Push Notification Routing: {@link #updateFcmToken(String, String)} associates device registration
 *   tokens with the user document for targeted proximity alerts.
 * - Profile Hydration: {@link #getUser(String, GetUserCallback)} fetches and maps Firestore document snapshots
 *   into application-level {@link User} domain objects.
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

        java.util.Map<String, Object> updateData = new java.util.HashMap<>();
        updateData.put("userId", userId);
        updateData.put("lastActiveAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Ensures that a user profile document exists for the given FirebaseUser.
     * If the profile already exists, updates lastActiveAt.
     * If it does not exist, safely creates it with valid initial values.
     *
     * @param firebaseUser The currently authenticated Firebase user.
     * @param callback     Callback for completion.
     */
    public void ensureUserProfile(com.google.firebase.auth.FirebaseUser firebaseUser, SaveUserCallback callback) {
        if (callback == null) return;
        if (firebaseUser == null || firebaseUser.getUid() == null || firebaseUser.getUid().trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("FirebaseUser cannot be null or have an empty UID"));
            return;
        }

        String userId = firebaseUser.getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // User profile exists, simply update lastActiveAt
                        updateLastActive(userId, new UpdateLastActiveCallback() {
                            @Override
                            public void onSuccess() {
                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        });
                    } else {
                        // Profile missing, create a default profile safely
                        String email = firebaseUser.getEmail();
                        String displayName = firebaseUser.getDisplayName();
                        if (displayName == null || displayName.trim().isEmpty()) {
                            if (email != null && email.contains("@")) {
                                displayName = email.split("@")[0];
                            } else {
                                displayName = "User";
                            }
                        }

                        User newUser = new User(
                                userId,
                                displayName,
                                email,
                                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null,
                                0, // Handled by FieldValue.serverTimestamp() in saveUser
                                0,
                                50, // default trustScore
                                50, // default reputationScore
                                false, // isModerator
                                false // isBlocked
                        );

                        saveUser(newUser, callback);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Updates the FCM token for a user.
     *
     * @param userId The ID of the user.
     * @param token  The new FCM token.
     */
    public void updateFcmToken(String userId, String token) {
        if (userId == null || userId.trim().isEmpty() || token == null || token.trim().isEmpty()) {
            return;
        }

        java.util.Map<String, Object> updateData = new java.util.HashMap<>();
        updateData.put("userId", userId);
        updateData.put("fcmToken", token);

        db.collection("users").document(userId)
                .set(updateData, com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener(e -> android.util.Log.e("UserFirestoreRepo", "Failed to update FCM token", e));
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
