package com.vlabs.routeguard.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.authentication.AuthenticationActivity;
import com.vlabs.routeguard.databinding.ActivitySplashBinding;
import com.vlabs.routeguard.managers.SessionManager;
import com.vlabs.routeguard.utils.AppPreferences;

/**
 * Application Entry Point and Startup Activity.
 *
 * Registered as the sole android.intent.category.LAUNCHER activity in AndroidManifest.xml.
 * Responsible for:
 * 1. Initializing saved user preferences (Theme / Night Mode and Locale / Language) before window inflation.
 * 2. Displaying the branded SafeRoute splash logo and title with a smooth fade-in entrance animation.
 * 3. Inspecting the active authentication session via {@link SessionManager#isLoggedIn()}.
 * 4. Routing the user to either {@link HomeActivity} (if an active session exists) or
 *    {@link AuthenticationActivity} (if unauthenticated), clearing the back-stack completely.
 */
public class SplashActivity extends AppCompatActivity {

    /** Duration in milliseconds that the splash screen remains visible before routing */
    private static final int SPLASH_DURATION = 1500;
    private ActivitySplashBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Step 1: Pre-inflate setup: Load and apply persisted Dark Mode and Language preferences
        // to ensure all subsequent UI elements and string resources render in the correct configuration.
        AppPreferences prefs = new AppPreferences(this);
        AppPreferences.applyTheme(prefs.getNightMode());
        AppPreferences.applyLanguage(prefs.getLanguage());

        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Step 2: Trigger visual entrance animation for branding elements
        startFadeInAnimation();

        // Step 3: Schedule authentication evaluation and screen transition
        navigateToNextScreenAfterDelay();
    }

    /**
     * Executes the fade-in alpha animation on the splash logo and title container
     * to provide a polished, smooth visual transition during startup.
     */
    private void startFadeInAnimation() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        binding.splashContainer.startAnimation(fadeIn);
    }

    /**
     * Schedules the post-splash routing decision on the main Looper after {@link #SPLASH_DURATION}.
     *
     * Execution Flow:
     * - Validates that the hosting Activity is still alive (not finishing or destroyed).
     * - Queries {@link SessionManager#isLoggedIn()}, which checks both Firebase Authentication
     *   (FirebaseAuth.getInstance().getCurrentUser() != null) and local session caches.
     * - Directs authenticated users to {@link HomeActivity} for instant map and hazard access.
     * - Directs unauthenticated users to {@link AuthenticationActivity} to register or log in.
     * - Applies FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK to ensure the user cannot
     *   navigate back to the splash screen via the hardware/system Back button.
     */
    private void navigateToNextScreenAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Guard check: Avoid performing fragment transactions or navigation if the user exited
            if (isFinishing() || isDestroyed()) return;

            Intent intent;
            if (SessionManager.getInstance().isLoggedIn()) {
                // User is authenticated: Launch the main home map interface
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                // User is not authenticated: Launch the login/registration workflow
                intent = new Intent(SplashActivity.this, AuthenticationActivity.class);
            }

            // Clear the entire task back-stack so Back button exits rather than returning to Splash
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
