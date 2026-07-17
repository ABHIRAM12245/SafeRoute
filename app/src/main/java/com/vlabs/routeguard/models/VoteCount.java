package com.vlabs.routeguard.models;

/**
 * Data model representing the aggregated vote counts for a report.
 */
public class VoteCount {

    private int upvoteCount;
    private int downvoteCount;

    public VoteCount() {
    }

    public VoteCount(int upvoteCount, int downvoteCount) {
        this.upvoteCount = upvoteCount;
        this.downvoteCount = downvoteCount;
    }

    public int getUpvoteCount() {
        return upvoteCount;
    }

    public void setUpvoteCount(int upvoteCount) {
        this.upvoteCount = upvoteCount;
    }

    public int getDownvoteCount() {
        return downvoteCount;
    }

    public void setDownvoteCount(int downvoteCount) {
        this.downvoteCount = downvoteCount;
    }
}
