package com.vlabs.routeguard.models;

import com.google.firebase.Timestamp;
import java.util.Map;

public class Comment {
    private String commentId;
    private String userId;
    private String displayName;
    private String photoUrl;
    private String text;
    private Timestamp timestamp;
    private boolean isDeleted;
    
    // Future expansion fields
    private Long editedAt;
    private Map<String, Integer> reactions;

    public Comment() {
        // Empty constructor for Firestore
    }

    public Comment(String commentId, String userId, String displayName, String photoUrl, String text, Timestamp timestamp) {
        this.commentId = commentId;
        this.userId = userId;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.text = text;
        this.timestamp = timestamp;
        this.isDeleted = false;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Long getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Long editedAt) {
        this.editedAt = editedAt;
    }

    public Map<String, Integer> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, Integer> reactions) {
        this.reactions = reactions;
    }
}
