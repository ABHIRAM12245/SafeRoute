package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.TrustScore;
import com.vlabs.routeguard.utils.TrustConfig;

/**
 * Foundation class for the Trust Score system.
 * Responsible for calculating and returning the user's trust score.
 * Future integration points: Voting, Report Verification, and Admin Actions.
 */
public class TrustManager {

    private int currentTrustScore;

    public TrustManager() {
        this.currentTrustScore = TrustConfig.DEFAULT_TRUST_SCORE;
    }

    /**
     * Prepares the architecture for future trust score calculation.
     * 
     * @return A stub trust score value for this milestone.
     */
    public int calculateTrustScore() {
        // TODO: Implement real calculation based on Voting, Report Verification, and Admin Action.
        return currentTrustScore;
    }

    /**
     * Returns the current trust score.
     * 
     * @return The current trust score.
     */
    public int getTrustScore() {
        return currentTrustScore;
    }

    /**
     * Calculates the trust score based on user metrics.
     * 
     * @param positiveVotes Number of upvotes.
     * @param negativeVotes Number of downvotes.
     * @param validReports Number of verified reports.
     * @param invalidReports Number of rejected reports.
     * @return A stubbed TrustScore model.
     */
    public TrustScore calculateTrustScore(
        int positiveVotes,
        int negativeVotes,
        int validReports,
        int invalidReports
    ) {
        int score = TrustConfig.DEFAULT_TRUST_SCORE
                + (positiveVotes * TrustConfig.POSITIVE_VOTE)
                - (negativeVotes * TrustConfig.NEGATIVE_VOTE_WEIGHT)
                + (validReports * TrustConfig.VALID_REPORT_WEIGHT)
                - (invalidReports * TrustConfig.INVALID_REPORT_WEIGHT);

        score = Math.max(TrustConfig.MIN_TRUST_SCORE, Math.min(TrustConfig.MAX_TRUST_SCORE, score));
        this.currentTrustScore = score;

        return new TrustScore(null, score, System.currentTimeMillis());
    }
}
