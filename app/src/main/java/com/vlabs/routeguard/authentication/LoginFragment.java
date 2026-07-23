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

public class LoginFragment extends Fragment {

    private AuthenticationViewModel authViewModel;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private View layoutLoadingOverlay;
    private TextView tvGoToRegister, tvForgotPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authViewModel = new ViewModelProvider(this).get(AuthenticationViewModel.class);
        
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        layoutLoadingOverlay = view.findViewById(R.id.layoutLoadingOverlay);
        tvGoToRegister = view.findViewById(R.id.tvGoToRegister);
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            authViewModel.login(email, password);
        });

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                String email = etEmail.getText().toString().trim();
                authViewModel.sendPasswordResetEmail(email);
            });
        }

        tvGoToRegister.setOnClickListener(v -> {
            if (getActivity() instanceof AuthenticationActivity) {
                authViewModel.resetState();
                ((AuthenticationActivity) getActivity()).loadFragment(new RegisterFragment());
            }
        });

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            boolean isLoading = state instanceof AuthenticationState.Loading;
            if (layoutLoadingOverlay != null) {
                layoutLoadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            
            btnLogin.setEnabled(!isLoading);
            etEmail.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
            if (tvGoToRegister != null) tvGoToRegister.setEnabled(!isLoading);
            if (tvForgotPassword != null) tvForgotPassword.setEnabled(!isLoading);

            if (state instanceof AuthenticationState.Success) {
                Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }
            } else if (state instanceof AuthenticationState.Error) {
                AuthenticationState.Error errorState = (AuthenticationState.Error) state;
                Toast.makeText(getContext(), errorState.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
