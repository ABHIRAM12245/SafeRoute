package com.vlabs.routeguard.managers;

import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.repositories.AuthenticationRepository;

/**
 * Global Session Manager Singleton.
 *
 * Provides a unified, application-wide interface for querying authentication state,
 * retrieving the active user's credentials and identifiers, and coordinating safe
 * session termination across local caches and cloud services.
 *
 * Architectural Role & Delegation:
 * - Serves as the single source of truth for UI controllers and background workers
 *   (e.g., {@link com.vlabs.routeguard.activities.SplashActivity}, {@link com.vlabs.routeguard.activities.HomeActivity},
 *   and {@link com.vlabs.routeguard.workers.TravelModeWorker}).
 * - Gracefully resolves active user properties (UID, email, display name) by first inspecting
 *   {@link com.google.firebase.auth.FirebaseUser} and falling back to {@link LocalAuthManager#getActiveUser()}.
 * - On logout: Cancels active Travel Mode proximity workers and clears both local credentials
 *   and Firebase Auth tokens.
 */
public class SessionManager {
    
    private final AuthenticationRepository authRepository;
    private static SessionManager instance;

    private SessionManager() {
        this.authRepository = new AuthenticationRepository();
    }

    /**
     * Retrieves the thread-safe singleton instance of SessionManager.
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Checks if a valid user session is active (either via Firebase Authentication or local storage).
     *
     * @return true if an authenticated user session is active; false otherwise.
     */
    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    public String getCurrentUserId() {
        if (getCurrentUser() != null && getCurrentUser().getUid() != null) {
            return getCurrentUser().getUid();
        }
        return LocalAuthManager.getInstance().getActiveUserId();
    }

    public String getDisplayName() {
        if (getCurrentUser() != null && getCurrentUser().getDisplayName() != null && !getCurrentUser().getDisplayName().isEmpty()) {
            return getCurrentUser().getDisplayName();
        }
        LocalAuthManager.LocalUser localUser = LocalAuthManager.getInstance().getActiveUser();
        return localUser != null ? localUser.displayName : "User";
    }

    public String getEmail() {
        if (getCurrentUser() != null && getCurrentUser().getEmail() != null) {
            return getCurrentUser().getEmail();
        }
        LocalAuthManager.LocalUser localUser = LocalAuthManager.getInstance().getActiveUser();
        return localUser != null ? localUser.email : "";
    }

    public void logout(android.content.Context context) {
        if (context != null) {
            try {
                android.content.Context appContext = context.getApplicationContext();
                TravelModeManager.getInstance().stopTravelModeOnLogout(appContext != null ? appContext : context);
            } catch (Exception e) {
                android.util.Log.e("SessionManager", "Error stopping TravelModeManager on logout", e);
            }
        }
        try {
            authRepository.logout();
        } catch (Exception e) {
            android.util.Log.e("SessionManager", "Error signing out", e);
        }
    }

    public void logout() {
        try {
            authRepository.logout();
        } catch (Exception e) {
            android.util.Log.e("SessionManager", "Error signing out", e);
        }
    }
}
