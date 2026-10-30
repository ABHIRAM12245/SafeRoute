package com.vlabs.routeguard.utils;

import android.location.Location;

/**
 * Geospatial Location Utility.
 *
 * Provides standardized great-circle distance calculations between geographic coordinates
 * (latitude/longitude pairs) using the WGS84 reference ellipsoid.
 *
 * Used across the application for:
 * - Determining whether hazard reports lie within the user's selected search radius in {@link com.vlabs.routeguard.managers.MarkerManager}.
 * - Detecting duplicate nearby reports during submission in {@link com.vlabs.routeguard.managers.LocalReportManager}.
 * - Evaluating proximity triggers for alerts in {@link com.vlabs.routeguard.workers.TravelModeWorker} and {@link com.vlabs.routeguard.managers.TravelModeManager}.
 */
public class LocationUtils {

    /**
     * Calculates the approximate distance in meters between two coordinates.
     *
     * @param startLat Latitude of the starting point in degrees.
     * @param startLon Longitude of the starting point in degrees.
     * @param endLat   Latitude of the destination point in degrees.
     * @param endLon   Longitude of the destination point in degrees.
     * @return The computed distance in meters.
     */
    public static float calculateDistanceMeters(double startLat, double startLon, double endLat, double endLon) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLon, endLat, endLon, results);
        return results[0];
    }

    /**
     * Calculates the approximate distance in kilometers between two coordinates.
     *
     * @param startLat Latitude of the starting point in degrees.
     * @param startLon Longitude of the starting point in degrees.
     * @param endLat   Latitude of the destination point in degrees.
     * @param endLon   Longitude of the destination point in degrees.
     * @return The computed distance in kilometers.
     */
    public static float calculateDistanceKm(double startLat, double startLon, double endLat, double endLon) {
        return calculateDistanceMeters(startLat, startLon, endLat, endLon) / 1000.0f;
    }
}
