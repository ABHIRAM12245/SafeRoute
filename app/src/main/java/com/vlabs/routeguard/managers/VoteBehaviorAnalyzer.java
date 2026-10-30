package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.BehaviorMetrics;
import com.vlabs.routeguard.models.BehaviorResult;
import com.vlabs.routeguard.models.BehaviorRiskLevel;
import com.vlabs.routeguard.utils.BehaviorConfig;

import java.util.ArrayList;

/**
 * User Voting Behavior and Anti-Abuse Analyzer.
 *
 * Evaluates voting statistics to detect malicious patterns such as mass downvoting,
 * targeted negative campaigns, and vote farming.
 *
 * Algorithmic Logic:
 * - Requires a minimum of {@link BehaviorConfig#MINIMUM_VOTES_FOR_ANALYSIS} votes before flagging.
 * - Computes the negative vote ratio: {@code negativeVotes / totalVotes}.
 * - Flags users exhibiting abnormal downvoting velocity with elevated risk scores (NORMAL, LOW_RISK, MEDIUM_RISK, HIGH_RISK).
 */
public class VoteBehaviorAnalyzer {

    public VoteBehaviorAnalyzer() {
        // Default constructor
    }

    /**
     * Evaluates voting behavior metrics.
     *
     * @param metrics The metrics to evaluate.
     * @return An independent BehaviorResult based on voting behavior.
     */
    public BehaviorResult analyze(BehaviorMetrics metrics) {
        if (metrics == null) {
            return new BehaviorResult(
                    BehaviorConfig.MIN_RISK_SCORE,
                    BehaviorRiskLevel.NORMAL,
                    new ArrayList<>(),
                    "No suspicious voting behavior detected."
            );
        }

        int riskScore = BehaviorConfig.MIN_RISK_SCORE;
        java.util.List<com.vlabs.routeguard.models.BehaviorFlag> flags = new ArrayList<>();
        String summary = "No suspicious voting behavior detected.";

        if (metrics.getTotalVotesGiven() < BehaviorConfig.MINIMUM_VOTES_FOR_ANALYSIS) {
            return new BehaviorResult(
                    riskScore,
                    BehaviorRiskLevel.NORMAL,
                    flags,
                    summary
            );
        }

        double negativeRatio = 0;
        if (metrics.getTotalVotesGiven() > 0) {
            negativeRatio = (double) metrics.getNegativeVotesGiven() / metrics.getTotalVotesGiven();
        }

        if (negativeRatio > BehaviorConfig.MAX_NEGATIVE_RATIO) {
            flags.add(com.vlabs.routeguard.models.BehaviorFlag.MASS_DOWNVOTING);
            riskScore += 40;
            if (riskScore > BehaviorConfig.MAX_RISK_SCORE) {
                riskScore = BehaviorConfig.MAX_RISK_SCORE;
            }
            summary = "Excessive downvoting behavior detected.";
        }

        // TODO: Implement future detection modules:
        // - Vote Farming
        // - High Daily Voting
        // - Vote Ratio Analysis

        double positiveRatio = 0;
        if (metrics.getTotalVotesGiven() > 0) {
            positiveRatio = (double) metrics.getPositiveVotesGiven() / metrics.getTotalVotesGiven();
        }

        if (positiveRatio > BehaviorConfig.MAX_POSITIVE_RATIO) {
            flags.add(com.vlabs.routeguard.models.BehaviorFlag.MASS_UPVOTING);
            riskScore += 30;
            if (riskScore > BehaviorConfig.MAX_RISK_SCORE) {
                riskScore = BehaviorConfig.MAX_RISK_SCORE;
            }
            summary = "Excessive upvoting behavior detected.";
        }

        return new BehaviorResult(
                riskScore,
                BehaviorRiskLevel.NORMAL,
                flags,
                summary
        );
    }
}
