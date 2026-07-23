package com.vlabs.routeguard.utils;

import android.location.Location;

public class LocationUtils {

    public static float calculateDistanceMeters(double startLat, double startLon, double endLat, double endLon) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLon, endLat, endLon, results);
        return results[0];
    }

    public static float calculateDistanceKm(double startLat, double startLon, double endLat, double endLon) {
        return calculateDistanceMeters(startLat, startLon, endLat, endLon) / 1000.0f;
    }
}
