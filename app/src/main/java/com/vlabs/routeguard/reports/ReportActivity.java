package com.vlabs.routeguard.reports;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.vlabs.routeguard.models.PhotoAttachment;
import com.vlabs.routeguard.utils.IntentConstants;
import com.vlabs.routeguard.utils.NetworkUtils;
import com.vlabs.routeguard.viewmodels.ReportCreationViewModel;
import com.vlabs.routeguard.viewmodels.SubmissionState;

import java.io.IOException;

import org.osmdroid.util.GeoPoint;

/**
 * Hazard Report Creation and Submission Activity.
 *
 * Provides the user interface for drafting and submitting a new road hazard report.
 *
 * End-to-End Workflow:
 * 1. Authentication Check:
 *    - Verifies an active session via {@link SessionManager#isLoggedIn()}. Rejects unauthenticated access.
 * 2. Parameter Extraction:
 *    - Extracts latitude, longitude, and hazard category from incoming Intent extras ({@link IntentConstants}).
 * 3. Media Capture & Management:
 *    - Integrates camera photography using {@link CameraCaptureManager} and AndroidX {@link ActivityResultContracts.TakePicture}
 *      backed by an internal {@link androidx.core.content.FileProvider} URI.
 *    - Provides a full-screen image preview, retake option, and photo deletion.
 * 4. Input & Validation:
 *    - Captures category confirmation and optional detailed description text.
 *    - Verifies network availability via {@link NetworkUtils#isNetworkAvailable(android.content.Context)}.
 * 5. Asynchronous Submission Pipeline:
 *    - Disables submit controls synchronously to prevent duplicate double-taps.
 *    - Passes payload to {@link ReportCreationViewModel#submitReport(String)}, which compresses photos
 *      via {@link com.vlabs.routeguard.managers.PhotoCompressionManager}, uploads to Cloudinary via
 *      {@link com.vlabs.routeguard.repositories.StorageRepository}, performs duplicate detection, and
 *      writes the final {@link com.vlabs.routeguard.models.RoadReport} document to Cloud Firestore.
 * 6. State Observation:
 *    - Reacts to {@link SubmissionState} (Idle, Loading, UploadingImage, DuplicateFound, Success, Error).
 */
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
                if (categoryName != null) {
                    com.vlabs.routeguard.models.ReportType category = com.vlabs.routeguard.models.ReportType.valueOf(categoryName);
                    binding.etCategory.setText(category.getDisplayName());
                    viewModel.setCategory(category);
                } else {
                    Toast.makeText(this, "Invalid category.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
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

        binding.etCategory.setOnClickListener(v -> {
            if (isFinishing() || isDestroyed()) return;
            CategorySelectionBottomSheet bottomSheet = CategorySelectionBottomSheet.newInstance();
            bottomSheet.setOnCategorySelectedListener(category -> {
                binding.etCategory.setText(category.getDisplayName());
                viewModel.setCategory(category);
            });
            bottomSheet.show(getSupportFragmentManager(), "CategorySelection");
        });
        
        binding.btnSubmit.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Cannot submit report.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            viewModel.setDescription(binding.etDescription.getText().toString());
            binding.btnSubmit.setEnabled(false); // Synchronously prevent rapid duplicate clicks
            
            String reporterId = SessionManager.getInstance().getCurrentUserId();
            if (reporterId != null && !reporterId.isEmpty()) {
                viewModel.submitReport(reporterId);
            } else {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.cvAddPhoto.setOnClickListener(v -> {
            if (viewModel.hasAttachment()) {
                showPhotoOptionsBottomSheet();
            } else {
                checkAndRequestCameraPermission();
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

    private void checkAndRequestCameraPermission() {
        if (cameraPermissionManager.isCameraPermissionGranted()) {
            prepareCamera();
        } else if (cameraPermissionManager.shouldShowRationale(this)) {
            showPermissionRationaleDialog();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void showPermissionRationaleDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("Camera permission is required to capture photo evidence of road hazards. Please grant permission to attach a photo.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showPermanentlyDeniedDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("Camera Permission Required")
                .setMessage("Camera permission is permanently denied. Please enable Camera permission in App Settings to attach photo evidence.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    cameraPermissionManager.openAppSettings(this);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showPhotoOptionsBottomSheet() {
        if (isFinishing() || isDestroyed()) return;
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
            checkAndRequestCameraPermission();
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
        View cameraIcon = binding.cvAddPhoto.findViewById(R.id.tvPhotoStatus) != null
                ? ((View) binding.cvAddPhoto.findViewById(R.id.tvPhotoStatus).getParent()).findViewById(R.id.tvPhotoStatus)
                : null;
        View promptContainer = binding.cvAddPhoto.getChildCount() > 1 ? binding.cvAddPhoto.getChildAt(1) : binding.cvAddPhoto.getChildAt(0);

        if (viewModel.hasAttachment()) {
            binding.tvPhotoStatus.setText("Photo Attached");
            previewImageView.setImageURI(viewModel.getAttachment().getUri());
            previewImageView.setVisibility(View.VISIBLE);
            if (promptContainer != null) promptContainer.setVisibility(View.GONE);
        } else {
            binding.tvPhotoStatus.setText("No Photo Selected");
            previewImageView.setVisibility(View.GONE);
            previewImageView.setImageDrawable(null);
            if (promptContainer != null) promptContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setupObservers() {
        viewModel.getIsSubmitEnabled().observe(this, isEnabled -> {
            if (viewModel.getSubmissionState().getValue() != com.vlabs.routeguard.viewmodels.SubmissionState.SUBMITTING) {
                binding.btnSubmit.setEnabled(isEnabled);
            }
        });

        viewModel.getSubmissionState().observe(this, state -> {
            if (isFinishing() || isDestroyed()) return;
            switch (state) {
                case SUBMITTING:
                    binding.btnSubmit.setEnabled(false);
                    binding.btnSubmit.setText("Submitting...");
                    break;
                case SUCCESS:
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
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
            if (error != null && !error.isEmpty() && !isFinishing() && !isDestroyed()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupPermissionLaunchers() {
        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isFinishing() || isDestroyed()) return;
            if (isGranted) {
                prepareCamera();
            } else {
                if (cameraPermissionManager.shouldShowRationale(this)) {
                    showPermissionRationaleDialog();
                } else {
                    showPermanentlyDeniedDialog();
                }
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (isFinishing() || isDestroyed()) return;
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
        if (isFinishing() || isDestroyed()) return;
        com.vlabs.routeguard.models.RoadReport duplicate = viewModel.getDuplicateReport();
        if (duplicate == null) return;

        float distance = viewModel.getDuplicateDistanceMeters();
        long ageMs = System.currentTimeMillis() - duplicate.getTimestamp();
        int ageMins = (int) (ageMs / (1000 * 60));
        
        String message = String.format("Category: %s\nAge: %d mins\nConfidence: %d/100\nDistance: %.0f meters",
                duplicate.getType() != null ? duplicate.getType().getDisplayName() : "Hazard",
                ageMins,
                duplicate.getConfidenceScore(),
                distance);

        new AlertDialog.Builder(this)
                .setTitle("Already reported nearby")
                .setMessage(message)
                .setPositiveButton("Confirm Existing", (dialog, which) -> {
                    if (isFinishing() || isDestroyed()) return;
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(IntentConstants.EXTRA_REPORT_ID, duplicate.getReportId());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .setNeutralButton("Create Anyway", (dialog, which) -> {
                    if (isFinishing() || isDestroyed()) return;
                    String reporterId = SessionManager.getInstance().getCurrentUserId();
                    if (reporterId != null && !reporterId.isEmpty()) {
                        binding.btnSubmit.setEnabled(false);
                        binding.btnSubmit.setText("Submitting...");
                        viewModel.submitReportInternal(reporterId, true);
                    } else {
                        Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
