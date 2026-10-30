package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.User;

/**
 * Domain repository responsible for orchestrating all user profile operations.
 * This repository sits between AuthenticationRepository and UserFirestoreRepository.
 */
public class UserRepository {

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

    private final UserFirestoreRepository userFirestoreRepository;

    public UserRepository() {
        this.userFirestoreRepository = new UserFirestoreRepository();
    }

    /**
     * Orchestrates saving a user profile.
     *
     * @param user     The user to save.
     * @param callback Callback for success/failure.
     */
    public void saveUser(User user, SaveUserCallback callback) {
        if (callback == null) return;
        
        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User and User ID cannot be null or empty"));
            return;
        }

        userFirestoreRepository.saveUser(user, new UserFirestoreRepository.SaveUserCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Updates the last active timestamp for a user.
     *
     * @param userId   The ID of the user.
     * @param callback Callback for success/failure.
     */
    public void updateLastActive(String userId, UpdateLastActiveCallback callback) {
        if (callback == null) return;
        
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        userFirestoreRepository.updateLastActive(userId, new UserFirestoreRepository.UpdateLastActiveCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Ensures that a user profile document exists for the given FirebaseUser.
     * If the profile exists, updates lastActiveAt; if missing, creates it safely.
     *
     * @param firebaseUser The currently authenticated Firebase user.
     * @param callback     Callback for success/failure.
     */
    public void ensureUserProfile(com.google.firebase.auth.FirebaseUser firebaseUser, SaveUserCallback callback) {
        if (callback == null) return;

        if (firebaseUser == null || firebaseUser.getUid() == null || firebaseUser.getUid().trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("FirebaseUser cannot be null or have an empty UID"));
            return;
        }

        userFirestoreRepository.ensureUserProfile(firebaseUser, new UserFirestoreRepository.SaveUserCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Orchestrates retrieving a user profile.
     *
     * @param userId   The ID of the user to retrieve.
     * @param callback Callback for success/failure.
     */
    public void getUser(String userId, GetUserCallback callback) {
        if (callback == null) return;
        
        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("User ID cannot be null or empty"));
            return;
        }

        userFirestoreRepository.getUser(userId, new UserFirestoreRepository.GetUserCallback() {
            @Override
            public void onSuccess(User user) {
                callback.onSuccess(user);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
}
