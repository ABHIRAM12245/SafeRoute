package com.vlabs.routeguard.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;
import com.vlabs.routeguard.authentication.AuthenticationActivity;
import com.vlabs.routeguard.fragments.LoginRequiredBottomSheet;
import com.vlabs.routeguard.fragments.ProfileBottomSheet;
import com.vlabs.routeguard.managers.SessionManager;
import com.vlabs.routeguard.notifications.NotificationActivity;
import com.vlabs.routeguard.utils.AppPreferences;

/**
 * Application Settings and Preferences Dashboard Activity.
 *
 * Provides entry points and configuration dialogs for user customization, system appearance,
 * geographic defaults, localization, and account management.
 *
 * Managed Capabilities:
 * - Profile Access: Launches {@link ProfileBottomSheet} for authenticated users or prompts {@link LoginRequiredBottomSheet}.
 * - Notification Rules: Navigates to {@link NotificationSettingsActivity}.
 * - Search Radius Configuration: Persists default hazard detection radius (1km to 25km).
 * - Theme Configuration: Toggles Dark Mode, Light Mode, or System Follow via {@link AppCompatDelegate#setDefaultNightMode(int)}.
 * - Multi-Language Localization: Allows dynamic locale selection via {@link AppPreferences#applyLanguage(String)}.
 * - About SafeRoute: Navigates to {@link AboutActivity}.
 * - Session Termination: Confirms logout via dialog, shuts down Travel Mode workers, clears local/cloud credentials,
 *   and redirects to {@link AuthenticationActivity}.
 */
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        // 1. View Profile
        if (findViewById(R.id.btnViewProfile) != null) {
            findViewById(R.id.btnViewProfile).setOnClickListener(v -> {
                if (isFinishing() || isDestroyed()) return;
                if (!SessionManager.getInstance().isLoggedIn()) {
                    LoginRequiredBottomSheet.newInstance().show(getSupportFragmentManager(), "LoginRequiredBottomSheet");
                    return;
                }
                new ProfileBottomSheet().show(getSupportFragmentManager(), "ProfileBottomSheet");
            });
        }

        // 2. Notification Settings
        if (findViewById(R.id.btnNotificationSettings) != null) {
            findViewById(R.id.btnNotificationSettings).setOnClickListener(v -> {
                if (isFinishing() || isDestroyed()) return;
                Intent intent = new Intent(SettingsActivity.this, NotificationSettingsActivity.class);
                startActivity(intent);
            });
        }

        // 3. Default Search Radius
        if (findViewById(R.id.btnDefaultSearchRadius) != null) {
            findViewById(R.id.btnDefaultSearchRadius).setOnClickListener(v -> {
                if (isFinishing() || isDestroyed()) return;
                com.vlabs.routeguard.utils.FilterPreferences filterPrefs = new com.vlabs.routeguard.utils.FilterPreferences(this);
                String[] options = new String[]{"1 km", "3 km", "5 km (Default)", "10 km"};
                float[] values = new float[]{1.0f, 3.0f, 5.0f, 10.0f};

                float currentRadius = filterPrefs.getNotificationRadiusKm();
                int selectedIndex = 2; // Default 5km
                if (currentRadius == 1.0f) selectedIndex = 0;
                else if (currentRadius == 3.0f) selectedIndex = 1;
                else if (currentRadius == 5.0f) selectedIndex = 2;
                else if (currentRadius == 10.0f) selectedIndex = 3;

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Select Notification Radius")
                        .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                            filterPrefs.saveNotificationRadiusKm(values[which]);
                            Toast.makeText(this, "Notification radius set to " + options[which], Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // 4. Dark Mode
        TextView tvDarkModeStatus = findViewById(R.id.tvDarkModeStatus);
        MaterialSwitch switchDarkMode = findViewById(R.id.switchDarkMode);
        AppPreferences appPrefs = new AppPreferences(this);

        int currentNightMode = appPrefs.getNightMode();
        updateDarkModeUI(currentNightMode, tvDarkModeStatus, switchDarkMode);

        if (findViewById(R.id.btnDarkMode) != null) {
            findViewById(R.id.btnDarkMode).setOnClickListener(v -> {
                if (isFinishing() || isDestroyed()) return;
                String[] options = new String[]{"System Default", "Light", "Dark"};
                int modeVal = appPrefs.getNightMode();
                int selectedIndex = 0; // Default System
                if (modeVal == AppCompatDelegate.MODE_NIGHT_NO) selectedIndex = 1;
                else if (modeVal == AppCompatDelegate.MODE_NIGHT_YES) selectedIndex = 2;

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Select Theme")
                        .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                            int newMode;
                            if (which == 1) newMode = AppCompatDelegate.MODE_NIGHT_NO;
                            else if (which == 2) newMode = AppCompatDelegate.MODE_NIGHT_YES;
                            else newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

                            appPrefs.saveNightMode(newMode);
                            AppPreferences.applyTheme(newMode);
                            updateDarkModeUI(newMode, tvDarkModeStatus, switchDarkMode);
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // 5. Language Selection
        TextView tvLanguageStatus = findViewById(R.id.tvLanguageStatus);
        String currentLang = appPrefs.getLanguage();
        if (tvLanguageStatus != null) {
            if ("hi".equals(currentLang)) {
                tvLanguageStatus.setText("હિન્દી (Hindi)");
            } else if ("gu".equals(currentLang)) {
                tvLanguageStatus.setText("ગુજરાતી (Gujarati)");
            } else {
                tvLanguageStatus.setText("English");
            }
        }

        if (findViewById(R.id.btnLanguage) != null) {
            findViewById(R.id.btnLanguage).setOnClickListener(v -> {
                if (isFinishing() || isDestroyed()) return;
                String[] languages = new String[]{"English", "હિન્દી (Hindi)", "ગુજરાતી (Gujarati)"};
                String activeLang = appPrefs.getLanguage();
                int selectedLangIndex = 0;
                if ("hi".equals(activeLang)) selectedLangIndex = 1;
                else if ("gu".equals(activeLang)) selectedLangIndex = 2;

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Select Language")
                        .setSingleChoiceItems(languages, selectedLangIndex, (dialog, which) -> {
                            String selectedLangCode = "en";
                            if (which == 1) selectedLangCode = "hi";
                            else if (which == 2) selectedLangCode = "gu";

                            if (!selectedLangCode.equals(activeLang)) {
                                appPrefs.saveLanguage(selectedLangCode);
                                AppPreferences.applyLanguage(selectedLangCode);
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // 6. About
        if (findViewById(R.id.btnAbout) != null) {
            findViewById(R.id.btnAbout).setOnClickListener(v -> {
                if (isFinishing() || isDestroyed()) return;
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
            });
        }

        // 7. App Version
        TextView tvSettingsAppVersion = findViewById(R.id.tvSettingsAppVersion);
        if (tvSettingsAppVersion != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                if (versionName != null && !versionName.isEmpty()) {
                    tvSettingsAppVersion.setText("v" + versionName);
                }
            } catch (Exception ignored) {}
        }

        // 8. Logout
        if (findViewById(R.id.btnLogout) != null) {
            findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    private void showLogoutConfirmationDialog() {
        if (isFinishing() || isDestroyed()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of RouteGuard?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    Context appContext = getApplicationContext() != null ? getApplicationContext() : this;
                    SessionManager.getInstance().logout(appContext);
                    try {
                        Intent intent = new Intent(appContext, AuthenticationActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        appContext.startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("SettingsActivity", "Error navigating to AuthenticationActivity during logout", e);
                    }
                    if (!isFinishing()) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateDarkModeUI(int mode, TextView tvStatus, MaterialSwitch switchView) {
        if (tvStatus != null) {
            if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
                tvStatus.setText("Light");
            } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
                tvStatus.setText("Dark");
            } else {
                tvStatus.setText("System Default");
            }
        }
        if (switchView != null) {
            switchView.setChecked(mode == AppCompatDelegate.MODE_NIGHT_YES);
        }
    }
}
