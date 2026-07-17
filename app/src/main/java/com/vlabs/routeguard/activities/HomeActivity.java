package com.vlabs.routeguard.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.vlabs.routeguard.R;
import com.vlabs.routeguard.adapters.PlacesAdapter;
import com.vlabs.routeguard.databinding.ActivityHomeBinding;
import com.vlabs.routeguard.fragments.ReportDetailsBottomSheet;
import com.vlabs.routeguard.managers.MapManager;
import com.vlabs.routeguard.managers.MarkerManager;
import com.vlabs.routeguard.managers.UserLocationManager;
import com.vlabs.routeguard.models.PlaceSuggestion;
import com.vlabs.routeguard.reports.ReportActivity;
import com.vlabs.routeguard.managers.SessionManager;
import com.vlabs.routeguard.fragments.LoginRequiredBottomSheet;
import com.vlabs.routeguard.utils.NetworkUtils;
import com.vlabs.routeguard.viewmodels.HomeViewModel;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;
    private MapView map;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    
    private HomeViewModel homeViewModel;
    private PlacesAdapter placesAdapter;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private MarkerManager markerManager;
    private MapManager mapManager;
    private UserLocationManager userLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure OSMDroid before inflating the layout
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, android.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
        
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        userLocationManager = new UserLocationManager(this);
        setupMap();
        setupLocationPermissionLauncher();
        setupSearch();
        setupMarkers();
        
        binding.fabLocation.setOnClickListener(v -> handleLocationClick());
        binding.fabReport.setOnClickListener(v -> handleReportClick());
        binding.btnProfile.setOnClickListener(v -> handleProfileClick());
    }

    private void handleReportClick() {
        if (SessionManager.getInstance().isLoggedIn()) {
            startActivity(new Intent(this, ReportActivity.class));
        } else {
            LoginRequiredBottomSheet.newInstance().show(getSupportFragmentManager(), "LoginRequiredBottomSheet");
        }
    }

    private void handleProfileClick() {
        new com.vlabs.routeguard.fragments.ProfileBottomSheet().show(getSupportFragmentManager(), "ProfileBottomSheet");
    }

    private void setupSearch() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        placesAdapter = new PlacesAdapter(suggestion -> {
            hideKeyboard();
            binding.etSearch.clearFocus();
            binding.suggestionsCard.setVisibility(View.GONE);
            homeViewModel.clearSuggestions();
            animateCameraToSuggestion(suggestion);
        });
        
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSuggestions.setAdapter(placesAdapter);
        
        binding.ivFilter.setOnClickListener(v -> {
            com.vlabs.routeguard.fragments.FiltersBottomSheet.newInstance()
                    .show(getSupportFragmentManager(), "FiltersBottomSheet");
        });
        
        searchHandler = new Handler(Looper.getMainLooper());
        
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    binding.suggestionsCard.setVisibility(View.GONE);
                    homeViewModel.clearSuggestions();
                    return;
                }
                
                if (query.length() >= 2) {
                    searchRunnable = () -> homeViewModel.searchPlaces(query, NetworkUtils.isNetworkAvailable(HomeActivity.this));
                    searchHandler.postDelayed(searchRunnable, 300); // 300ms debounce
                }
            }
        });
        
        homeViewModel.getSuggestions().observe(this, suggestions -> {
            if (suggestions != null && !suggestions.isEmpty()) {
                placesAdapter.submitList(suggestions);
                binding.suggestionsCard.setVisibility(View.VISIBLE);
            } else {
                binding.suggestionsCard.setVisibility(View.GONE);
            }
        });
        
        homeViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupMarkers() {
        markerManager = new MarkerManager(this, map);
        markerManager.setOnReportClickListener(report -> {
            ReportDetailsBottomSheet bottomSheet = ReportDetailsBottomSheet.newInstance(
                    report.getReportId(),
                    report.getReporterId(),
                    report.getType(),
                    report.getDescription(),
                    report.getTimestamp()
            );
            bottomSheet.show(getSupportFragmentManager(), "ReportDetailsBottomSheet");
        });

        homeViewModel.getRoadReports().observe(this, reports -> {
            markerManager.setReports(reports);
        });

        homeViewModel.startRoadReportsListener();
    }
    
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    private void animateCameraToSuggestion(PlaceSuggestion suggestion) {
        try {
            double lat = Double.parseDouble(suggestion.getLat());
            double lon = Double.parseDouble(suggestion.getLon());
            GeoPoint point = new GeoPoint(lat, lon);
            
            double currentZoom = map.getZoomLevelDouble();
            double targetZoom = currentZoom > 15.0 ? currentZoom : 15.0; // Keep current zoom if closer than 15
            
            mapManager.animateTo(point, targetZoom, 1000L);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupMap() {
        map = binding.mapView;
        mapManager = new MapManager(map);
        mapManager.initialize();
        mapManager.enableRotation();
        
        mapManager.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629); // Coordinates for India
        mapManager.setCenter(startPoint);
    }
    
    private void setupLocationPermissionLauncher() {
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

            if ((fineLocationGranted != null && fineLocationGranted) || 
                (coarseLocationGranted != null && coarseLocationGranted)) {
                checkGpsAndFetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required to show your current location.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleLocationClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkGpsAndFetchLocation();
        } else {
            locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void checkGpsAndFetchLocation() {
        userLocationManager.checkAndFetchLocation(new UserLocationManager.LocationCallback() {
            @Override
            public void onLocationFound(GeoPoint point) {
                mapManager.animateTo(point, 17.0, 1000L);
            }

            @Override
            public void onLocationError(String error) {
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGpsDisabled() {
                showGpsBottomSheet();
            }
        });
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
    
    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume(); // memory safe implementation
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause(); // memory safe implementation
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (map != null) {
            map.onDetach(); // Clean up to avoid memory leaks
        }
        if (homeViewModel != null) {
            homeViewModel.stopRoadReportsListener();
        }
    }
}
