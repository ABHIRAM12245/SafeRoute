package com.vlabs.routeguard.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DURATION = 2000;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        startFadeInAnimation();
        navigateToHomeAfterDelay();
    }

    private void startFadeInAnimation() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        binding.splashContainer.startAnimation(fadeIn);
    }

    private void navigateToHomeAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed()) return;
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
