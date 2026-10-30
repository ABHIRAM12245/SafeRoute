package com.vlabs.routeguard.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class AppPreferences {
    private static final String PREF_NAME = "RouteGuardPreferences";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String KEY_NOTIF_ENABLED = "notif_enabled";
    private static final String KEY_NOTIF_HAZARDS = "notif_hazards";
    private static final String KEY_NOTIF_COMMUNITY = "notif_community";
    private static final String KEY_NOTIF_SOUND = "notif_sound";
    private static final String KEY_NOTIF_VIBRATE = "notif_vibrate";
    private static final String KEY_NOTIF_DND = "notif_dnd";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getNightMode() {
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void saveNightMode(int mode) {
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }

    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIF_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply();
    }

    public boolean isHazardAlertsEnabled() {
        return prefs.getBoolean(KEY_NOTIF_HAZARDS, true);
    }

    public void setHazardAlertsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_HAZARDS, enabled).apply();
    }

    public boolean isCommunityAlertsEnabled() {
        return prefs.getBoolean(KEY_NOTIF_COMMUNITY, true);
    }

    public void setCommunityAlertsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_COMMUNITY, enabled).apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_NOTIF_SOUND, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_SOUND, enabled).apply();
    }

    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_NOTIF_VIBRATE, true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_VIBRATE, enabled).apply();
    }

    public boolean isDndEnabled() {
        return prefs.getBoolean(KEY_NOTIF_DND, false);
    }

    public void setDndEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIF_DND, enabled).apply();
    }

    public String getLanguage() {
        LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
        if (!currentLocales.isEmpty() && currentLocales.get(0) != null) {
            String lang = currentLocales.get(0).getLanguage();
            if (lang != null && !lang.isEmpty()) {
                return lang;
            }
        }
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public void saveLanguage(String languageCode) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        applyLanguage(languageCode);
    }

    public static void applyTheme(int nightMode) {
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public static void applyLanguage(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            languageCode = "en";
        }
        LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocales);
    }
}
