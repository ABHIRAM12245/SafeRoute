package com.vlabs.routeguard.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthenticationRepository extends BaseRepository {
    
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public AuthenticationRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userRepository = new UserRepository();
    }

    public void login(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        userRepository.updateLastActive(authResult.getUser().getUid(), new UserRepository.UpdateLastActiveCallback() {
                            @Override
                            public void onSuccess() {
                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onError(e.getMessage());
                            }
                        });
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void register(String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        com.vlabs.routeguard.models.User newUser = new com.vlabs.routeguard.models.User(
                                firebaseUser.getUid(),
                                firebaseUser.getDisplayName(),
                                firebaseUser.getEmail(),
                                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null,
                                0, // Handled by FieldValue.serverTimestamp() in repo
                                0, // Handled by FieldValue.serverTimestamp() in repo
                                50, // trustScore
                                50, // reputationScore
                                false, // isModerator
                                false // isBlocked
                        );

                        userRepository.saveUser(newUser, new UserRepository.SaveUserCallback() {
                            @Override
                            public void onSuccess() {
                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onError("Failed to create user profile: " + e.getMessage());
                            }
                        });
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout() {
        firebaseAuth.signOut();
    }

    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
}
