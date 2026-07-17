package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.ReportType;
import com.vlabs.routeguard.utils.ReportExpirationConfig;

public class ReportLifecycleManager {

    /**
     * Gets the lifetime of a specific report category in milliseconds.
     *
     * @param type The type of the report.
     * @return The lifetime of the category in milliseconds.
     */
    public long getCategoryLifetime(ReportType type) {
        return ReportExpirationConfig.getExpirationDuration(type);
    }

    /**
     * Calculates the exact expiration timestamp for a report.
     *
     * @param type      The category of the report.
     * @param createdAt The creation timestamp in milliseconds.
     * @return The expiration timestamp in milliseconds.
     */
    public long calculateExpiration(ReportType type, long createdAt) {
        long lifetime = getCategoryLifetime(type);
        return createdAt + lifetime;
    }

    /**
     * Checks if a given expiration timestamp is considered expired based on current time.
     *
     * @param expiresAt The timestamp when the report expires in milliseconds.
     * @return true if the report is expired, false otherwise.
     */
    public boolean isExpired(long expiresAt) {
        return System.currentTimeMillis() > expiresAt;
    }
}
