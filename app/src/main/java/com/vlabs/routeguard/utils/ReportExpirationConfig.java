package com.vlabs.routeguard.utils;

import com.vlabs.routeguard.models.ReportType;

public class ReportExpirationConfig {

    // Default lifetimes in milliseconds
    private static final long DEFAULT_LIFETIME_MS = 2 * 60 * 60 * 1000L; // 2 hours
    private static final long ACCIDENT_LIFETIME_MS = 6 * 60 * 60 * 1000L; // 6 hours
    private static final long HEAVY_TRAFFIC_LIFETIME_MS = 1 * 60 * 60 * 1000L; // 1 hour
    private static final long ROAD_CONSTRUCTION_LIFETIME_MS = 365L * 24 * 60 * 60 * 1000L; // Long duration (1 year)
    private static final long POLICE_CHECKING_LIFETIME_MS = 2 * 60 * 60 * 1000L; // 2 hours
    private static final long ANIMAL_ON_ROAD_LIFETIME_MS = 2 * 60 * 60 * 1000L; // 2 hours
    private static final long WATER_LOGGING_LIFETIME_MS = 2 * 60 * 60 * 1000L; // 2 hours
    private static final long OTHER_LIFETIME_MS = 2 * 60 * 60 * 1000L; // 2 hours

    /**
     * Gets the expiration duration of a specific report category in milliseconds.
     *
     * @param type The type of the report.
     * @return The lifetime of the category in milliseconds.
     */
    public static long getExpirationDuration(ReportType type) {
        if (type == null) {
            return DEFAULT_LIFETIME_MS;
        }

        switch (type) {
            case ACCIDENT:
                return ACCIDENT_LIFETIME_MS;
            case HEAVY_TRAFFIC:
                return HEAVY_TRAFFIC_LIFETIME_MS;
            case ROAD_CONSTRUCTION:
                return ROAD_CONSTRUCTION_LIFETIME_MS;
            case POLICE_CHECKING:
                return POLICE_CHECKING_LIFETIME_MS;
            case ANIMAL_ON_ROAD:
                return ANIMAL_ON_ROAD_LIFETIME_MS;
            case WATER_LOGGING:
                return WATER_LOGGING_LIFETIME_MS;
            case OTHER:
                return OTHER_LIFETIME_MS;
            default:
                return DEFAULT_LIFETIME_MS;
        }
    }
}
