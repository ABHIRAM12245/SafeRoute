package com.vlabs.routeguard.authentication;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;

public class AuthenticationActivity extends BaseActivity {

    public static final String EXTRA_START_FRAGMENT = "START_FRAGMENT";
    public static final String FRAGMENT_LOGIN = "LOGIN";
    public static final String FRAGMENT_REGISTER = "REGISTER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        if (savedInstanceState == null) {
            String startFragment = getIntent().getStringExtra(EXTRA_START_FRAGMENT);
            if (FRAGMENT_REGISTER.equals(startFragment)) {
                loadFragment(new RegisterFragment());
            } else {
                loadFragment(new LoginFragment());
            }
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.auth_fragment_container, fragment)
                .commit();
    }
}
