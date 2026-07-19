package com.vlabs.routeguard.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import android.content.SharedPreferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.HashMap;
import java.util.List;

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
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.reports.ReportActivity;
import com.vlabs.routeguard.managers.SessionManager;
import com.vlabs.routeguard.fragments.LoginRequiredBottomSheet;
import com.vlabs.routeguard.utils.NetworkUtils;
import com.vlabs.routeguard.utils.IntentConstants;
import com.vlabs.routeguard.utils.FilterPreferences;
import com.vlabs.routeguard.viewmodels.HomeViewModel;
import org.osmdroid.events.MapEventsReceiver;
import java.util.Set;

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
    private MapManager mapManager;
    private UserLocationManager userLocationManager;
    private FilterPreferences filterPreferences;
    private boolean hasCenteredOnUser = false;

    private SharedPreferences travelPrefs;
    private boolean isTravelModeON = false;
    private final HashMap<String, Long> lastNotificationTimestamp = new HashMap<>();
    private com.google.android.material.materialswitch.MaterialSwitch switchTravelMode;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private static final String PREFS_NAME = "TravelModePrefs";
    private static final String PREF_IS_TRAVEL_MODE = "isTravelMode";
    private static final String PREF_EXPIRY_TIME = "expiryTime";
    private static final long TRAVEL_MODE_DURATION_MS = 3 * 60 * 60 * 1000L;
    private static final long NOTIFICATION_COOLDOWN_MS = 30 * 60 * 1000L;
    private static final String CHANNEL_ID = "travel_mode_reports";
    private ActivityResultLauncher<Intent> reportActivityLauncher;

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
        filterPreferences = new FilterPreferences(this);

        setupMap();
        setupPermissionLaunchers();
        setupSearch();
        setupMarkers();
        setupTravelMode();
        
        binding.fabLocation.setOnClickListener(v -> handleLocationClick());
        binding.fabReport.setOnClickListener(v -> handleReportClick());
        binding.btnProfile.setOnClickListener(v -> handleProfileClick());
        
        handleLocationClick();
    }

    private void handleReportClick() {
        if (!SessionManager.getInstance().isLoggedIn()) {
            LoginRequiredBottomSheet.newInstance().show(getSupportFragmentManager(), "LoginRequiredBottomSheet");
            return;
        }
        
        GeoPoint currentLoc = homeViewModel.getCurrentLocation().getValue();
        if (currentLoc != null) {
            showCategorySelectionForLocation(currentLoc);
        } else {
            Toast.makeText(this, "Location not available. Long press on the map to place a report.", Toast.LENGTH_LONG).show();
        }
    }

    private void showCategorySelectionForLocation(GeoPoint point) {
        com.vlabs.routeguard.fragments.CategorySelectionBottomSheet bottomSheet = com.vlabs.routeguard.fragments.CategorySelectionBottomSheet.newInstance();
        bottomSheet.setOnCategorySelectedListener(category -> {
            Intent intent = new Intent(HomeActivity.this, ReportActivity.class);
            intent.putExtra(IntentConstants.EXTRA_LATITUDE, point.getLatitude());
            intent.putExtra(IntentConstants.EXTRA_LONGITUDE, point.getLongitude());
            intent.putExtra(IntentConstants.EXTRA_CATEGORY, category.name());
            if (reportActivityLauncher != null) {
                reportActivityLauncher.launch(intent);
            } else {
                startActivity(intent);
            }
        });
        bottomSheet.show(getSupportFragmentManager(), "CategorySelection");
    }

    private void handleProfileClick() {
        new com.vlabs.routeguard.fragments.ProfileBottomSheet().show(getSupportFragmentManager(), "ProfileBottomSheet");
    }

    private void setupSearch() {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        
        Set<String> savedFilters = filterPreferences.loadFilters();
        android.util.Log.e("MARKER_TEST", "savedFilters = " + savedFilters);
        homeViewModel.setSelectedCategories(savedFilters);
        
        com.google.android.material.search.SearchBar searchBar = findViewById(R.id.searchBar);
        com.google.android.material.search.SearchView searchView = findViewById(R.id.searchView);
        View llSearchState = findViewById(R.id.llSearchState);
        View progressSearch = findViewById(R.id.progressSearch);
        android.widget.TextView tvSearchState = findViewById(R.id.tvSearchState);
        androidx.recyclerview.widget.RecyclerView rvSuggestions = findViewById(R.id.rvSuggestions);

        placesAdapter = new PlacesAdapter(suggestion -> {
            hideKeyboard();
            searchView.hide();
            
            if (suggestion.getLat() == null) {
                // Clear History action
                saveSearchHistory(new ArrayList<>());
                showSearchHistory();
                return;
            }
            
            addToSearchHistory(suggestion);
            searchBar.setText(suggestion.getDisplayName());
            animateCameraToSuggestion(suggestion);
        });
        
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestions.setAdapter(placesAdapter);
        
        // Add Filter button to MaterialToolbar
        com.google.android.material.appbar.MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(item -> {
            // Alternatively handle via a direct button. Since we removed ivFilter, let's just trigger it from the profile/notification layout
            return false;
        });
        
        searchHandler = new Handler(Looper.getMainLooper());
        
        searchView.getEditText().addTextChangedListener(new TextWatcher() {
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
                    showSearchHistory();
                    llSearchState.setVisibility(View.GONE);
                    return;
                }
                
                if (query.length() >= 2) {
                    searchRunnable = () -> homeViewModel.searchPlaces(query, NetworkUtils.isNetworkAvailable(HomeActivity.this));
                    searchHandler.postDelayed(searchRunnable, 500); // 500ms debounce
                }
            }
        });
        
        searchView.addTransitionListener((sv, previousState, newState) -> {
            if (newState == com.google.android.material.search.SearchView.TransitionState.SHOWN) {
                if (searchView.getText().toString().trim().isEmpty()) {
                    showSearchHistory();
                }
            }
        });
        
        homeViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                llSearchState.setVisibility(View.VISIBLE);
                progressSearch.setVisibility(View.VISIBLE);
                tvSearchState.setVisibility(View.GONE);
                placesAdapter.submitList(new ArrayList<>()); // clear list while loading
            } else {
                progressSearch.setVisibility(View.GONE);
            }
        });

        homeViewModel.getSuggestions().observe(this, suggestions -> {
            Boolean isLoading = homeViewModel.getIsLoading().getValue();
            if (isLoading != null && isLoading) return; // Wait for loading to finish

            if (suggestions != null && !suggestions.isEmpty()) {
                placesAdapter.submitList(suggestions);
                llSearchState.setVisibility(View.GONE);
            } else if (searchView.getText().toString().trim().length() >= 2) {
                placesAdapter.submitList(new ArrayList<>());
                llSearchState.setVisibility(View.VISIBLE);
                tvSearchState.setVisibility(View.VISIBLE);
                tvSearchState.setText("No places found");
            }
        });
        
        homeViewModel.getError().observe(this, error -> {
            Boolean isLoading = homeViewModel.getIsLoading().getValue();
            if (isLoading != null && isLoading) return;

            if (error != null && !error.isEmpty()) {
                placesAdapter.submitList(new ArrayList<>());
                llSearchState.setVisibility(View.VISIBLE);
                tvSearchState.setVisibility(View.VISIBLE);
                tvSearchState.setText(error);
            }
        });

        homeViewModel.getCurrentLocation().observe(this, point -> {
            if (point != null) {
                mapManager.setCurrentLocationMarker(this, point);
                Double radius = homeViewModel.getActiveRadiusKm().getValue();
                mapManager.setLocationAndRadius(point, radius != null ? radius : 5.0);
                
                if (isTravelModeON) {
                    checkNearbyReports(point);
                }
            }
        });

        homeViewModel.getActiveRadiusKm().observe(this, radius -> {
            if (radius != null) {
                GeoPoint point = homeViewModel.getCurrentLocation().getValue();
                mapManager.setLocationAndRadius(point, radius);
            }
        });
    }

    private void showSearchHistory() {
        List<PlaceSuggestion> history = getSearchHistory();
        if (!history.isEmpty()) {
            PlaceSuggestion clearItem = new PlaceSuggestion();
            clearItem.setDisplayName("Clear Recent Searches");
            clearItem.setLat(null);
            clearItem.setLon(null);
            
            List<PlaceSuggestion> displayList = new ArrayList<>(history);
            displayList.add(clearItem);
            placesAdapter.submitList(displayList);
        } else {
            placesAdapter.submitList(new ArrayList<>());
        }
    }

    private List<PlaceSuggestion> getSearchHistory() {
        SharedPreferences prefs = getSharedPreferences("SearchHistoryPrefs", MODE_PRIVATE);
        String json = prefs.getString("history", "[]");
        Type type = new TypeToken<List<PlaceSuggestion>>(){}.getType();
        List<PlaceSuggestion> history = new Gson().fromJson(json, type);
        return history == null ? new ArrayList<>() : history;
    }

    private void saveSearchHistory(List<PlaceSuggestion> history) {
        SharedPreferences prefs = getSharedPreferences("SearchHistoryPrefs", MODE_PRIVATE);
        String json = new Gson().toJson(history);
        prefs.edit().putString("history", json).apply();
    }

    private void addToSearchHistory(PlaceSuggestion newSuggestion) {
        List<PlaceSuggestion> history = getSearchHistory();
        
        // Remove duplicate if exists
        Iterator<PlaceSuggestion> iterator = history.iterator();
        while (iterator.hasNext()) {
            PlaceSuggestion s = iterator.next();
            if (s.getDisplayName().equals(newSuggestion.getDisplayName())) {
                iterator.remove();
                break;
            }
        }
        
        // Add to top
        history.add(0, newSuggestion);
        
        // Keep only last 10
        if (history.size() > 10) {
            history = history.subList(0, 10);
        }
        
        saveSearchHistory(history);
    }

    
    private void setupMarkers() {
        mapManager.setOnReportClickListener(reportId -> {
            org.osmdroid.util.GeoPoint currentLoc = homeViewModel.getCurrentLocation().getValue();
            double userLat = currentLoc != null ? currentLoc.getLatitude() : Double.NaN;
            double userLon = currentLoc != null ? currentLoc.getLongitude() : Double.NaN;
            ReportDetailsBottomSheet bottomSheet = ReportDetailsBottomSheet.newInstance(reportId, userLat, userLon);
            bottomSheet.show(getSupportFragmentManager(), "ReportDetailsBottomSheet");
        });

        homeViewModel.getRoadReports().observe(this, reports -> {
            mapManager.setReports(reports);
        });

        homeViewModel.getSelectedCategories().observe(this, categories -> {
            mapManager.setFilters(categories);
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
            
            mapManager.animateTo(point, targetZoom, 800L); // 800ms smooth animation
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupMap() {
        map = binding.mapView;
        mapManager = new MapManager(this, map);
        mapManager.initialize();
        mapManager.enableRotation();
        
        mapManager.setZoom(15.0);
        GeoPoint startPoint = new GeoPoint(20.5937, 78.9629); // Coordinates for India
        mapManager.setCenter(startPoint);
        
        mapManager.setOnMapLongClickListener(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (!SessionManager.getInstance().isLoggedIn()) {
                    LoginRequiredBottomSheet.newInstance().show(getSupportFragmentManager(), "LoginRequiredBottomSheet");
                    return true;
                }
                showCategorySelectionForLocation(p);
                return true;
            }
        });
    }
    
    private void setupPermissionLaunchers() {
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

            if ((fineLocationGranted != null && fineLocationGranted) || 
                (coarseLocationGranted != null && coarseLocationGranted)) {
                checkGpsAndFetchLocation();
                if (isTravelModeON) {
                    startTravelModeLocationUpdates();
                }
            } else {
                Toast.makeText(this, "Location permission is required to show your current location.", Toast.LENGTH_SHORT).show();
            }
        });

        notificationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) {
                Toast.makeText(this, "Notifications disabled. You won't receive nearby alerts.", Toast.LENGTH_SHORT).show();
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
                homeViewModel.setCurrentLocation(point);
                if (!hasCenteredOnUser) {
                    mapManager.animateTo(point, 17.0, 1000L);
                    hasCenteredOnUser = true;
                }
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
        if (userLocationManager != null) {
            userLocationManager.cancel();
        }
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    private void setupTravelMode() {
        travelPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        switchTravelMode = binding.switchTravelMode;
        if (switchTravelMode == null) {
            switchTravelMode = findViewById(R.id.switchTravelMode);
        }
        createNotificationChannel();
        checkTravelModeExpiry();

        if (switchTravelMode != null) {
            switchTravelMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    enableTravelMode();
                } else {
                    disableTravelMode();
                }
            });
        }
    }

    private void checkTravelModeExpiry() {
        isTravelModeON = travelPrefs.getBoolean(PREF_IS_TRAVEL_MODE, false);
        long expiryTime = travelPrefs.getLong(PREF_EXPIRY_TIME, 0);

        if (isTravelModeON) {
            if (System.currentTimeMillis() > expiryTime) {
                disableTravelMode();
            } else {
                if (switchTravelMode != null) {
                    switchTravelMode.setChecked(true);
                    switchTravelMode.setText("Travel Mode: ON");
                }
                startTravelModeLocationUpdates();
            }
        } else {
            if (switchTravelMode != null) {
                switchTravelMode.setChecked(false);
                switchTravelMode.setText("Travel Mode: OFF");
            }
        }
    }

    private void enableTravelMode() {
        isTravelModeON = true;
        long expiryTime = System.currentTimeMillis() + TRAVEL_MODE_DURATION_MS;
        travelPrefs.edit()
            .putBoolean(PREF_IS_TRAVEL_MODE, true)
            .putLong(PREF_EXPIRY_TIME, expiryTime)
            .apply();
        
        if (switchTravelMode != null) {
            switchTravelMode.setText("Travel Mode: ON");
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        startTravelModeLocationUpdates();
    }

    private void disableTravelMode() {
        isTravelModeON = false;
        travelPrefs.edit()
            .putBoolean(PREF_IS_TRAVEL_MODE, false)
            .remove(PREF_EXPIRY_TIME)
            .apply();
        
        if (switchTravelMode != null) {
            switchTravelMode.setChecked(false);
            switchTravelMode.setText("Travel Mode: OFF");
        }
        if (userLocationManager != null) {
            userLocationManager.stopLocationUpdates();
        }
    }

    private void startTravelModeLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            userLocationManager.startLocationUpdates(new UserLocationManager.LocationCallback() {
                @Override
                public void onLocationFound(GeoPoint point) {
                    homeViewModel.setCurrentLocation(point);
                    if (!hasCenteredOnUser) {
                        mapManager.animateTo(point, 17.0, 1000L);
                        hasCenteredOnUser = true;
                    }
                }
                @Override
                public void onLocationError(String error) {
                    // Silent ignore or small toast
                }
                @Override
                public void onGpsDisabled() {
                    showGpsBottomSheet();
                }
            }, 5000); // 5 seconds interval
        } else {
            locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void checkNearbyReports(GeoPoint currentLoc) {
        List<RoadReport> reports = homeViewModel.getRoadReports().getValue();
        if (reports == null || reports.isEmpty()) return;

        long currentTime = System.currentTimeMillis();

        for (RoadReport report : reports) {
            if (report.getExpiresAt() <= currentTime) continue;

            float[] results = new float[1];
            android.location.Location.distanceBetween(currentLoc.getLatitude(), currentLoc.getLongitude(), report.getLatitude(), report.getLongitude(), results);
            float distanceMeters = results[0];

            if (distanceMeters <= 500) {
                Long lastNotified = lastNotificationTimestamp.get(report.getReportId());
                if (lastNotified == null || (currentTime - lastNotified > NOTIFICATION_COOLDOWN_MS)) {
                    if (sendNearbyNotification(report, distanceMeters)) {
                        lastNotificationTimestamp.put(report.getReportId(), currentTime);
                    }
                }
            }
        }
    }

    private boolean sendNearbyNotification(RoadReport report, float distanceMeters) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        String title = getNotificationTitleForType(report.getType());
        String body = String.format(java.util.Locale.getDefault(), "Report detected %.0f meters ahead. Tap to open RouteGuard.", distanceMeters);

        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("open_report_id", report.getReportId());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, report.getReportId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_report)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(report.getReportId().hashCode(), builder.build());
        return true;
    }

    private String getNotificationTitleForType(ReportType type) {
        switch (type) {
            case POLICE_CHECKING: return "Police Checking Ahead";
            case ACCIDENT: return "Accident Ahead";
            case ROAD_CONSTRUCTION: return "Construction Ahead";
            case HEAVY_TRAFFIC: return "Heavy Traffic Ahead";
            case ANIMAL_ON_ROAD: return "Animal on Road Ahead";
            case WATER_LOGGING: return "Water Logging Ahead";
            default: return "Road Block Ahead";
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Travel Mode Alerts";
            String description = "Notifications for nearby reports while in Travel Mode";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        if (mapManager != null) {
            mapManager.destroy();
        }
        super.onDestroy();
    }
}
