package com.vlabs.routeguard.reports;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
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
import com.vlabs.routeguard.managers.UserLocationManager;
import com.vlabs.routeguard.models.PhotoAttachment;
import com.vlabs.routeguard.viewmodels.ReportCreationViewModel;
import com.vlabs.routeguard.viewmodels.SubmissionState;

import java.io.IOException;
import com.vlabs.routeguard.viewmodels.SubmissionState;

import org.osmdroid.util.GeoPoint;

public class ReportActivity extends BaseActivity {

    private ActivityReportBinding binding;
    private ReportCreationViewModel viewModel;
    private UserLocationManager userLocationManager;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
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
        userLocationManager = new UserLocationManager(this);
        cameraCaptureManager = new CameraCaptureManager(this);
        cameraPermissionManager = new CameraPermissionManager(this);

        setupViews();
        setupObservers();
        setupPermissionLaunchers();
        
        fetchLocation();
    }

    private void setupViews() {
        binding.topAppBar.setNavigationOnClickListener(v -> finish());
        
        binding.btnCancel.setOnClickListener(v -> finish());
        
        binding.btnSubmit.setOnClickListener(v -> {
            viewModel.setDescription(binding.etDescription.getText().toString());
            
            if (SessionManager.getInstance().getCurrentUser() != null) {
                String reporterId = SessionManager.getInstance().getCurrentUser().getUid();
                viewModel.submitReport(reporterId);
            } else {
                Toast.makeText(this, "Session invalid. Please login again.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.etCategory.setOnClickListener(v -> {
            CategorySelectionBottomSheet bottomSheet = CategorySelectionBottomSheet.newInstance();
            bottomSheet.setOnCategorySelectedListener(category -> {
                binding.etCategory.setText(category.getDisplayName());
                viewModel.setCategory(category);
            });
            bottomSheet.show(getSupportFragmentManager(), "CategorySelection");
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
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

            if ((fineLocationGranted != null && fineLocationGranted) || 
                (coarseLocationGranted != null && coarseLocationGranted)) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required to create a report.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

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

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            binding.etLocation.setText("Fetching location...");
            
            userLocationManager.checkAndFetchLocation(new UserLocationManager.LocationCallback() {
                @Override
                public void onLocationFound(GeoPoint point) {
                    String locString = String.format("%.4f, %.4f", point.getLatitude(), point.getLongitude());
                    binding.etLocation.setText(locString);
                    viewModel.setLocation(point);
                }

                @Override
                public void onLocationError(String error) {
                    binding.etLocation.setText("Error fetching location");
                    Toast.makeText(ReportActivity.this, error, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onGpsDisabled() {
                    binding.etLocation.setText("GPS Disabled");
                    showGpsBottomSheet();
                }
            });
        } else {
            locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }
    
    private void showGpsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_gps);

        MaterialButton btnTurnOn = bottomSheetDialog.findViewById(R.id.btnTurnOnGps);
        MaterialButton btnCancel = bottomSheetDialog.findViewById(R.id.btnCancelGps);

        if (btnTurnOn != null) {
            btnTurnOn.setOnClickListener(v -> {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                bottomSheetDialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        }

        bottomSheetDialog.show();
    }
}
