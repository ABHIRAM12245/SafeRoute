package com.vlabs.routeguard.models;

/**
 * Data model for a user's trust score.
 */
public class TrustScore {
    private String userId;
    private int score;
    private long lastUpdated;

    public TrustScore() {
    }

    public TrustScore(String userId, int score, long lastUpdated) {
        this.userId = userId;
        this.score = score;
        this.lastUpdated = lastUpdated;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
