package com.vlabs.routeguard.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;

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

        if (findViewById(R.id.btnGithub) != null) {
            findViewById(R.id.btnGithub).setOnClickListener(v -> openUrl("https://github.com/VLabs99"));
        }

        if (findViewById(R.id.btnLinkedin) != null) {
            findViewById(R.id.btnLinkedin).setOnClickListener(v -> openUrl("https://www.linkedin.com/in/vishwarajsinh-chudasama/"));
        }

        if (findViewById(R.id.btnPortfolio) != null) {
            findViewById(R.id.btnPortfolio).setOnClickListener(v -> openUrl("https://vlabs99.github.io/VLabs/"));
        }

        if (findViewById(R.id.btnPrivacy) != null) {
            findViewById(R.id.btnPrivacy).setOnClickListener(v -> {
                Toast.makeText(this, "Privacy Policy: RouteGuard protects your location data.", Toast.LENGTH_SHORT).show();
            });
        }

        if (findViewById(R.id.btnTerms) != null) {
            findViewById(R.id.btnTerms).setOnClickListener(v -> {
                Toast.makeText(this, "Terms & Conditions: Please drive safely and report responsibly.", Toast.LENGTH_SHORT).show();
            });
        }

        if (findViewById(R.id.btnOpenSource) != null) {
            findViewById(R.id.btnOpenSource).setOnClickListener(v -> {
                Toast.makeText(this, "Open Source Licenses: OpenStreetMap, Firebase, Material Components.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty() || isFinishing() || isDestroyed()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                startActivity(intent);
            }
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No browser installed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "No browser installed", Toast.LENGTH_SHORT).show();
        }
    }
}
