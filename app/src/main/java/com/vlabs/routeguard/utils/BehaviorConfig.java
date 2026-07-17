package com.vlabs.routeguard.utils;

public final class BehaviorConfig {
    
    // Risk Score Limits
    public static final int MIN_RISK_SCORE = 0;
    public static final int MAX_RISK_SCORE = 100;
    
    // Risk Thresholds
    public static final int LOW_RISK_THRESHOLD = 20;
    public static final int MEDIUM_RISK_THRESHOLD = 40;
    public static final int HIGH_RISK_THRESHOLD = 70;
    public static final int CRITICAL_RISK_THRESHOLD = 90;
    
    // Voting Thresholds
    public static final double MAX_NEGATIVE_RATIO = 0.80;
    public static final double MAX_POSITIVE_RATIO = 0.90;
    public static final int MAX_VOTES_PER_DAY = 100;
    public static final int MINIMUM_VOTES_FOR_ANALYSIS = 20;
    
    // Reporting Thresholds
    public static final int MAX_REPORTS_PER_DAY = 20;
    public static final double MIN_REPORT_ACCURACY = 0.40;
    
    // Future Moderation Thresholds
    public static final int WARNING_THRESHOLD = 40;
    public static final int RESTRICT_THRESHOLD = 70;
    public static final int CRITICAL_THRESHOLD = 90;
}
