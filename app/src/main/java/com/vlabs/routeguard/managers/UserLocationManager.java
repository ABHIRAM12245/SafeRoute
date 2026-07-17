package com.vlabs.routeguard.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import org.osmdroid.util.GeoPoint;

public class UserLocationManager {

    public interface LocationCallback {
        void onLocationFound(GeoPoint point);
        void onLocationError(String error);
        void onGpsDisabled();
    }

    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationManager systemLocationManager;

    public UserLocationManager(Context context) {
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
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocationFound(new GeoPoint(location.getLatitude(), location.getLongitude()));
                    } else {
                        callback.onLocationError("Unable to retrieve current location.");
                    }
                })
                .addOnFailureListener(e -> callback.onLocationError(e.getMessage()));
    }
}
