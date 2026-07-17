package com.vlabs.routeguard.utils;

public class TrustConfig {
    public static final int DEFAULT_TRUST_SCORE = 50;
    public static final int MIN_TRUST_SCORE = 0;
    public static final int MAX_TRUST_SCORE = 100;

    public static final int POSITIVE_VOTE = 1;
    public static final int NEGATIVE_VOTE_WEIGHT = 2;
    public static final int VALID_REPORT_WEIGHT = 5;
    public static final int INVALID_REPORT_WEIGHT = 10;
}
