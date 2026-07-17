package com.vlabs.routeguard.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.authentication.AuthenticationActivity;

public class LoginRequiredBottomSheet extends BottomSheetDialogFragment {

    public static LoginRequiredBottomSheet newInstance() {
        return new LoginRequiredBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_login_required, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnCreateAccount = view.findViewById(R.id.btnCreateAccount);
        Button btnNotNow = view.findViewById(R.id.btnNotNow);

        btnLogin.setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(getContext(), AuthenticationActivity.class);
            intent.putExtra(AuthenticationActivity.EXTRA_START_FRAGMENT, AuthenticationActivity.FRAGMENT_LOGIN);
            startActivity(intent);
        });

        btnCreateAccount.setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(getContext(), AuthenticationActivity.class);
            intent.putExtra(AuthenticationActivity.EXTRA_START_FRAGMENT, AuthenticationActivity.FRAGMENT_REGISTER);
            startActivity(intent);
        });

        btnNotNow.setOnClickListener(v -> dismiss());
    }
}
