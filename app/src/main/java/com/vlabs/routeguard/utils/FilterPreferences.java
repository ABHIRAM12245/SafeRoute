package com.vlabs.routeguard.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class FilterPreferences {
    private static final String PREF_NAME = "RouteGuardFilters";
    private static final String KEY_CATEGORIES = "selected_categories";
    private final SharedPreferences prefs;

    public FilterPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveFilters(Set<String> categories) {
        prefs.edit().putStringSet(KEY_CATEGORIES, new HashSet<>(categories)).apply();
    }

    public Set<String> loadFilters() {
        if (!prefs.contains(KEY_CATEGORIES)) {
            Set<String> defaultCategories = new HashSet<>();
            for (com.vlabs.routeguard.models.ReportType type : com.vlabs.routeguard.models.ReportType.values()) {
                defaultCategories.add(type.name());
            }
            return defaultCategories; // Default: All Categories
        }
        Set<String> saved = prefs.getStringSet(KEY_CATEGORIES, new HashSet<>());
        return new HashSet<>(saved); // Defensive copy
    }

    public void saveNotificationRadiusKm(float radiusKm) {
        prefs.edit().putFloat("notification_radius_km", radiusKm).apply();
    }

    public float getNotificationRadiusKm() {
        return prefs.getFloat("notification_radius_km", 5.0f); // Default 5.0 km
    }
}
