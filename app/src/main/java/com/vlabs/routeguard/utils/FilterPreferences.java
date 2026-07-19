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
        Set<String> saved = prefs.getStringSet(KEY_CATEGORIES, new HashSet<>());
        return new HashSet<>(saved); // Defensive copy
    }
}
