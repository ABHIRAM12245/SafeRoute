package com.vlabs.routeguard.reports;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.activities.BaseActivity;
import com.vlabs.routeguard.databinding.ActivityReportBinding;
import com.vlabs.routeguard.fragments.CategorySelectionBottomSheet;
import com.vlabs.routeguard.managers.CameraCaptureManager;
import com.vlabs.routeguard.managers.CameraPermissionManager;
import com.vlabs.routeguard.managers.SessionManager;
import com.vlabs.routeguard.managers.CameraPermissionManager;
import com.vlabs.routeguard.managers.SessionManager;
import com.vlabs.routeguard.models.PhotoAttachment;
import com.vlabs.routeguard.utils.IntentConstants;
import com.vlabs.routeguard.utils.NetworkUtils;
import com.vlabs.routeguard.viewmodels.ReportCreationViewModel;
import com.vlabs.routeguard.viewmodels.SubmissionState;

import java.io.IOException;
import com.vlabs.routeguard.viewmodels.SubmissionState;

import org.osmdroid.util.GeoPoint;

public class ReportActivity extends BaseActivity {

    private ActivityReportBinding binding;
    private ReportCreationViewModel viewModel;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private CameraCaptureManager cameraCaptureManager;
    private CameraPermissionManager cameraPermissionManager;
    private ImageView previewImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!SessionManager.getInstance().isLoggedIn()) {
            Toast.makeText(this, "Session expired or invalid.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ReportCreationViewModel.class);
        cameraCaptureManager = new CameraCaptureManager(this);
        cameraPermissionManager = new CameraPermissionManager(this);

        setupViews();
        setupObservers();
        setupPermissionLaunchers();
        
        parseIntentExtras();
    }

    private void parseIntentExtras() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(IntentConstants.EXTRA_LATITUDE) && intent.hasExtra(IntentConstants.EXTRA_LONGITUDE) && intent.hasExtra(IntentConstants.EXTRA_CATEGORY)) {
            double lat = intent.getDoubleExtra(IntentConstants.EXTRA_LATITUDE, 0);
            double lon = intent.getDoubleExtra(IntentConstants.EXTRA_LONGITUDE, 0);
            String categoryName = intent.getStringExtra(IntentConstants.EXTRA_CATEGORY);
            
            GeoPoint point = new GeoPoint(lat, lon);
            String locString = String.format("%.4f, %.4f", lat, lon);
            binding.etLocation.setText(locString);
            viewModel.setLocation(point);

            try {
                com.vlabs.routeguard.models.ReportType category = com.vlabs.routeguard.models.ReportType.valueOf(categoryName);
                binding.etCategory.setText(category.getDisplayName());
                viewModel.setCategory(category);
            } catch (IllegalArgumentException e) {
                Toast.makeText(this, "Invalid category.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Missing location or category data.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupViews() {
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
        
        binding.btnCancel.setOnClickListener(v -> finish());
        
        binding.btnSubmit.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Cannot submit report.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            viewModel.setDescription(binding.etDescription.getText().toString());
            
            if (SessionManager.getInstance().getCurrentUser() != null) {
                String reporterId = SessionManager.getInstance().getCurrentUser().getUid();
                viewModel.submitReport(reporterId);
            } else {
                Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.cvAddPhoto.setOnClickListener(v -> {
            if (viewModel.hasAttachment()) {
                showPhotoOptionsBottomSheet();
            } else {
                if (!cameraPermissionManager.isCameraPermissionGranted()) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                } else {
                    prepareCamera();
                }
            }
        });

        previewImageView = new ImageView(this);
        previewImageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        previewImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewImageView.setVisibility(View.GONE);
        binding.cvAddPhoto.addView(previewImageView, 0);

        updatePhotoAttachmentStatus();
    }

    private void showPhotoOptionsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        android.widget.TextView tvRetake = new android.widget.TextView(this);
        tvRetake.setText("Retake Photo");
        tvRetake.setTextSize(18);
        tvRetake.setPadding(0, padding / 2, 0, padding / 2);
        tvRetake.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            cameraCaptureManager.deleteTempPhoto();
            if (!cameraPermissionManager.isCameraPermissionGranted()) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                prepareCamera();
            }
        });

        android.widget.TextView tvRemove = new android.widget.TextView(this);
        tvRemove.setText("Remove Photo");
        tvRemove.setTextSize(18);
        tvRemove.setPadding(0, padding / 2, 0, padding / 2);
        tvRemove.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            cameraCaptureManager.deleteTempPhoto();
            viewModel.clearAttachment();
            updatePhotoAttachmentStatus();
        });

        android.widget.TextView tvCancel = new android.widget.TextView(this);
        tvCancel.setText("Cancel");
        tvCancel.setTextSize(18);
        tvCancel.setPadding(0, padding / 2, 0, padding / 2);
        tvCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        layout.addView(tvRetake);
        layout.addView(tvRemove);
        layout.addView(tvCancel);

        bottomSheetDialog.setContentView(layout);
        bottomSheetDialog.show();
    }

    private void prepareCamera() {
        try {
            cameraCaptureManager.createTempPhoto();
            android.net.Uri tempUri = cameraCaptureManager.getTempPhotoUri();
            try {
                cameraLauncher.launch(tempUri);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open camera.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Unable to prepare camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePhotoAttachmentStatus() {
        android.widget.LinearLayout ll = (android.widget.LinearLayout) binding.cvAddPhoto.getChildAt(1);
        View cameraIcon = ll.getChildAt(0);

        if (viewModel.hasAttachment()) {
            binding.tvPhotoStatus.setText("Photo Attached");
            previewImageView.setImageURI(viewModel.getAttachment().getUri());
            previewImageView.setVisibility(View.VISIBLE);
            cameraIcon.setVisibility(View.GONE);
        } else {
            binding.tvPhotoStatus.setText("No Photo Selected");
            previewImageView.setVisibility(View.GONE);
            previewImageView.setImageDrawable(null);
            cameraIcon.setVisibility(View.VISIBLE);
        }
    }

    private void setupObservers() {
        viewModel.getIsSubmitEnabled().observe(this, isEnabled -> {
            binding.btnSubmit.setEnabled(isEnabled);
        });

        viewModel.getSubmissionState().observe(this, state -> {
            switch (state) {
                case SUBMITTING:
                    binding.btnSubmit.setEnabled(false);
                    binding.btnSubmit.setText("Submitting...");
                    break;
                case SUCCESS:
                    finish(); // Simply close, let HomeActivity's listener show marker
                    break;
                case ERROR:
                    binding.btnSubmit.setEnabled(true);
                    binding.btnSubmit.setText("Submit Report");
                    break;
                case DUPLICATE_FOUND:
                    binding.btnSubmit.setEnabled(true);
                    binding.btnSubmit.setText("Submit Report");
                    showDuplicateDialog();
                    break;
                case IDLE:
                case VALIDATING:
                default:
                    binding.btnSubmit.setText("Submit Report");
                    break;
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPermissionLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                prepareCamera();
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                android.net.Uri tempUri = cameraCaptureManager.getTempPhotoUri();
                if (tempUri != null) {
                    PhotoAttachment attachment = new PhotoAttachment.Builder().setUri(tempUri).build();
                    viewModel.setAttachment(attachment);
                }
            } else {
                cameraCaptureManager.deleteTempPhoto();
                viewModel.clearAttachment();
            }
            updatePhotoAttachmentStatus();
        });
    }

    private void showDuplicateDialog() {
        com.vlabs.routeguard.models.RoadReport duplicate = viewModel.getDuplicateReport();
        if (duplicate == null) return;

        float distance = viewModel.getDuplicateDistanceMeters();
        long ageMs = System.currentTimeMillis() - duplicate.getTimestamp();
        int ageMins = (int) (ageMs / (1000 * 60));
        
        String message = String.format("Category: %s\nAge: %d mins\nConfidence: %d/100\nDistance: %.0f meters",
                duplicate.getType().getDisplayName(),
                ageMins,
                duplicate.getConfidenceScore(),
                distance);

        new AlertDialog.Builder(this)
                .setTitle("Already reported nearby")
                .setMessage(message)
                .setPositiveButton("Confirm Existing", (dialog, which) -> {
                    // Tell HomeActivity to select this marker by returning it in the intent
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(IntentConstants.EXTRA_REPORT_ID, duplicate.getReportId());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .setNeutralButton("Create Anyway", (dialog, which) -> {
                    String reporterId = SessionManager.getInstance().getCurrentUser().getUid();
                    viewModel.submitReportInternal(reporterId, true);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
