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
import com.vlabs.routeguard.viewmodels.AuthenticationState;
import com.vlabs.routeguard.viewmodels.AuthenticationViewModel;

/**
 * User Registration Fragment.
 *
 * Provides the user interface for creating a new SafeRoute account using
 * email and password credentials.
 *
 * Execution Flow & Architectural Role:
 * 1. Collects desired email and password inputs from the user.
 * 2. Invokes {@link AuthenticationViewModel#register(String, String)}, which coordinates with
 *    {@link com.vlabs.routeguard.repositories.AuthenticationRepository} to create the user in
 *    Firebase Authentication and provision an initial profile record in Cloud Firestore.
 * 3. Observes {@link AuthenticationViewModel#getAuthState()} LiveData:
 *    - {@link AuthenticationState.Loading}: Displays a progress bar and disables submission controls.
 *    - {@link AuthenticationState.Success}: Shows a success toast, clears the activity task stack,
 *      and launches {@link com.vlabs.routeguard.activities.HomeActivity}.
 *    - {@link AuthenticationState.Error}: Surfaces validation or network error descriptions.
 * 4. Provides a toggle button (tvGoToLogin) to return to {@link LoginFragment}.
 */
public class RegisterFragment extends Fragment {

    private AuthenticationViewModel authViewModel;
    private EditText etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvGoToLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the registration layout
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Bind the shared AuthenticationViewModel scoped to this Fragment
        authViewModel = new ViewModelProvider(this).get(AuthenticationViewModel.class);
        
        // Bind UI input elements
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        progressBar = view.findViewById(R.id.progressBar);
        tvGoToLogin = view.findViewById(R.id.tvGoToLogin);

        // Submit registration credentials
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            authViewModel.register(email, password);
        });

        // Navigate back to the LoginFragment
        tvGoToLogin.setOnClickListener(v -> {
            if (getActivity() instanceof AuthenticationActivity) {
                authViewModel.resetState();
                ((AuthenticationActivity) getActivity()).loadFragment(new LoginFragment());
            }
        });

        // Observe live authentication state updates
        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            boolean isLoading = state instanceof AuthenticationState.Loading;
            // Toggle indeterminate progress bar
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }

            // Disable controls to prevent duplicate submissions
            btnRegister.setEnabled(!isLoading);
            etEmail.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
            if (tvGoToLogin != null) tvGoToLogin.setEnabled(!isLoading);

            if (state instanceof AuthenticationState.Success) {
                // Account created and initial Firestore profile provisioned successfully
                Toast.makeText(getContext(), "Registration Successful", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), com.vlabs.routeguard.activities.HomeActivity.class);
                    // Clear the back-stack so hardware Back does not return to registration
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            } else if (state instanceof AuthenticationState.Error) {
                // Registration failed: Display error message (e.g., email already in use, weak password)
                AuthenticationState.Error errorState = (AuthenticationState.Error) state;
                Toast.makeText(getContext(), errorState.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
