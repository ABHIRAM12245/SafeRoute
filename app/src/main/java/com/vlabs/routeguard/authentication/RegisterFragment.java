package com.vlabs.routeguard.authentication;

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

public class RegisterFragment extends Fragment {

    private AuthenticationViewModel authViewModel;
    private EditText etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvGoToLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        authViewModel = new ViewModelProvider(this).get(AuthenticationViewModel.class);
        
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        progressBar = view.findViewById(R.id.progressBar);
        tvGoToLogin = view.findViewById(R.id.tvGoToLogin);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            authViewModel.register(email, password);
        });

        tvGoToLogin.setOnClickListener(v -> {
            if (getActivity() instanceof AuthenticationActivity) {
                authViewModel.resetState();
                ((AuthenticationActivity) getActivity()).loadFragment(new LoginFragment());
            }
        });

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            boolean isLoading = state instanceof AuthenticationState.Loading;
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }

            btnRegister.setEnabled(!isLoading);
            etEmail.setEnabled(!isLoading);
            etPassword.setEnabled(!isLoading);
            if (tvGoToLogin != null) tvGoToLogin.setEnabled(!isLoading);

            if (state instanceof AuthenticationState.Success) {
                Toast.makeText(getContext(), "Registration Successful", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().finish();
                }
            } else if (state instanceof AuthenticationState.Error) {
                AuthenticationState.Error errorState = (AuthenticationState.Error) state;
                Toast.makeText(getContext(), errorState.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
