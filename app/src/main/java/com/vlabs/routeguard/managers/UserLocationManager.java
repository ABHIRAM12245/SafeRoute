package com.vlabs.routeguard.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.CancellationTokenSource;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Looper;
import androidx.core.app.ActivityCompat;

import org.osmdroid.util.GeoPoint;

public class UserLocationManager {

    public interface LocationCallback {
        void onLocationFound(GeoPoint point);
        void onLocationError(String error);
        void onGpsDisabled();
    }

    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationManager systemLocationManager;
    private CancellationTokenSource cancellationTokenSource;
    private com.google.android.gms.location.LocationCallback locationCallback;
    private final Context context;

    public UserLocationManager(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.systemLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void checkAndFetchLocation(LocationCallback callback) {
        if (systemLocationManager != null && !systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            callback.onGpsDisabled();
        } else {
            fetchCurrentLocation(callback);
        }
    }

    @SuppressLint("MissingPermission") // Checked externally
    private void fetchCurrentLocation(LocationCallback callback) {
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
        }
        cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocationFound(new GeoPoint(location.getLatitude(), location.getLongitude()));
                    } else {
                        callback.onLocationError("Unable to retrieve current location.");
                    }
                })
                .addOnFailureListener(e -> callback.onLocationError(e.getMessage()));
    }

    public void startLocationUpdates(LocationCallback callback, long intervalMs) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onLocationError("Location permission not granted.");
            return;
        }

        if (systemLocationManager != null && !systemLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            callback.onGpsDisabled();
            return;
        }

        stopLocationUpdates(); // Ensure no duplicates

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .build();

        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (android.location.Location location : locationResult.getLocations()) {
                    callback.onLocationFound(new GeoPoint(location.getLatitude(), location.getLongitude()));
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }

    public void cancel() {
        if (cancellationTokenSource != null) {
            cancellationTokenSource.cancel();
            cancellationTokenSource = null;
        }
        stopLocationUpdates();
    }
}
