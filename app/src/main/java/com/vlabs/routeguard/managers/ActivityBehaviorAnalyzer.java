package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.BehaviorMetrics;
import com.vlabs.routeguard.models.BehaviorResult;
import com.vlabs.routeguard.models.BehaviorRiskLevel;
import com.vlabs.routeguard.utils.BehaviorConfig;

import java.util.ArrayList;

/**
 * Independent analyzer responsible for evaluating activity-related behavior.
 * This analyzer only analyzes activity metrics and returns an independent BehaviorResult.
 *
 * Future detection modules to be implemented:
 * - High Daily Activity
 * - Excessive Voting Frequency
 * - Excessive Reporting Frequency
 * - Bot-like Activity
 * - Abnormal Usage Pattern
 */
public class ActivityBehaviorAnalyzer {

    public ActivityBehaviorAnalyzer() {
        // Default constructor
    }

    /**
     * Evaluates activity behavior metrics.
     *
     * @param metrics The metrics to evaluate.
     * @return An independent BehaviorResult based on activity behavior.
     */
    public BehaviorResult analyze(BehaviorMetrics metrics) {
        if (metrics == null) {
            return new BehaviorResult(
                    BehaviorConfig.MIN_RISK_SCORE,
                    BehaviorRiskLevel.NORMAL,
                    new ArrayList<>(),
                    "No suspicious activity detected."
            );
        }

        int riskScore = BehaviorConfig.MIN_RISK_SCORE;
        java.util.List<com.vlabs.routeguard.models.BehaviorFlag> flags = new ArrayList<>();
        java.util.Set<String> summaries = new java.util.LinkedHashSet<>();

        if (metrics.getVotesToday() > BehaviorConfig.MAX_VOTES_PER_DAY) {
            if (!flags.contains(com.vlabs.routeguard.models.BehaviorFlag.HIGH_ACTIVITY)) {
                flags.add(com.vlabs.routeguard.models.BehaviorFlag.HIGH_ACTIVITY);
            }
            riskScore += 20;
            if (riskScore > BehaviorConfig.MAX_RISK_SCORE) {
                riskScore = BehaviorConfig.MAX_RISK_SCORE;
            }
            summaries.add("Excessive voting activity detected.");
        }

        if (metrics.getReportsToday() > BehaviorConfig.MAX_REPORTS_PER_DAY) {
            if (!flags.contains(com.vlabs.routeguard.models.BehaviorFlag.HIGH_ACTIVITY)) {
                flags.add(com.vlabs.routeguard.models.BehaviorFlag.HIGH_ACTIVITY);
            }
            riskScore += 20;
            if (riskScore > BehaviorConfig.MAX_RISK_SCORE) {
                riskScore = BehaviorConfig.MAX_RISK_SCORE;
            }
            summaries.add("Excessive reporting activity detected.");
        }

        if (metrics.getAccountAgeDays() <= 7 && 
            (metrics.getVotesToday() > BehaviorConfig.MAX_VOTES_PER_DAY || metrics.getReportsToday() > BehaviorConfig.MAX_REPORTS_PER_DAY)) {
            if (!flags.contains(com.vlabs.routeguard.models.BehaviorFlag.NEW_ACCOUNT_ABUSE)) {
                flags.add(com.vlabs.routeguard.models.BehaviorFlag.NEW_ACCOUNT_ABUSE);
            }
            riskScore += 25;
            if (riskScore > BehaviorConfig.MAX_RISK_SCORE) {
                riskScore = BehaviorConfig.MAX_RISK_SCORE;
            }
            summaries.add("New account showing suspicious activity.");
        }

        String summary = "No suspicious activity detected.";
        if (!summaries.isEmpty()) {
            summary = String.join(" ", summaries);
        }

        // TODO: Implement future detection modules:
        // - Excessive Voting Frequency
        // - Excessive Reporting Frequency
        // - Bot-like Activity
        // - Abnormal Usage Pattern

        return new BehaviorResult(
                riskScore,
                BehaviorRiskLevel.NORMAL,
                flags,
                summary
        );
    }
}
