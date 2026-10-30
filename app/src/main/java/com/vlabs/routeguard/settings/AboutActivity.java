package com.vlabs.routeguard.settings;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;

/**
 * About and Legal Attribution Activity.
 *
 * Displays the installed application version, terms of service and community guidelines,
 * open-source library licenses, and copyright notices.
 *
 * Attribution & Compliance:
 * - Dynamically extracts {@code versionName} from {@link android.content.pm.PackageManager}.
 * - Displays OpenStreetMap Open Database License (ODbL) and OSMDroid Apache 2.0 attributions.
 * - Presents community safety and non-distracted driving terms.
 */
public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            try {
                String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                if (versionName != null && !versionName.isEmpty()) {
                    tvAppVersion.setText("Version " + versionName);
                }
            } catch (Exception ignored) {}
        }

        if (findViewById(R.id.btnTerms) != null) {
            findViewById(R.id.btnTerms).setOnClickListener(v -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Terms & Conditions")
                        .setMessage("SafeRoute is a community-driven road hazard awareness platform.\n\n"
                                + "• Always prioritize safe driving. Do not interact with the app while actively operating a vehicle.\n"
                                + "• Report accurate and verified road hazards to help fellow motorists.\n"
                                + "• Respect community guidelines. False or misleading reports may lead to trust score reduction.\n"
                                + "• Obey all local traffic and safety regulations.")
                        .setPositiveButton("Understood", null)
                        .show();
            });
        }

        if (findViewById(R.id.btnOpenSource) != null) {
            findViewById(R.id.btnOpenSource).setOnClickListener(v -> {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                        .setTitle("Open Source Licenses & Attributions")
                        .setMessage("SafeRoute is built with gratitude to the open-source community:\n\n"
                                + "• OpenStreetMap: Map data © OpenStreetMap contributors, licensed under the Open Database License (ODbL).\n\n"
                                + "• Osmdroid: OpenStreetMap-Tools for Android, licensed under the Apache License 2.0.\n\n"
                                + "• Google Firebase: Firebase Authentication, Cloud Firestore, Cloud Messaging, licensed under Apache 2.0.\n\n"
                                + "• Google Android Material Components: Licensed under Apache 2.0.\n\n"
                                + "• Square OkHttp: Licensed under Apache 2.0.\n\n"
                                + "• Bumptech Glide: Licensed under BSD, MIT, and Apache 2.0.")
                        .setPositiveButton("Close", null)
                        .show();
            });
        }
    }
}
