package com.vlabs.routeguard.settings;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;
import com.vlabs.routeguard.utils.AppPreferences;

/**
 * Notification Preferences Configuration Activity.
 *
 * Provides granular user controls over alerting behavior, channel suppression, sound,
 * vibration, and Do Not Disturb (DND) modes.
 *
 * Preference Hierarchy & Control Rules:
 * - Master Switch: Toggling {@code switchMasterNotifications} enables or disables all notification
 *   subsystems and cascades an enabled/disabled UI state to all child switches.
 * - Granular Channels: Independent toggles for Hazard Alerts (accidents, road closures, police checks)
 *   and Community Alerts (new comments, upvotes).
 * - Feedback Controls: Separate toggles for notification sound and vibration.
 * - Do Not Disturb (DND) Intelligence: When DND is active, general community updates are silenced,
 *   while critical road hazard warnings are preserved to guarantee driver safety.
 * - Persistence: All preferences are backed by {@link AppPreferences} via Android {@link android.content.SharedPreferences}.
 */
public class NotificationSettingsActivity extends BaseActivity {

    private AppPreferences appPreferences;
    private MaterialSwitch switchMasterNotifications;
    private MaterialSwitch switchHazardAlerts;
    private MaterialSwitch switchCommunityAlerts;
    private MaterialSwitch switchSound;
    private MaterialSwitch switchVibration;
    private MaterialSwitch switchDnd;
    private TextView tvDndBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        appPreferences = new AppPreferences(this);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        switchMasterNotifications = findViewById(R.id.switchMasterNotifications);
        switchHazardAlerts = findViewById(R.id.switchHazardAlerts);
        switchCommunityAlerts = findViewById(R.id.switchCommunityAlerts);
        switchSound = findViewById(R.id.switchSound);
        switchVibration = findViewById(R.id.switchVibration);
        switchDnd = findViewById(R.id.switchDnd);
        tvDndBadge = findViewById(R.id.tvDndBadge);

        loadPreferences();
        setupListeners();
    }

    private void loadPreferences() {
        boolean masterEnabled = appPreferences.isNotificationsEnabled();
        switchMasterNotifications.setChecked(masterEnabled);

        switchHazardAlerts.setChecked(appPreferences.isHazardAlertsEnabled());
        switchCommunityAlerts.setChecked(appPreferences.isCommunityAlertsEnabled());
        switchSound.setChecked(appPreferences.isSoundEnabled());
        switchVibration.setChecked(appPreferences.isVibrationEnabled());

        boolean dndEnabled = appPreferences.isDndEnabled();
        switchDnd.setChecked(dndEnabled);
        updateDndBadge(dndEnabled);

        updateSubSwitchesState(masterEnabled);
    }

    private void updateSubSwitchesState(boolean masterEnabled) {
        switchHazardAlerts.setEnabled(masterEnabled);
        switchCommunityAlerts.setEnabled(masterEnabled);
        switchSound.setEnabled(masterEnabled);
        switchVibration.setEnabled(masterEnabled);
        switchDnd.setEnabled(masterEnabled);
    }

    private void updateDndBadge(boolean dndEnabled) {
        if (tvDndBadge != null) {
            if (dndEnabled) {
                tvDndBadge.setText("Active");
                tvDndBadge.setBackgroundResource(R.drawable.bg_chip_verified);
                tvDndBadge.setTextColor(ContextCompat.getColor(this, R.color.rg_on_primary_container));
            } else {
                tvDndBadge.setText("Off");
                tvDndBadge.setBackgroundResource(R.drawable.bg_chip_expired);
                tvDndBadge.setTextColor(ContextCompat.getColor(this, R.color.rg_on_surface_variant));
            }
        }
    }

    private void setupListeners() {
        switchMasterNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPreferences.setNotificationsEnabled(isChecked);
            updateSubSwitchesState(isChecked);
            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        switchHazardAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPreferences.setHazardAlertsEnabled(isChecked);
        });

        switchCommunityAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPreferences.setCommunityAlertsEnabled(isChecked);
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPreferences.setSoundEnabled(isChecked);
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPreferences.setVibrationEnabled(isChecked);
        });

        switchDnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPreferences.setDndEnabled(isChecked);
            updateDndBadge(isChecked);
            Toast.makeText(this, isChecked ? "Do Not Disturb enabled" : "Do Not Disturb disabled", Toast.LENGTH_SHORT).show();
        });
    }
}
