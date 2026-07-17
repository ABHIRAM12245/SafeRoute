package com.vlabs.routeguard.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vlabs.routeguard.repositories.AuthenticationRepository;
import com.vlabs.routeguard.utils.AuthenticationValidator;

public class AuthenticationViewModel extends ViewModel {

    private final AuthenticationRepository authRepository;
    private final MutableLiveData<AuthenticationState> authState = new MutableLiveData<>(new AuthenticationState.Idle());

    public AuthenticationViewModel() {
        this.authRepository = new AuthenticationRepository();
    }

    public LiveData<AuthenticationState> getAuthState() {
        return authState;
    }

    public void login(String email, String password) {
        if (!AuthenticationValidator.isValidEmail(email)) {
            authState.setValue(new AuthenticationState.Error("Invalid email address"));
            return;
        }
        if (!AuthenticationValidator.isValidPassword(password)) {
            authState.setValue(new AuthenticationState.Error("Password must be at least 6 characters"));
            return;
        }

        authState.setValue(new AuthenticationState.Loading());
        
        authRepository.login(email, password, new AuthenticationRepository.AuthCallback() {
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
        if (!AuthenticationValidator.isValidEmail(email)) {
            authState.setValue(new AuthenticationState.Error("Invalid email address"));
            return;
        }
        if (!AuthenticationValidator.isValidPassword(password)) {
            authState.setValue(new AuthenticationState.Error("Password must be at least 6 characters"));
            return;
        }

        authState.setValue(new AuthenticationState.Loading());
        
        authRepository.register(email, password, new AuthenticationRepository.AuthCallback() {
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
    
    public void resetState() {
        authState.setValue(new AuthenticationState.Idle());
    }
}
