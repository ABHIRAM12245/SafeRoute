package com.vlabs.routeguard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.models.User;
import com.vlabs.routeguard.repositories.UserRepository;

public class ProfileBottomSheet extends BottomSheetDialogFragment {

    private TextView tvDisplayName;
    private TextView tvEmail;
    private UserRepository userRepository;
    private User loadedUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        userRepository = new UserRepository();

        View btnClose = view.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        View btnEditName = view.findViewById(R.id.btnEditName);
        View layoutNameDisplay = view.findViewById(R.id.layoutNameDisplay);
        View layoutEditName = view.findViewById(R.id.layoutEditName);
        android.widget.EditText etDisplayName = view.findViewById(R.id.etDisplayName);
        View btnSaveName = view.findViewById(R.id.btnSaveName);
        View btnCancelName = view.findViewById(R.id.btnCancelName);

        if (btnEditName != null) {
            btnEditName.setOnClickListener(v -> {
                if (layoutNameDisplay != null) layoutNameDisplay.setVisibility(View.GONE);
                if (layoutEditName != null) layoutEditName.setVisibility(View.VISIBLE);
                if (etDisplayName != null && tvDisplayName != null) {
                    etDisplayName.setText(tvDisplayName.getText().toString());
                }
            });
        }

        if (btnCancelName != null) {
            btnCancelName.setOnClickListener(v -> {
                if (layoutEditName != null) layoutEditName.setVisibility(View.GONE);
                if (layoutNameDisplay != null) layoutNameDisplay.setVisibility(View.VISIBLE);
            });
        }

        if (btnSaveName != null) {
            btnSaveName.setOnClickListener(v -> {
                if (loadedUser != null && etDisplayName != null) {
                    String newName = etDisplayName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        loadedUser.setDisplayName(newName);
                        userRepository.saveUser(loadedUser, new UserRepository.SaveUserCallback() {
                            @Override
                            public void onSuccess() {
                                if (getActivity() == null || !isAdded()) return;
                                if (tvDisplayName != null) tvDisplayName.setText(newName);
                                if (layoutEditName != null) layoutEditName.setVisibility(View.GONE);
                                if (layoutNameDisplay != null) layoutNameDisplay.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                // Do not crash, keep edit mode open
                            }
                        });
                    }
                }
            });
        }

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            dismiss();
            return;
        }

        userRepository.getUser(currentUser.getUid(), new UserRepository.GetUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() == null || !isAdded()) return;
                loadedUser = user;
                
                if (tvDisplayName != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    tvDisplayName.setText(user.getDisplayName());
                }
                
                if (tvEmail != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    tvEmail.setText(user.getEmail());
                }

                TextView tvReputation = getView() != null ? getView().findViewById(R.id.tvReputation) : null;
                if (tvReputation != null) {
                    tvReputation.setText("Reputation: " + user.getReputationScore());
                }

                android.widget.ImageView ivProfilePhoto = getView() != null ? getView().findViewById(R.id.ivProfilePhoto) : null;
                if (ivProfilePhoto != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                    com.bumptech.glide.Glide.with(ProfileBottomSheet.this)
                         .load(user.getPhotoUrl())
                         .placeholder(ivProfilePhoto.getDrawable())
                         .error(ivProfilePhoto.getDrawable())
                         .into(ivProfilePhoto);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Do not crash, leave default placeholder values
            }
        });

        com.vlabs.routeguard.repositories.TrustRepository trustRepository = new com.vlabs.routeguard.repositories.TrustRepository();
        trustRepository.readTrustScore(currentUser.getUid(), new com.vlabs.routeguard.repositories.TrustRepository.ReadTrustScoreCallback() {
            @Override
            public void onSuccess(com.vlabs.routeguard.models.TrustScore trustScore) {
                if (getActivity() == null || !isAdded()) return;
                TextView tvTrustScore = getView() != null ? getView().findViewById(R.id.tvTrustScore) : null;
                if (tvTrustScore != null && trustScore != null) {
                    tvTrustScore.setText("Trust Score: " + trustScore.getScore() + " / 100");
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null || !isAdded()) return;
                TextView tvTrustScore = getView() != null ? getView().findViewById(R.id.tvTrustScore) : null;
                if (tvTrustScore != null) {
                    tvTrustScore.setText("Trust Score: --");
                }
            }
        });

        com.vlabs.routeguard.repositories.BehaviorRepository behaviorRepository = new com.vlabs.routeguard.repositories.BehaviorRepository();
        behaviorRepository.readBehaviorResult(currentUser.getUid(), new com.vlabs.routeguard.repositories.BehaviorRepository.ReadBehaviorResultCallback() {
            @Override
            public void onSuccess(com.vlabs.routeguard.models.BehaviorResult result) {
                if (getActivity() == null || !isAdded()) return;
                TextView tvBehaviorStatus = getView() != null ? getView().findViewById(R.id.tvBehaviorStatus) : null;
                if (tvBehaviorStatus != null && result != null && result.getRiskLevel() != null) {
                    String statusText = "Behavior: --";
                    switch (result.getRiskLevel()) {
                        case NORMAL: statusText = "Behavior: Normal"; break;
                        case LOW: statusText = "Behavior: Low Risk"; break;
                        case MEDIUM: statusText = "Behavior: Medium Risk"; break;
                        case HIGH: statusText = "Behavior: High Risk"; break;
                        case CRITICAL: statusText = "Behavior: Critical"; break;
                    }
                    tvBehaviorStatus.setText(statusText);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null || !isAdded()) return;
                TextView tvBehaviorStatus = getView() != null ? getView().findViewById(R.id.tvBehaviorStatus) : null;
                if (tvBehaviorStatus != null) {
                    tvBehaviorStatus.setText("Behavior: --");
                }
            }
        });
    }
}
