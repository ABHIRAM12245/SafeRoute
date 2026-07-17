package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.BehaviorMetrics;
import com.vlabs.routeguard.models.BehaviorResult;
import com.vlabs.routeguard.models.BehaviorRiskLevel;
import com.vlabs.routeguard.utils.BehaviorConfig;

import java.util.ArrayList;

/**
 * Independent analyzer responsible for evaluating report-related behavior.
 * This analyzer only analyzes reporting metrics and returns an independent BehaviorResult.
 *
 * Future detection modules to be implemented:
 * - Spam Reporting
 * - Low Report Accuracy
 * - Excessive Daily Reporting
 * - Fake Report Detection
 * - Repeated Invalid Reports
 */
public class ReportBehaviorAnalyzer {

    public ReportBehaviorAnalyzer() {
        // Default constructor
    }

    /**
     * Evaluates reporting behavior metrics.
     *
     * @param metrics The metrics to evaluate.
     * @return An independent BehaviorResult based on reporting behavior.
     */
    public BehaviorResult analyze(BehaviorMetrics metrics) {
        if (metrics == null) {
            return new BehaviorResult(
                    BehaviorConfig.MIN_RISK_SCORE,
                    BehaviorRiskLevel.NORMAL,
                    new ArrayList<>(),
                    "No suspicious reporting behavior detected."
            );
        }

        int riskScore = BehaviorConfig.MIN_RISK_SCORE;
        java.util.List<com.vlabs.routeguard.models.BehaviorFlag> flags = new ArrayList<>();
        String summary = "No suspicious reporting behavior detected.";

        if (metrics.getTotalReports() == 0) {
            return new BehaviorResult(
                    riskScore,
                    BehaviorRiskLevel.NORMAL,
                    flags,
                    summary
            );
        }

        double invalidReportRatio = (double) metrics.getInvalidReports() / metrics.getTotalReports();

        if (invalidReportRatio > (1.0 - BehaviorConfig.MIN_REPORT_ACCURACY)) {
            flags.add(com.vlabs.routeguard.models.BehaviorFlag.SPAM_REPORTING);
            riskScore += 35;
            if (riskScore > BehaviorConfig.MAX_RISK_SCORE) {
                riskScore = BehaviorConfig.MAX_RISK_SCORE;
            }
            summary = "High invalid report ratio detected.";
        }

        // TODO: Implement future detection modules:
        // - Low Report Accuracy
        // - Excessive Daily Reporting
        // - Fake Report Detection
        // - Repeated Invalid Reports

        return new BehaviorResult(
                riskScore,
                BehaviorRiskLevel.NORMAL,
                flags,
                summary
        );
    }
}
