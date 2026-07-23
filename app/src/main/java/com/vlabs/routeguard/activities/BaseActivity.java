package com.vlabs.routeguard.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.vlabs.routeguard.utils.AppPreferences;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppPreferences prefs = new AppPreferences(this);
        AppPreferences.applyTheme(prefs.getNightMode());
        AppPreferences.applyLanguage(prefs.getLanguage());
        super.onCreate(savedInstanceState);
    }
}
