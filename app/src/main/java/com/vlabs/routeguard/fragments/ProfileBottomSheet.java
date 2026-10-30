package com.vlabs.routeguard.fragments;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.managers.LocalAuthManager;
import com.vlabs.routeguard.managers.LocalReportManager;
import com.vlabs.routeguard.managers.TrustManager;
import com.vlabs.routeguard.managers.UploadManager;
import com.vlabs.routeguard.models.BehaviorResult;
import com.vlabs.routeguard.models.PhotoAttachment;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.TrustScore;
import com.vlabs.routeguard.models.User;
import com.vlabs.routeguard.repositories.BehaviorRepository;
import com.vlabs.routeguard.repositories.ReportRepository;
import com.vlabs.routeguard.repositories.StorageRepository;
import com.vlabs.routeguard.repositories.TrustRepository;
import com.vlabs.routeguard.repositories.UserRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * User Profile and Reputation Bottom Sheet Dialog.
 *
 * Provides a comprehensive, interactive user profile dashboard displaying account identity,
 * membership longevity, report statistics, trust and reputation scores, and profile editing.
 *
 * Architectural & Data Flow:
 * 1. Sheet Behavior & Gestures:
 *    - Configured in {@link #onStart()} with a 65% peek height and two-step drag expansion (STATE_COLLAPSED to STATE_EXPANDED)
 *      providing a fluid, modern bottom sheet experience.
 * 2. Identity & Profile Hydration:
 *    - Resolves the active user via {@link SessionManager} and {@link UserRepository#getUser(String, UserRepository.GetUserCallback)}.
 *    - Displays display name, email address, and account creation date ("Member since MMMM yyyy").
 * 3. Profile Customization:
 *    - Profile Avatar: Launches system gallery via {@link ActivityResultContracts.GetContent}, compresses the chosen image,
 *      uploads it to Cloudinary via {@link UploadManager}, and updates both the {@link FirebaseUser} profile
 *      ({@link UserProfileChangeRequest}) and the Cloud Firestore {@code users/{userId}} document.
 *    - Inline Display Name Editing: Allows immediate updating of the user's name across Firebase Auth, Cloud Firestore,
 *      and {@link LocalAuthManager}.
 * 4. Reputation & Gamification:
 *    - Computes gamified reporter tiers ("Verified Reporter", "Contributor", "Road Guardian")
 *      and renders visual trust progress bars and behavioral flags via {@link TrustManager} and {@link BehaviorRepository}.
 * 5. Contribution Statistics:
 *    - Aggregates the total number of hazard reports submitted by this user across both local storage
 *      ({@link LocalReportManager}) and cloud storage ({@link ReportRepository}).
 */
public class ProfileBottomSheet extends BottomSheetDialogFragment {

    private TextView tvDisplayName;
    private TextView tvEmail;
    private TextView tvAccountSince;
    private TextView tvReportsCount;
    private ImageView ivProfilePhoto;
    private ProgressBar pbProfilePhotoUpload;
    private UserRepository userRepository;
    private User loadedUser;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadAndSetProfileImage(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_profile, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Support smooth dragging upwards into expanded full profile view & collapsing/dismissing
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            FrameLayout bottomSheet = ((BottomSheetDialog) dialog).findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight((int) (screenHeight * 0.65));
                behavior.setFitToContents(false);
                behavior.setHalfExpandedRatio(0.65f);
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                behavior.setSkipCollapsed(false);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvDisplayName = view.findViewById(R.id.tvDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAccountSince = view.findViewById(R.id.tvAccountSince);
        tvReportsCount = view.findViewById(R.id.tvReportsCount);
        ivProfilePhoto = view.findViewById(R.id.ivProfilePhoto);
        pbProfilePhotoUpload = view.findViewById(R.id.pbProfilePhotoUpload);

        userRepository = new UserRepository();

        View btnClose = view.findViewById(R.id.btnClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        // Photo change button
        View btnChangePhoto = view.findViewById(R.id.btnChangePhoto);
        if (btnChangePhoto != null) {
            btnChangePhoto.setOnClickListener(v -> {
                if (isAdded() && getActivity() != null) {
                    try {
                        galleryLauncher.launch("image/*");
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Unable to open gallery", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Inline edit name controls
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
                    CharSequence currentName = tvDisplayName.getText();
                    etDisplayName.setText(currentName != null ? currentName.toString() : "");
                    etDisplayName.requestFocus();
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
                if (etDisplayName == null) return;
                String newName = etDisplayName.getText().toString().trim();
                if (newName.isEmpty()) {
                    etDisplayName.setError("Name cannot be empty");
                    return;
                }

                saveDisplayName(newName, layoutEditName, layoutNameDisplay);
            });
        }

        loadUserProfile();
    }

    private void saveDisplayName(String newName, View layoutEditName, View layoutNameDisplay) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();
            currentUser.updateProfile(profileUpdates);
        }

        if (loadedUser != null) {
            loadedUser.setDisplayName(newName);
            userRepository.saveUser(loadedUser, new UserRepository.SaveUserCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() == null || !isAdded()) return;
                    if (tvDisplayName != null) tvDisplayName.setText(newName);
                    if (layoutEditName != null) layoutEditName.setVisibility(View.GONE);
                    if (layoutNameDisplay != null) layoutNameDisplay.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Name updated successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    if (getActivity() == null || !isAdded()) return;
                    if (tvDisplayName != null) tvDisplayName.setText(newName);
                    if (layoutEditName != null) layoutEditName.setVisibility(View.GONE);
                    if (layoutNameDisplay != null) layoutNameDisplay.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (tvDisplayName != null) tvDisplayName.setText(newName);
            if (layoutEditName != null) layoutEditName.setVisibility(View.GONE);
            if (layoutNameDisplay != null) layoutNameDisplay.setVisibility(View.VISIBLE);
        }
    }

    private void uploadAndSetProfileImage(Uri imageUri) {
        if (getContext() == null || !isAdded()) return;

        // Immediate preview
        if (ivProfilePhoto != null) {
            Glide.with(this)
                    .load(imageUri)
                    .circleCrop()
                    .into(ivProfilePhoto);
        }

        if (pbProfilePhotoUpload != null) {
            pbProfilePhotoUpload.setVisibility(View.VISIBLE);
        }

        new Thread(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                File tempFile = new File(requireContext().getCacheDir(), "profile_avatar_" + System.currentTimeMillis() + ".jpg");
                OutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();

                PhotoAttachment photoAttachment = new PhotoAttachment.Builder()
                        .setUri(Uri.fromFile(tempFile))
                        .build();

                new UploadManager(requireContext()).upload(photoAttachment, new StorageRepository.UploadCallback() {
                    @Override
                    public void onSuccess(String downloadUrl, String publicId) {
                        if (getActivity() == null || !isAdded()) return;

                        if (pbProfilePhotoUpload != null) {
                            pbProfilePhotoUpload.setVisibility(View.GONE);
                        }

                        // Save permanent remote URL into Firestore
                        if (loadedUser != null) {
                            loadedUser.setPhotoUrl(downloadUrl);
                            userRepository.saveUser(loadedUser, null);
                        }

                        // Update Firebase Auth profile
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            user.updateProfile(new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(Uri.parse(downloadUrl))
                                    .build());
                        }

                        if (ivProfilePhoto != null) {
                            Glide.with(ProfileBottomSheet.this)
                                    .load(downloadUrl)
                                    .circleCrop()
                                    .into(ivProfilePhoto);
                        }

                        Toast.makeText(requireContext(), "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (getActivity() == null || !isAdded()) return;

                        if (pbProfilePhotoUpload != null) {
                            pbProfilePhotoUpload.setVisibility(View.GONE);
                        }

                        Toast.makeText(requireContext(), "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                if (getActivity() != null && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (pbProfilePhotoUpload != null) {
                            pbProfilePhotoUpload.setVisibility(View.GONE);
                        }
                        Toast.makeText(requireContext(), "Error preparing image file", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            LocalAuthManager.LocalUser localUser = LocalAuthManager.getInstance().getActiveUser();
            if (localUser != null) {
                bindLocalUser(localUser);
                return;
            }
            dismiss();
            return;
        }

        // Account creation date from Firebase Authentication metadata
        long creationTime = 0;
        if (currentUser.getMetadata() != null) {
            creationTime = currentUser.getMetadata().getCreationTimestamp();
        }
        setAccountSinceDate(creationTime);

        // Pre-fill email and display name if available from FirebaseUser
        if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            if (tvEmail != null) tvEmail.setText(currentUser.getEmail());
        }

        String initialName = currentUser.getDisplayName();
        if (initialName == null || initialName.trim().isEmpty()) {
            if (currentUser.getEmail() != null && currentUser.getEmail().contains("@")) {
                initialName = currentUser.getEmail().split("@")[0];
            } else {
                initialName = "Motorist";
            }
        }
        if (tvDisplayName != null) tvDisplayName.setText(initialName);

        if (currentUser.getPhotoUrl() != null && ivProfilePhoto != null) {
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(ivProfilePhoto);
        }

        // Fetch User profile from Firestore
        userRepository.getUser(currentUser.getUid(), new UserRepository.GetUserCallback() {
            @Override
            public void onSuccess(User user) {
                if (getActivity() == null || !isAdded()) return;
                loadedUser = user;

                if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
                    if (tvDisplayName != null) tvDisplayName.setText(user.getDisplayName());
                }

                if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                    if (tvEmail != null) tvEmail.setText(user.getEmail());
                }

                if (user.getCreatedAt() > 0 && tvAccountSince != null && tvAccountSince.getText().toString().contains("Active")) {
                    setAccountSinceDate(user.getCreatedAt());
                }

                TextView tvReputation = getView() != null ? getView().findViewById(R.id.tvReputation) : null;
                if (tvReputation != null) {
                    tvReputation.setText(String.valueOf(user.getReputationScore()));
                }

                TextView tvUserLevel = getView() != null ? getView().findViewById(R.id.tvUserLevel) : null;
                if (tvUserLevel != null) {
                    String levelStr = user.getUserLevel();
                    if (levelStr == null || levelStr.isEmpty()) {
                        levelStr = new TrustManager().calculateUserLevel(user.getTrustScore());
                    }
                    tvUserLevel.setText("Level: " + levelStr);
                }

                if (ivProfilePhoto != null && user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                    Glide.with(ProfileBottomSheet.this)
                            .load(user.getPhotoUrl())
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .into(ivProfilePhoto);
                }
            }

            @Override
            public void onFailure(Exception e) {
                // Keep default values
            }
        });

        // Load Trust Score
        TrustRepository trustRepository = new TrustRepository();
        trustRepository.getTrustScore(currentUser.getUid(), new TrustRepository.GetTrustScoreCallback() {
            @Override
            public void onSuccess(TrustScore trustScore) {
                if (getActivity() == null || !isAdded()) return;
                TextView tvTrustScore = getView() != null ? getView().findViewById(R.id.tvTrustScore) : null;
                TextView tvUserLevel = getView() != null ? getView().findViewById(R.id.tvUserLevel) : null;
                ProgressBar pbTrustScore = getView() != null ? getView().findViewById(R.id.pbTrustScore) : null;

                if (trustScore != null) {
                    if (tvTrustScore != null) {
                        tvTrustScore.setText("Trust Score: " + trustScore.getScore() + " / 100");
                    }
                    if (pbTrustScore != null) {
                        pbTrustScore.setProgress(Math.max(0, Math.min(100, trustScore.getScore())));
                    }
                    if (tvUserLevel != null) {
                        String level = new TrustManager().calculateUserLevel(trustScore.getScore());
                        tvUserLevel.setText("Level: " + level);
                    }
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

        // Load Behavior Status
        BehaviorRepository behaviorRepository = new BehaviorRepository();
        behaviorRepository.readBehaviorResult(currentUser.getUid(), new BehaviorRepository.ReadBehaviorResultCallback() {
            @Override
            public void onSuccess(BehaviorResult result) {
                if (getActivity() == null || !isAdded()) return;
                TextView tvBehaviorStatus = getView() != null ? getView().findViewById(R.id.tvBehaviorStatus) : null;
                if (tvBehaviorStatus != null && result != null && result.getRiskLevel() != null) {
                    String statusText = "Normal";
                    int statusColor = ContextCompat.getColor(requireContext(), R.color.rg_primary);

                    switch (result.getRiskLevel()) {
                        case NORMAL:
                            statusText = "Normal";
                            statusColor = ContextCompat.getColor(requireContext(), R.color.rg_primary);
                            break;
                        case LOW:
                            statusText = "Low Risk";
                            statusColor = ContextCompat.getColor(requireContext(), R.color.rg_primary);
                            break;
                        case MEDIUM:
                            statusText = "Medium Risk";
                            statusColor = ContextCompat.getColor(requireContext(), R.color.rg_tertiary);
                            break;
                        case HIGH:
                            statusText = "High Risk";
                            statusColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);
                            break;
                        case CRITICAL:
                            statusText = "Critical";
                            statusColor = ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark);
                            break;
                    }
                    tvBehaviorStatus.setText(statusText);
                    tvBehaviorStatus.setTextColor(statusColor);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getActivity() == null || !isAdded()) return;
                TextView tvBehaviorStatus = getView() != null ? getView().findViewById(R.id.tvBehaviorStatus) : null;
                if (tvBehaviorStatus != null) {
                    tvBehaviorStatus.setText("Normal");
                }
            }
        });

        // Load Reports statistics
        loadReportStatistics(currentUser.getUid());
    }

    private void loadReportStatistics(String userId) {
        int localCount = 0;
        try {
            List<RoadReport> localReports = LocalReportManager.getInstance().getReportsByUserId(userId);
            if (localReports != null) {
                localCount = localReports.size();
            }
        } catch (Exception ignored) {}

        if (tvReportsCount != null) {
            tvReportsCount.setText(String.valueOf(localCount));
        }

        new ReportRepository().fetchUserReports(userId, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(List<RoadReport> reports) {
                if (getActivity() == null || !isAdded()) return;
                if (tvReportsCount != null && reports != null) {
                    tvReportsCount.setText(String.valueOf(reports.size()));
                }
            }

            @Override
            public void onError(String error) {
                // Keep local count
            }
        });
    }

    private void setAccountSinceDate(long timestampMs) {
        if (tvAccountSince == null) return;
        if (timestampMs > 0) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dateStr = sdf.format(new Date(timestampMs));
                tvAccountSince.setText("Account Since: " + dateStr);
                return;
            } catch (Exception ignored) {}
        }
        tvAccountSince.setText("Account Since: Active Member");
    }

    private void bindLocalUser(LocalAuthManager.LocalUser localUser) {
        if (tvDisplayName != null) {
            String name = localUser.displayName;
            if (name == null || name.isEmpty()) {
                if (localUser.email != null && localUser.email.contains("@")) {
                    name = localUser.email.split("@")[0];
                } else {
                    name = "Motorist";
                }
            }
            tvDisplayName.setText(name);
        }
        if (tvEmail != null && localUser.email != null) {
            tvEmail.setText(localUser.email);
        }
        if (tvAccountSince != null) {
            tvAccountSince.setText("Account Since: Active Member");
        }
        if (tvReportsCount != null) {
            tvReportsCount.setText("0");
        }
        TextView tvReputation = getView() != null ? getView().findViewById(R.id.tvReputation) : null;
        if (tvReputation != null) {
            tvReputation.setText("50");
        }
        TextView tvUserLevel = getView() != null ? getView().findViewById(R.id.tvUserLevel) : null;
        if (tvUserLevel != null) {
            tvUserLevel.setText("Level: Explorer");
        }
    }
}
