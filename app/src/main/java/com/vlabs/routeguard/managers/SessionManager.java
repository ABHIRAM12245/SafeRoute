package com.vlabs.routeguard.managers;

import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.repositories.AuthenticationRepository;

public class SessionManager {
    
    private final AuthenticationRepository authRepository;
    private static SessionManager instance;

    private SessionManager() {
        this.authRepository = new AuthenticationRepository();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return authRepository.isLoggedIn();
    }

    public FirebaseUser getCurrentUser() {
        return authRepository.getCurrentUser();
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
