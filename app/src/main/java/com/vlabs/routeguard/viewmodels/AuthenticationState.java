package com.vlabs.routeguard.viewmodels;

public abstract class AuthenticationState {
    private AuthenticationState() {}

    public static final class Idle extends AuthenticationState {}
    
    public static final class Loading extends AuthenticationState {}
    
    public static final class Success extends AuthenticationState {}
    
    public static final class Error extends AuthenticationState {
        private final String message;

        public Error(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
