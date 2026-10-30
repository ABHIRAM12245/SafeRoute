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
import com.vlabs.routeguard.managers.TravelModeManager;
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

/**
 * Primary Application Dashboard and Interactive Map Activity.
 *
 * Serves as the central hub of SafeRoute, hosting the interactive OpenStreetMap (OSMDroid)
 * canvas, search autocompletion, real-time hazard overlays, bottom navigation, and Travel Mode.
 *
 * Core Subsystems & Execution Flow:
 * 1. OpenStreetMap Configuration:
 *    - Loads tile cache preferences and sets the required custom User-Agent identifier prior to layout inflation.
 * 2. Location & Centering:
 *    - Requests runtime location permissions (FINE & COARSE) via {@link #locationPermissionRequest}.
 *    - Obtains device GPS coordinates via {@link UserLocationManager} and centers the map upon first fix.
 * 3. Real-Time Hazard Observation:
 *    - Observes {@link HomeViewModel#getReports()} LiveData, which queries Cloud Firestore for nearby reports.
 *    - Applies user category and radius filters from {@link FilterPreferences}.
 *    - Delegates marker rendering and click listeners to {@link MapManager}. Clicking a marker opens {@link ReportDetailsBottomSheet}.
 * 4. Place Search & Geocoding:
 *    - Captures search text input with a 400ms debounce to prevent API flooding.
 *    - Dispatches HTTP search queries to the OpenStreetMap Nominatim API via Retrofit in {@link HomeViewModel#searchPlaces(String)}.
 *    - Displays suggestions in a dropdown {@link PlacesAdapter}; selecting a place animates the camera to those coordinates.
 * 5. Report Creation Launch:
 *    - Tapping the hazard report FloatingActionButton checks {@link SessionManager#isLoggedIn()}.
 *    - If unauthenticated, prompts {@link LoginRequiredBottomSheet}; if authenticated, launches {@link ReportActivity}.
 * 6. Travel Mode Proximity Alerts:
 *    - Manages a 3-hour foreground/background proximity alert mode (with 30-minute notification cooldowns per hazard)
 *      coordinated with {@link TravelModeManager} and {@link com.vlabs.routeguard.workers.TravelModeWorker}.
 */
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
        
        // Step 1: Configure OSMDroid configuration and custom User-Agent before inflating map views
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
        setupDrawer();
        setupNotificationBadge();
        
        binding.fabLocation.setOnClickListener(v -> handleLocationClick());
        binding.fabReport.setOnClickListener(v -> handleReportClick());
        binding.btnProfile.setOnClickListener(v -> handleProfileClick());
        if (binding.btnNotification != null) {
            binding.btnNotification.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, com.vlabs.routeguard.notifications.NotificationActivity.class);
                startActivity(intent);
            });
        }
        
        handleLocationClick();
        checkIntentForReport(getIntent());

        if (!NetworkUtils.isNetworkAvailable(this)) {
            com.vlabs.routeguard.utils.SnackbarUtils.showWarning(binding.getRoot(), "📡 You're offline. Reconnect to receive live updates.");
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void setupDrawer() {
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        com.google.android.material.navigation.NavigationView navigationView = findViewById(R.id.navigationView);
        com.google.android.material.appbar.MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        if (navigationView != null) {
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.menu_drawer);
        }

        androidx.appcompat.app.ActionBarDrawerToggle toggle = new androidx.appcompat.app.ActionBarDrawerToggle(
                this, drawerLayout, topAppBar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Close drawer only
            } else if (id == R.id.nav_my_reports) {
                Intent intent = new Intent(HomeActivity.this, com.vlabs.routeguard.activities.MyReportsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_settings) {
                Intent intent = new Intent(HomeActivity.this, com.vlabs.routeguard.settings.SettingsActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_about) {
                Intent intent = new Intent(HomeActivity.this, com.vlabs.routeguard.settings.AboutActivity.class);
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                handleLogoutClick();
            }
            return true;
        });
    }

    private void handleLogoutClick() {
        if (isFinishing() || isDestroyed()) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of SafeRoute?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    Context appContext = getApplicationContext() != null ? getApplicationContext() : this;
                    SessionManager.getInstance().logout(appContext);
                    try {
                        Intent intent = new Intent(appContext, com.vlabs.routeguard.authentication.AuthenticationActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        appContext.startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("HomeActivity", "Error navigating to AuthenticationActivity during logout", e);
                    }
                    if (!isFinishing()) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkIntentForReport(intent);
    }

    private void checkIntentForReport(Intent intent) {
        if (intent != null) {
            String reportId = null;
            if (intent.hasExtra(IntentConstants.EXTRA_REPORT_ID)) {
                reportId = intent.getStringExtra(IntentConstants.EXTRA_REPORT_ID);
            } else if (intent.hasExtra("reportId")) {
                reportId = intent.getStringExtra("reportId");
            } else if (intent.hasExtra("open_report_id")) {
                reportId = intent.getStringExtra("open_report_id");
            }
            if (reportId != null && !reportId.trim().isEmpty()) {
                final String finalReportId = reportId.trim();
                double userLat = Double.NaN;
                double userLon = Double.NaN;
                if (homeViewModel != null && homeViewModel.getCurrentLocation() != null && homeViewModel.getCurrentLocation().getValue() != null) {
                    org.osmdroid.util.GeoPoint loc = homeViewModel.getCurrentLocation().getValue();
                    userLat = loc.getLatitude();
                    userLon = loc.getLongitude();
                }
                if (isFinishing() || isDestroyed()) return;
                java.util.ArrayList<String> ids = new java.util.ArrayList<>();
                ids.add(finalReportId);
                com.vlabs.routeguard.fragments.ReportDetailsBottomSheet bottomSheet = com.vlabs.routeguard.fragments.ReportDetailsBottomSheet.newInstance(ids, userLat, userLon);
                bottomSheet.show(getSupportFragmentManager(), "ReportDetailsBottomSheet");

                // Center map on report coordinates if report details can be fetched
                new com.vlabs.routeguard.repositories.ReportRepository().fetchReportById(finalReportId, new com.vlabs.routeguard.repositories.ReportRepository.SingleReportCallback() {
                    @Override
                    public void onSuccess(RoadReport report) {
                        if (report != null && map != null && !isFinishing() && !isDestroyed()) {
                            runOnUiThread(() -> {
                                if (map != null && map.getController() != null && !isFinishing() && !isDestroyed()) {
                                    map.getController().animateTo(new org.osmdroid.util.GeoPoint(report.getLatitude(), report.getLongitude()));
                                }
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {}
                });
            }
        }
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
        if (isFinishing() || isDestroyed()) return;
        if (getSupportFragmentManager().findFragmentByTag("ProfileBottomSheet") != null) {
            return;
        }
        if (!SessionManager.getInstance().isLoggedIn()) {
            com.vlabs.routeguard.utils.DialogUtils.showLoginRequiredDialog(this, () -> {
                Intent intent = new Intent(HomeActivity.this, com.vlabs.routeguard.authentication.AuthenticationActivity.class);
                startActivity(intent);
            });
            return;
        }
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
        
        searchBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_filter) {
                com.vlabs.routeguard.fragments.FiltersBottomSheet bottomSheet = com.vlabs.routeguard.fragments.FiltersBottomSheet.newInstance(
                        homeViewModel.getSelectedCategories().getValue(),
                        calculateCategoryCounts()
                );
                bottomSheet.setFilterListener(categories -> {
                    filterPreferences.saveFilters(categories);
                    homeViewModel.setSelectedCategories(categories);
                });
                bottomSheet.show(getSupportFragmentManager(), "FiltersBottomSheet");
                return true;
            }
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
            if (point != null && mapManager != null && map != null && !isFinishing() && !isDestroyed()) {
                mapManager.setCurrentLocationMarker(this, point);
                Double radius = homeViewModel.getActiveRadiusKm().getValue();
                mapManager.setLocationAndRadius(point, radius != null ? radius : 5.0);
                
                if (TravelModeManager.getInstance().isTravelModeActive(this)) {
                    String currentUserId = SessionManager.getInstance().getCurrentUser() != null
                            ? SessionManager.getInstance().getCurrentUser().getUid()
                            : "";
                    TravelModeManager.getInstance().evaluateAndNotifyReports(
                            this,
                            point.getLatitude(),
                            point.getLongitude(),
                            homeViewModel.getRoadReports().getValue(),
                            currentUserId
                    );
                }
            }
        });

        homeViewModel.getActiveRadiusKm().observe(this, radius -> {
            if (radius != null && mapManager != null && !isFinishing() && !isDestroyed()) {
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

    private java.util.Map<String, Integer> calculateCategoryCounts() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (ReportType type : ReportType.values()) {
            counts.put(type.name(), 0);
        }
        if (homeViewModel != null && homeViewModel.getRoadReports() != null && homeViewModel.getRoadReports().getValue() != null) {
            List<RoadReport> reports = homeViewModel.getRoadReports().getValue();
            long now = System.currentTimeMillis();
            for (RoadReport report : reports) {
                if (report != null && report.getType() != null) {
                    boolean isExpired = report.getExpiresAt() > 0 && now >= report.getExpiresAt();
                    if (!isExpired) {
                        String catName = report.getType().name();
                        counts.put(catName, counts.getOrDefault(catName, 0) + 1);
                    }
                }
            }
        }
        return counts;
    }

    private void setupMarkers() {
        mapManager.setOnReportClickListener(reportIds -> {
            if (isFinishing() || isDestroyed()) return;
            if (getSupportFragmentManager().findFragmentByTag("ReportDetailsBottomSheet") != null) {
                return;
            }
            org.osmdroid.util.GeoPoint currentLoc = homeViewModel.getCurrentLocation().getValue();
            double userLat = currentLoc != null ? currentLoc.getLatitude() : Double.NaN;
            double userLon = currentLoc != null ? currentLoc.getLongitude() : Double.NaN;
            ReportDetailsBottomSheet bottomSheet = ReportDetailsBottomSheet.newInstance(new java.util.ArrayList<>(reportIds), userLat, userLon);
            bottomSheet.show(getSupportFragmentManager(), "ReportDetailsBottomSheet");
        });

        homeViewModel.getRoadReports().observe(this, reports -> {
            if (mapManager != null && !isFinishing() && !isDestroyed()) {
                mapManager.setReports(reports);
            }
        });

        homeViewModel.getSelectedCategories().observe(this, categories -> {
            if (mapManager != null && !isFinishing() && !isDestroyed()) {
                mapManager.setFilters(categories);
            }
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
        if (isFinishing() || isDestroyed() || map == null || mapManager == null) return;
        try {
            double lat = Double.parseDouble(suggestion.getLat());
            double lon = Double.parseDouble(suggestion.getLon());
            GeoPoint point = new GeoPoint(lat, lon);
            
            double currentZoom = map.getZoomLevelDouble();
            double targetZoom = currentZoom > 15.0 ? currentZoom : 15.0; // Keep current zoom if closer than 15
            
            mapManager.animateTo(point, targetZoom, 800L); // 800ms smooth animation
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.util.Log.e("HomeActivity", "Error animating camera", e);
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
                if (isFinishing() || isDestroyed()) return true;
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
                if (isFinishing() || isDestroyed()) return;
                homeViewModel.setCurrentLocation(point);
                if (!hasCenteredOnUser && mapManager != null) {
                    mapManager.animateTo(point, 17.0, 1000L);
                    hasCenteredOnUser = true;
                }
            }

            @Override
            public void onLocationError(String error) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(HomeActivity.this, error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onGpsDisabled() {
                if (isFinishing() || isDestroyed()) return;
                showGpsBottomSheet();
            }
        });
    }

    private void showGpsBottomSheet() {
        if (isFinishing() || isDestroyed()) return;
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
    
    private com.google.firebase.firestore.ListenerRegistration unreadNotificationListener;

    private void setupNotificationBadge() {
        android.widget.TextView tvBadge = findViewById(R.id.badgeNotification);
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (tvBadge != null) tvBadge.setVisibility(View.GONE);
            return;
        }

        if (unreadNotificationListener != null) {
            unreadNotificationListener.remove();
        }

        unreadNotificationListener = new com.vlabs.routeguard.repositories.NotificationRepository().listenToUnreadCount(currentUser.getUid(), new com.vlabs.routeguard.repositories.NotificationRepository.UnreadCountCallback() {
            @Override
            public void onSuccess(int unreadCount) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvBadge != null) {
                        if (unreadCount > 0) {
                            tvBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                            tvBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvBadge.setVisibility(View.GONE);
                        }
                    }
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvBadge != null) tvBadge.setVisibility(View.GONE);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (unreadNotificationListener != null) {
            unreadNotificationListener.remove();
            unreadNotificationListener = null;
        }
        if (mapManager != null) {
            mapManager.destroy();
            mapManager = null;
        }
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
        super.onDestroy();
    }

    private void setupTravelMode() {
        switchTravelMode = binding.switchTravelMode;
        if (switchTravelMode == null) {
            switchTravelMode = findViewById(R.id.switchTravelMode);
        }

        TravelModeManager travelManager = TravelModeManager.getInstance();
        boolean isActive = travelManager.isTravelModeActive(this);
        updateTravelModeCardUI();

        if (switchTravelMode != null) {
            switchTravelMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    travelManager.enableTravelMode(this);
                    updateTravelModeCardUI();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                        }
                    }
                    startTravelModeLocationUpdates();
                } else {
                    travelManager.disableTravelMode(this);
                    updateTravelModeCardUI();
                    if (userLocationManager != null) {
                        userLocationManager.stopLocationUpdates();
                    }
                }
            });
        }

        if (isActive) {
            startTravelModeLocationUpdates();
        }
    }

    private void updateTravelModeCardUI() {
        TravelModeManager travelManager = TravelModeManager.getInstance();
        boolean isActive = travelManager.isTravelModeActive(this);
        if (switchTravelMode != null) {
            switchTravelMode.setChecked(isActive);
            switchTravelMode.setText(isActive ? "Travel Mode: ON" : "Travel Mode: OFF");
        }
        android.widget.TextView tvTimer = findViewById(R.id.tvTravelModeTimer);
        if (tvTimer != null) {
            if (isActive) {
                long remainingMs = travelManager.getTravelModeExpiresAt(this) - System.currentTimeMillis();
                long mins = Math.max(0, remainingMs / (60 * 1000L));
                long hours = mins / 60;
                long remMins = mins % 60;
                tvTimer.setText(String.format(java.util.Locale.getDefault(), "%dh %02dm left", hours, remMins));
                tvTimer.setVisibility(View.VISIBLE);
            } else {
                tvTimer.setVisibility(View.GONE);
            }
        }
    }

    private void startTravelModeLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            userLocationManager.startLocationUpdates(new UserLocationManager.LocationCallback() {
                @Override
                public void onLocationFound(GeoPoint point) {
                    if (isFinishing() || isDestroyed()) return;
                    TravelModeManager travelManager = TravelModeManager.getInstance();
                    if (!travelManager.isTravelModeActive(HomeActivity.this)) {
                        updateTravelModeCardUI();
                        if (userLocationManager != null) {
                            userLocationManager.stopLocationUpdates();
                        }
                        return;
                    }

                    updateTravelModeCardUI();

                    homeViewModel.setCurrentLocation(point);
                    if (!hasCenteredOnUser && mapManager != null) {
                        mapManager.animateTo(point, 17.0, 1000L);
                        hasCenteredOnUser = true;
                    }

                    // Foreground monitoring while app is open (Refinement #1)
                    String currentUserId = SessionManager.getInstance().getCurrentUser() != null
                            ? SessionManager.getInstance().getCurrentUser().getUid()
                            : "";
                    travelManager.evaluateAndNotifyReports(
                            HomeActivity.this,
                            point.getLatitude(),
                            point.getLongitude(),
                            homeViewModel.getRoadReports().getValue(),
                            currentUserId
                    );
                }

                @Override
                public void onLocationError(String error) {
                }

                @Override
                public void onGpsDisabled() {
                    if (isFinishing() || isDestroyed()) return;
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
}
