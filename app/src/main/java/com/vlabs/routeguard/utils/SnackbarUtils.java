package com.vlabs.routeguard.utils;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

public class SnackbarUtils {

    public static void showSuccess(View view, String message) {
        showCustomSnackbar(view, message, Color.parseColor("#D1FAE5"), Color.parseColor("#064E3B"));
    }

    public static void showError(View view, String message) {
        showCustomSnackbar(view, message, Color.parseColor("#FEE2E2"), Color.parseColor("#7F1D1D"));
    }

    public static void showWarning(View view, String message) {
        showCustomSnackbar(view, message, Color.parseColor("#FEF3C7"), Color.parseColor("#78350F"));
    }

    public static void showInfo(View view, String message) {
        showCustomSnackbar(view, message, Color.parseColor("#DBEAFE"), Color.parseColor("#1E3A8A"));
    }

    private static void showCustomSnackbar(View view, String message, int backgroundColor, int textColor) {
        if (view == null || message == null || message.isEmpty()) return;

        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(backgroundColor);

        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(textColor);
            textView.setTextSize(14f);
        }

        snackbar.show();
    }
}
