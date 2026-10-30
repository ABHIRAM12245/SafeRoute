package com.vlabs.routeguard.authentication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;

/**
 * Authentication Host Activity.
 *
 * Serves as the single-activity container for user authentication flows, hosting
 * either {@link LoginFragment} or {@link RegisterFragment} inside its fragment container.
 *
 * Execution Flow & Navigation:
 * - Launched from {@link com.vlabs.routeguard.activities.SplashActivity} when no active session is detected,
 *   or from {@link com.vlabs.routeguard.fragments.LoginRequiredBottomSheet} when an unauthenticated user attempts
 *   a protected action (e.g., submitting a hazard report or upvoting).
 * - Inspects {@link #EXTRA_START_FRAGMENT} to determine whether to present the Login or Registration screen initially.
 * - Provides {@link #loadFragment(Fragment)} to enable seamless switching between login and registration
 *   without starting new activities.
 */
public class AuthenticationActivity extends BaseActivity {

    /** Intent extra key determining the initial fragment to display */
    public static final String EXTRA_START_FRAGMENT = "START_FRAGMENT";

    /** Argument value to show the Login screen on startup */
    public static final String FRAGMENT_LOGIN = "LOGIN";

    /** Argument value to show the Registration screen on startup */
    public static final String FRAGMENT_REGISTER = "REGISTER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        // Prevent duplicate fragment attachment on orientation change or activity recreation
        if (savedInstanceState == null) {
            String startFragment = getIntent().getStringExtra(EXTRA_START_FRAGMENT);
            if (FRAGMENT_REGISTER.equals(startFragment)) {
                loadFragment(new RegisterFragment());
            } else {
                // Default to Login screen
                loadFragment(new LoginFragment());
            }
        }
    }

    /**
     * Swaps the active fragment in R.id.auth_fragment_container.
     *
     * @param fragment The fragment instance to display (LoginFragment or RegisterFragment).
     */
    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_fragment_container, fragment)
                .commit();
    }
}
