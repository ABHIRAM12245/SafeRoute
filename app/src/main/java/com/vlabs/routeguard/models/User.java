package com.vlabs.routeguard.models;

/**
 * Firestore user profile model used throughout RouteGuard.
 * This model is completely independent from FirebaseAuth.
 */
public class User {
    private String userId;
    private String displayName;
    private String email;
    private String photoUrl;
    private long createdAt;
    private long lastActiveAt;
    private int trustScore;
    private int reputationScore;
    private boolean isModerator;
    private boolean isBlocked;

    public User() {
        // Default constructor required for Firestore
    }

    public User(String userId, String displayName, String email, String photoUrl, long createdAt, long lastActiveAt, int trustScore, int reputationScore, boolean isModerator, boolean isBlocked) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.trustScore = trustScore;
        this.reputationScore = reputationScore;
        this.isModerator = isModerator;
        this.isBlocked = isBlocked;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(long lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public int getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(int trustScore) {
        this.trustScore = trustScore;
    }

    public int getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(int reputationScore) {
        this.reputationScore = reputationScore;
    }

    public boolean isModerator() {
        return isModerator;
    }

    public void setModerator(boolean moderator) {
        isModerator = moderator;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}
