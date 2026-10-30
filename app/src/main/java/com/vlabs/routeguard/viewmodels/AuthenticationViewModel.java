package com.vlabs.routeguard.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vlabs.routeguard.repositories.AuthenticationRepository;
import com.vlabs.routeguard.utils.AuthenticationValidator;

/**
 * Authentication ViewModel.
 *
 * Manages UI state and business logic for user authentication, registration,
 * and password recovery workflows.
 *
 * Architecture & Data Flow:
 * - Sits between the UI layer ({@link LoginFragment}, {@link RegisterFragment})
 *   and the data access layer ({@link AuthenticationRepository}).
 * - Performs pre-submission input validation using {@link AuthenticationValidator}
 *   (checking for non-empty fields, valid email syntax, and minimum 6-character passwords).
 * - Exposes an observable {@link LiveData} of {@link AuthenticationState} (Idle, Loading, Success, Error)
 *   so UI controllers can reactively update buttons, loading overlays, and navigation without tight coupling.
 */
public class AuthenticationViewModel extends ViewModel {

    private final AuthenticationRepository authRepository;
    private final MutableLiveData<AuthenticationState> authState = new MutableLiveData<>(new AuthenticationState.Idle());

    public AuthenticationViewModel() {
        this.authRepository = new AuthenticationRepository();
    }

    /**
     * Exposes the current authentication UI state as an immutable LiveData stream.
     *
     * @return LiveData containing the active {@link AuthenticationState}.
     */
    public LiveData<AuthenticationState> getAuthState() {
        return authState;
    }

    public void login(String email, String password) {
        if (AuthenticationValidator.isEmpty(email)) {
            authState.setValue(new AuthenticationState.Error("Please enter your email"));
            return;
        }
        if (!AuthenticationValidator.isValidEmail(email)) {
            authState.setValue(new AuthenticationState.Error("Please enter a valid email address"));
            return;
        }
        if (AuthenticationValidator.isEmpty(password)) {
            authState.setValue(new AuthenticationState.Error("Please enter your password"));
            return;
        }
        if (!AuthenticationValidator.isValidPassword(password)) {
            authState.setValue(new AuthenticationState.Error("Password must be at least 6 characters"));
            return;
        }

        authState.setValue(new AuthenticationState.Loading());
        
        authRepository.login(email.trim(), password, new AuthenticationRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                authState.postValue(new AuthenticationState.Success());
            }

            @Override
            public void onError(String error) {
                authState.postValue(new AuthenticationState.Error(error));
            }
        });
    }

    public void register(String email, String password) {
        if (AuthenticationValidator.isEmpty(email)) {
            authState.setValue(new AuthenticationState.Error("Please enter your email"));
            return;
        }
        if (!AuthenticationValidator.isValidEmail(email)) {
            authState.setValue(new AuthenticationState.Error("Please enter a valid email address"));
            return;
        }
        if (AuthenticationValidator.isEmpty(password)) {
            authState.setValue(new AuthenticationState.Error("Please enter a password"));
            return;
        }
        if (!AuthenticationValidator.isValidPassword(password)) {
            authState.setValue(new AuthenticationState.Error("Password must be at least 6 characters"));
            return;
        }

        authState.setValue(new AuthenticationState.Loading());
        
        authRepository.register(email.trim(), password, new AuthenticationRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                authState.postValue(new AuthenticationState.Success());
            }

            @Override
            public void onError(String error) {
                authState.postValue(new AuthenticationState.Error(error));
            }
        });
    }

    public void sendPasswordResetEmail(String email) {
        if (AuthenticationValidator.isEmpty(email)) {
            authState.setValue(new AuthenticationState.Error("Please enter your email"));
            return;
        }
        if (!AuthenticationValidator.isValidEmail(email)) {
            authState.setValue(new AuthenticationState.Error("Please enter a valid email address"));
            return;
        }

        authState.setValue(new AuthenticationState.Loading());
        authRepository.sendPasswordResetEmail(email.trim(), new AuthenticationRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                authState.postValue(new AuthenticationState.Error("Password reset email sent! Check your inbox."));
            }

            @Override
            public void onError(String error) {
                authState.postValue(new AuthenticationState.Error(error));
            }
        });
    }
    
    public void resetState() {
        authState.setValue(new AuthenticationState.Idle());
    }
}
