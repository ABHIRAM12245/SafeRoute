package com.vlabs.routeguard.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.HomeActivity;
import com.vlabs.routeguard.viewmodels.AuthenticationState;
import com.vlabs.routeguard.viewmodels.AuthenticationViewModel;

/**
 * User Login Fragment.
 *
 * Provides the interactive UI for existing SafeRoute users to authenticate using
 * their email and password credentials.
 *
 * Architectural & Data Flow:
 * 1. Collects email and password inputs from the user interface.
 * 2. Delegates credential verification to {@link AuthenticationViewModel#login(String, String)}.
 * 3. Observes LiveData updates from {@link AuthenticationViewModel#getAuthState()}:
 *    - {@link AuthenticationState.Loading}: Shows a full-screen loading overlay and disables inputs.
 *    - {@link AuthenticationState.Success}: Displays a confirmation toast, clears the activity stack,
 *      and launches {@link HomeActivity}.
 *    - {@link AuthenticationState.Error}: Hides loading indicators and displays actionable error messages.
 * 4. Supports password reset requests via {@link AuthenticationViewModel#sendPasswordResetEmail(String)}.
 * 5. Enables quick navigation to {@link RegisterFragment} for new accounts via {@link AuthenticationActivity#loadFragment(Fragment)}.
 */
public class LoginFragment extends Fragment {

    private AuthenticationViewModel authViewModel;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private View layoutLoadingOverlay;
    private TextView tvGoToRegister, tvForgotPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the login layout containing input fields, buttons, and loading overlay
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize the ViewModel scoped to this Fragment's lifecycle
        authViewModel = new ViewModelProvider(this).get(AuthenticationViewModel.class);
        
        // Bind UI widgets
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        layoutLoadingOverlay = view.findViewById(R.id.layoutLoadingOverlay);
        tvGoToRegister = view.findViewById(R.id.tvGoToRegister);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);

        // Submit credentials for Firebase Authentication verification
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            authViewModel.login(email, password);
        });

        // Trigger password reset email via Firebase Auth for the entered email address
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                String email = etEmail.getText().toString().trim();
                authViewModel.sendPasswordResetEmail(email);
            });
        }

        // Switch container view to the Registration fragment
        tvGoToRegister.setOnClickListener(v -> {
            if (getActivity() instanceof AuthenticationActivity) {
                // Clear any lingering error or loading state before navigating
                authViewModel.resetState();
                ((AuthenticationActivity) getActivity()).loadFragment(new RegisterFragment());
            }
        });

        // Observe authentication state changes emitted by the ViewModel
        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            boolean isLoading = state instanceof AuthenticationState.Loading;
            // Toggle visual loading indicator
            if (layoutLoadingOverlay != null) {
                layoutLoadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            
            // Prevent multiple concurrent login attempts while an operation is in-flight
            btnLogin.setEnabled(!isLoading);
            etEmail.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
            if (tvGoToRegister != null) tvGoToRegister.setEnabled(!isLoading);
            if (tvForgotPassword != null) tvForgotPassword.setEnabled(!isLoading);

            if (state instanceof AuthenticationState.Success) {
                // Login succeeded: SessionManager is now populated. Route to main map screen.
                Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), HomeActivity.class);
                    // Clear the back-stack so hardware Back exits the app rather than reopening login
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            } else if (state instanceof AuthenticationState.Error) {
                // Login failed: Surface error description returned by the repository / Firebase
                AuthenticationState.Error errorState = (AuthenticationState.Error) state;
                Toast.makeText(getContext(), errorState.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
