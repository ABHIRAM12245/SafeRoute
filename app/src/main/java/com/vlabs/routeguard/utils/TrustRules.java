package com.vlabs.routeguard.utils;

public class TrustRules {
    public static final int DEFAULT_TRUST_SCORE = 50;
    public static final int MIN_TRUST_SCORE = 0;
    public static final int MAX_TRUST_SCORE = 100;

    // Vote scoring weights
    public static final int UPVOTE_WEIGHT = 2;
    public static final int DOWNVOTE_WEIGHT = 3;

    // Report outcome weights for creator
    public static final int VERIFIED_REPORT_BONUS = 10;
    public static final int DISPUTED_REPORT_PENALTY = 15;

    // Verification thresholds
    public static final int VERIFIED_VOTE_THRESHOLD = 2;
    public static final int DISPUTED_VOTE_THRESHOLD = 2;

    // User level labels
    public static final String LEVEL_NOVICE = "Novice Reporter";
    public static final String LEVEL_CONTRIBUTOR = "Contributor";
    public static final String LEVEL_TRUSTED = "Trusted Reporter";
    public static final String LEVEL_MASTER = "Community Master";

    // User level score thresholds
    public static final int THRESHOLD_NOVICE_MAX = 25;
    public static final int THRESHOLD_CONTRIBUTOR_MAX = 50;
    public static final int THRESHOLD_TRUSTED_MAX = 75;
}
