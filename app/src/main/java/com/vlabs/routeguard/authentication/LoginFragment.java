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

public class LoginFragment extends Fragment {

    private AuthenticationViewModel authViewModel;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvGoToRegister;

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
        progressBar = view.findViewById(R.id.progressBar);
        tvGoToRegister = view.findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            authViewModel.login(email, password);
        });

        tvGoToRegister.setOnClickListener(v -> {
            if (getActivity() instanceof AuthenticationActivity) {
                authViewModel.resetState();
                ((AuthenticationActivity) getActivity()).loadFragment(new RegisterFragment());
            }
        });

        authViewModel.getAuthState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof AuthenticationState.Loading) {
                progressBar.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
            } else if (state instanceof AuthenticationState.Success) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                if (getActivity() != null) {
                    getActivity().finish(); // Return to previous activity
                }
            } else if (state instanceof AuthenticationState.Error) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                AuthenticationState.Error errorState = (AuthenticationState.Error) state;
                Toast.makeText(getContext(), errorState.getMessage(), Toast.LENGTH_SHORT).show();
            } else if (state instanceof AuthenticationState.Idle) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
            }
        });
    }
}
