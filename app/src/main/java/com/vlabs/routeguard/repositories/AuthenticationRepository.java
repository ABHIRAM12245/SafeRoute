package com.vlabs.routeguard.repositories;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Authentication Repository.
 *
 * Coordinates user identity verification, account registration, session termination,
 * and profile synchronization across Firebase Authentication, Cloud Firestore, and
 * the device's local offline account cache ({@link com.vlabs.routeguard.managers.LocalAuthManager}).
 *
 * Architectural & Data Flow:
 * - Hybrid Authentication Strategy:
 *   1. Queries {@link com.vlabs.routeguard.managers.LocalAuthManager} for instantaneous,
 *      offline-tolerant authentication.
 *   2. Falls back to {@link FirebaseAuth#signInWithEmailAndPassword(String, String)} for cloud verification.
 *   3. Upon cloud authentication success, synchronizes credentials to local cache and triggers
 *      {@link #syncUserPostLogin(FirebaseUser)} to ensure a corresponding profile document exists in
 *      the Cloud Firestore {@code users} collection, followed by updating the device's Firebase Cloud
 *      Messaging (FCM) registration token via {@link UserFirestoreRepository#updateFcmToken(String, String)}.
 * - Distinguishes between Firebase Authentication (identity & JWT management) and Cloud Firestore
 *   (storing application-level profile fields such as trust scores, reputation badges, and report statistics).
 */
public class AuthenticationRepository extends BaseRepository {
    
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    /**
     * Callback interface communicating asynchronous authentication outcomes back to callers (e.g. ViewModels).
     */
    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public AuthenticationRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.userRepository = new UserRepository();
    }

    public void login(String email, String password, AuthCallback callback) {
        String cleanEmail = email != null ? email.trim() : "";
        String cleanPass = password != null ? password : "";

        // 1. Verify against local accounts file first (Zero backend database required!)
        com.vlabs.routeguard.managers.LocalAuthManager localAuth = com.vlabs.routeguard.managers.LocalAuthManager.getInstance();
        com.vlabs.routeguard.managers.LocalAuthManager.LocalUser localUser = localAuth.authenticate(cleanEmail, cleanPass);

        if (localUser != null) {
            // Local file authentication succeeded! User is allowed inside the application.
            callback.onSuccess();
            return;
        }

        // 2. Fallback to Firebase Authentication if not found in local file
        firebaseAuth.signInWithEmailAndPassword(cleanEmail, cleanPass)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Cache into local file so future logins work offline without backend
                        String displayName = firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().isEmpty()
                                ? firebaseUser.getDisplayName()
                                : (firebaseUser.getEmail() != null ? firebaseUser.getEmail().split("@")[0] : "User");
                        localAuth.saveUser(firebaseUser.getUid(), firebaseUser.getEmail(), cleanPass, displayName);
                        syncUserPostLogin(firebaseUser);
                    }
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError(getFriendlyErrorMessage(e)));
    }

    private void syncUserPostLogin(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        userRepository.ensureUserProfile(firebaseUser, new UserRepository.SaveUserCallback() {
            @Override
            public void onSuccess() {
                android.util.Log.d("AuthRepo", "User profile synchronized successfully for UID: " + firebaseUser.getUid());
                com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(token -> {
                            new com.vlabs.routeguard.repositories.UserFirestoreRepository()
                                    .updateFcmToken(firebaseUser.getUid(), token);
                        })
                        .addOnFailureListener(e -> android.util.Log.w("AuthRepo", "Failed to fetch FCM token", e));
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.w("AuthRepo", "Post-login Firestore profile sync encountered an issue (non-fatal): " + e.getMessage());
            }
        });
    }

    public void register(String email, String password, AuthCallback callback) {
        String cleanEmail = email != null ? email.trim() : "";
        String cleanPass = password != null ? password : "";
        String displayName = cleanEmail.contains("@") ? cleanEmail.split("@")[0] : "User";

        // 1. Save directly into local file on device storage (no backend database required!)
        com.vlabs.routeguard.managers.LocalAuthManager.getInstance().saveUser(
                null,
                cleanEmail,
                cleanPass,
                displayName
        );

        // 2. Registration in local file succeeded! Allow user inside immediately.
        callback.onSuccess();

        // 3. Silently attempt background Firebase registration if reachable
        try {
            firebaseAuth.createUserWithEmailAndPassword(cleanEmail, cleanPass)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser != null) {
                            com.vlabs.routeguard.models.User newUser = new com.vlabs.routeguard.models.User(
                                    firebaseUser.getUid(),
                                    displayName,
                                    firebaseUser.getEmail(),
                                    null,
                                    0,
                                    0,
                                    50,
                                    50,
                                    false,
                                    false
                            );
                            userRepository.saveUser(newUser, new UserRepository.SaveUserCallback() {
                                @Override public void onSuccess() {}
                                @Override public void onFailure(Exception e) {}
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.d("AuthRepo", "Firebase background registration skipped/offline: " + e.getMessage());
                    });
        } catch (Exception ignored) {
        }
    }

    public void sendPasswordResetEmail(String email) {
        if (email != null && !email.trim().isEmpty()) {
            try {
                firebaseAuth.sendPasswordResetEmail(email.trim());
            } catch (Exception ignored) {
            }
        }
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
        } else if (message.contains("permission_denied") || message.contains("missing or insufficient permissions")) {
            return "Database permission denied. Please verify your Firestore security rules.";
        }
        return e.getMessage();
    }

    public void logout() {
        com.vlabs.routeguard.managers.LocalAuthManager.getInstance().logout();
        try {
            firebaseAuth.signOut();
        } catch (Exception ignored) {
        }
    }

    public boolean isLoggedIn() {
        return com.vlabs.routeguard.managers.LocalAuthManager.getInstance().isLoggedIn() || firebaseAuth.getCurrentUser() != null;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }
}
