package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.BehaviorMetrics;
import com.vlabs.routeguard.models.BehaviorResult;
import com.vlabs.routeguard.models.BehaviorRiskLevel;
import com.vlabs.routeguard.utils.BehaviorConfig;

import java.util.ArrayList;

/**
 * Core analysis engine architecture for the Behavior System.
 *
 * Future Note:
 * Future milestones will inject independent analyzers such as:
 * - VoteBehaviorAnalyzer
 * - ReportBehaviorAnalyzer
 * - ActivityBehaviorAnalyzer
 * - PatternBehaviorAnalyzer
 *
 * BehaviorEngine will only orchestrate them.
 */
public class BehaviorEngine {

    private final VoteBehaviorAnalyzer voteBehaviorAnalyzer;
    private final ReportBehaviorAnalyzer reportBehaviorAnalyzer;
    private final ActivityBehaviorAnalyzer activityBehaviorAnalyzer;

    public BehaviorEngine() {
        this.voteBehaviorAnalyzer = new VoteBehaviorAnalyzer();
        this.reportBehaviorAnalyzer = new ReportBehaviorAnalyzer();
        this.activityBehaviorAnalyzer = new ActivityBehaviorAnalyzer();
    }

    public BehaviorResult analyze(BehaviorMetrics metrics) {
        if (metrics == null) {
            return new BehaviorResult(
                    BehaviorConfig.MIN_RISK_SCORE,
                    BehaviorRiskLevel.NORMAL,
                    new ArrayList<>(),
                    "No suspicious behavior detected."
            );
        }

        BehaviorResult voteResult = voteBehaviorAnalyzer.analyze(metrics);
        BehaviorResult reportResult = reportBehaviorAnalyzer.analyze(metrics);
        BehaviorResult activityResult = activityBehaviorAnalyzer.analyze(metrics);

        // Merge Risk Score
        int totalScore = voteResult.getRiskScore() + reportResult.getRiskScore() + activityResult.getRiskScore();
        totalScore = Math.max(BehaviorConfig.MIN_RISK_SCORE, Math.min(BehaviorConfig.MAX_RISK_SCORE, totalScore));

        // Merge Flags
        java.util.Set<com.vlabs.routeguard.models.BehaviorFlag> mergedFlags = new java.util.HashSet<>();
        if (voteResult.getFlags() != null) mergedFlags.addAll(voteResult.getFlags());
        if (reportResult.getFlags() != null) mergedFlags.addAll(reportResult.getFlags());
        if (activityResult.getFlags() != null) mergedFlags.addAll(activityResult.getFlags());

        // Merge Summaries
        java.util.Set<String> uniqueSummaries = new java.util.LinkedHashSet<>();
        if (voteResult.getSummary() != null) uniqueSummaries.add(voteResult.getSummary().trim());
        if (reportResult.getSummary() != null) uniqueSummaries.add(reportResult.getSummary().trim());
        if (activityResult.getSummary() != null) uniqueSummaries.add(activityResult.getSummary().trim());

        // Avoid adding safe defaults if there are other suspicious summaries
        uniqueSummaries.remove("No suspicious voting behavior detected.");
        uniqueSummaries.remove("No suspicious reporting behavior detected.");
        uniqueSummaries.remove("No suspicious activity detected.");

        String finalSummary;
        if (uniqueSummaries.isEmpty()) {
            finalSummary = "No suspicious behavior detected.";
        } else {
            finalSummary = String.join(" ", uniqueSummaries);
        }

        // Determine Risk Level
        BehaviorRiskLevel finalLevel;
        if (totalScore < BehaviorConfig.LOW_RISK_THRESHOLD) {
            finalLevel = BehaviorRiskLevel.NORMAL;
        } else if (totalScore < BehaviorConfig.MEDIUM_RISK_THRESHOLD) {
            finalLevel = BehaviorRiskLevel.LOW;
        } else if (totalScore < BehaviorConfig.HIGH_RISK_THRESHOLD) {
            finalLevel = BehaviorRiskLevel.MEDIUM;
        } else if (totalScore < BehaviorConfig.CRITICAL_RISK_THRESHOLD) {
            finalLevel = BehaviorRiskLevel.HIGH;
        } else {
            finalLevel = BehaviorRiskLevel.CRITICAL;
        }

        return new BehaviorResult(
                totalScore,
                finalLevel,
                new ArrayList<>(mergedFlags),
                finalSummary
        );
    }
}
