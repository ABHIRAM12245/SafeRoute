package com.vlabs.routeguard.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class AppPreferences {
    private static final String PREF_NAME = "RouteGuardPreferences";
    private static final String KEY_NIGHT_MODE = "night_mode";
    private static final String KEY_LANGUAGE = "app_language";

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
