package com.vlabs.routeguard.utils;

import android.util.Patterns;
import java.util.regex.Pattern;

public class AuthenticationValidator {

    private static final Pattern FALLBACK_EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
    );

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String trimmed = email.trim();
        try {
            if (Patterns.EMAIL_ADDRESS != null) {
                return Patterns.EMAIL_ADDRESS.matcher(trimmed).matches();
            }
        } catch (Throwable ignored) {
        }
        return FALLBACK_EMAIL_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }
}
