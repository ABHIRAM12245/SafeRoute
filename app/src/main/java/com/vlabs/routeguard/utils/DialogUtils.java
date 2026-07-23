package com.vlabs.routeguard.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vlabs.routeguard.R;

public class DialogUtils {

    public static void showDeleteConfirmation(Context context, String title, String message, Runnable onConfirm) {
        if (context == null) return;
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title != null ? title : "Delete Report")
                .setMessage(message != null ? message : "Are you sure you want to delete this report? This action cannot be undone.")
                .setPositiveButton("Delete", (dialogInterface, i) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton("Cancel", null)
                .show();

        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        }
    }

    public static void showLogoutConfirmation(Context context, Runnable onConfirm) {
        if (context == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of RouteGuard?")
                .setPositiveButton("Logout", (dialogInterface, i) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showConfirmation(Context context, String title, String message, String positiveText, Runnable onConfirm) {
        if (context == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText != null ? positiveText : "OK", (dialogInterface, i) -> {
                    if (onConfirm != null) onConfirm.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showLoginRequiredDialog(Context context, Runnable onLoginPressed) {
        if (context == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle("Login Required")
                .setMessage("Please sign in to access your profile and account features.")
                .setPositiveButton("Login", (dialog, which) -> {
                    if (onLoginPressed != null) {
                        onLoginPressed.run();
                    } else {
                        android.content.Intent intent = new android.content.Intent(context, com.vlabs.routeguard.authentication.AuthenticationActivity.class);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
