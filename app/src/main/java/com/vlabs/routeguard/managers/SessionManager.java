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

    public void logout() {
        authRepository.logout();
    }
}
