package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.RoadReport;

/**
 * Road Hazard Reliability and Confidence Scoring Engine.
 *
 * Computes a dynamic composite reliability score (0-100) and confidence tier for road reports
 * by evaluating community signals, peer confirmations, and temporal decay.
 *
 * Scoring Algorithm & Weights:
 * - Baseline: 50 points.
 * - Upvotes & Downvotes: +5 points per upvote; -5 points per downvote.
 * - Peer Confirmations: +15 points per active ("Still Exists") confirmation; -15 points per gone ("Cleared") confirmation.
 * - Age Decay: -1 point per elapsed hour since creation.
 * - Expiry Decay: -5 points if remaining lifetime is under 30 minutes.
 * - Range Clamping: Bound within [0, 100].
 *
 * Confidence Tiers:
 * - HIGH: score >= 70
 * - MEDIUM: 20 <= score < 70
 * - LOW: score < 20
 */
public class ConfidenceEngine {

    public enum ConfidenceLevel {
        HIGH,
        MEDIUM,
        LOW
    }

    public static int calculateConfidence(RoadReport report) {
        if (report == null) return 0;

        int confidence = 50; // Baseline

        // Votes
        confidence += (report.getUpvotes() * 5);
        confidence -= (report.getDownvotes() * 5);

        // Confirmations
        confidence += (report.getActiveConfirmations() * 15);
        confidence -= (report.getGoneConfirmations() * 15);

        // Age
        long ageMs = System.currentTimeMillis() - report.getTimestamp();
        int ageHours = (int) (ageMs / (1000 * 60 * 60));
        confidence -= ageHours; // -1 for every hour old

        // Expiry
        long timeToExpiryMs = report.getExpiresAt() - System.currentTimeMillis();
        if (timeToExpiryMs < 30 * 60 * 1000) { // < 30 mins
            confidence -= 5;
        }

        // Clamp 0-100
        if (confidence > 100) confidence = 100;
        if (confidence < 0) confidence = 0;

        return confidence;
    }

    public static ConfidenceLevel getConfidenceLevel(int confidence) {
        if (confidence >= 70) {
            return ConfidenceLevel.HIGH;
        } else if (confidence >= 20) {
            return ConfidenceLevel.MEDIUM;
        } else {
            return ConfidenceLevel.LOW;
        }
    }
}
