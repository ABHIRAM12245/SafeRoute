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
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                                        .addOnSuccessListener(token -> {
                                            new com.vlabs.routeguard.repositories.UserFirestoreRepository()
                                                    .updateFcmToken(authResult.getUser().getUid(), token);
                                        })
                                        .addOnFailureListener(e -> android.util.Log.e("AuthRepo", "Failed to fetch FCM token", e));
                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onError(getFriendlyErrorMessage(e));
                            }
                        });
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    public void register(String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String displayName = firebaseUser.getDisplayName();
                        if (displayName == null || displayName.trim().isEmpty()) {
                            if (email != null && email.contains("@")) {
                                displayName = email.split("@")[0];
                            } else {
                                displayName = "User";
                            }
                        }

                        com.vlabs.routeguard.models.User newUser = new com.vlabs.routeguard.models.User(
                                firebaseUser.getUid(),
                                displayName,
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
                                callback.onError("Failed to create user profile: " + getFriendlyErrorMessage(e));
                            }
                        });
                    } else {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    private String getFriendlyErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null) {
            return "Authentication failed. Please try again.";
        }
        String message = e.getMessage().toLowerCase(java.util.Locale.US);
        if (message.contains("network") || message.contains("connection") || message.contains("timeout") || message.contains("unreachable")) {
            return "Network failure. Please check your internet connection.";
        } else if (message.contains("email address is already in use") || message.contains("usercollision")) {
            return "This email is already registered. Please login or reset your password.";
        } else if (message.contains("invalid-credential") || message.contains("wrong-password") || message.contains("user-not-found") || message.contains("invalid_login_credentials")) {
            return "Invalid email or password. Please try again.";
        } else if (message.contains("badly formatted") || message.contains("invalid email")) {
            return "Invalid email address format.";
        } else if (message.contains("weak-password")) {
            return "Password is too weak. Please use at least 6 characters.";
        } else if (message.contains("disabled") || message.contains("blocked")) {
            return "This account has been disabled. Please contact support.";
        }
        return e.getMessage();
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
