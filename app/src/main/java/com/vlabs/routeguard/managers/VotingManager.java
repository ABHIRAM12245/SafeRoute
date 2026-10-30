package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.ReportVote;
import com.vlabs.routeguard.models.VoteResult;
import com.vlabs.routeguard.models.VoteCount;
import com.vlabs.routeguard.repositories.VoteRepository;

/**
 * Community Voting Subsystem Manager.
 *
 * Coordinates upvoting and downvoting on road hazard reports, managing vote records,
 * real-time vote listeners, and score aggregation across Cloud Firestore.
 *
 * Architecture & Collection Hierarchy:
 * - Subcollection Path: {@code reports/{reportId}/votes/{userId}}
 * - Vote Entity: {@link ReportVote} storing voteId, reportId, userId, voteType (UPVOTE/DOWNVOTE), timestamp, reportOwnerId.
 * - Score Aggregation: {@link VoteCount} tallies total upvotes, downvotes, and net confidence score.
 * - Anti-Abuse: Works in tandem with {@link VoteBehaviorAnalyzer} to detect rapid vote manipulation.
 */
public class VotingManager {

    private final VoteRepository voteRepository;

    /**
     * Represents the type of vote that can be cast on a report.
     */
    public enum VoteType {
        UPVOTE,
        DOWNVOTE
    }

    public VotingManager() {
        this.voteRepository = new VoteRepository();
    }

    /**
     * Submits a vote on a report by delegating to the VoteRepository.
     *
     * @param reportId The ID of the report being voted on.
     * @param voteType The type of vote (UPVOTE or DOWNVOTE).
     * @param userId   The ID of the user casting the vote.
     * @return A VoteResult indicating the submission status.
     */
    public VoteResult vote(String reportId, VoteType voteType, String userId) {
        return vote(reportId, null, voteType, userId);
    }

    public VoteResult vote(String reportId, String reportOwnerId, VoteType voteType, String userId) {
        if (reportId == null || reportId.isEmpty() || voteType == null || userId == null || userId.isEmpty()) {
            return new VoteResult(false, "Invalid vote parameters.");
        }

        String voteId = reportId + "_" + userId;
        ReportVote reportVote = new ReportVote(voteId, reportId, userId, voteType, System.currentTimeMillis(), reportOwnerId);

        boolean success = voteRepository.saveVote(reportVote);
        return new VoteResult(success, success ? null : "Failed to save vote to repository.");
    }

    public interface GetVoteCountCallback {
        void onSuccess(VoteCount voteCount);
        void onFailure(Exception e);
    }

    /**
     * Retrieves the aggregated vote count for a specific report.
     *
     * @param reportId The ID of the report.
     * @param callback The callback to return the VoteCount.
     */
    public void getVoteCount(String reportId, GetVoteCountCallback callback) {
        voteRepository.getVotes(reportId, new com.vlabs.routeguard.repositories.VoteFirestoreRepository.GetVotesCallback() {
            @Override
            public void onSuccess(java.util.List<ReportVote> votes) {
                VoteCount voteCount = voteRepository.calculateVoteCount(votes);
                callback.onSuccess(voteCount);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public interface GetCurrentUserVoteCallback {
        void onSuccess(ReportVote vote);
        void onFailure(Exception e);
    }

    /**
     * Retrieves the current user's vote for a specific report.
     *
     * @param reportId The ID of the report.
     * @param userId The ID of the user.
     * @param callback The callback to return the ReportVote.
     */
    public void getCurrentUserVote(String reportId, String userId, GetCurrentUserVoteCallback callback) {
        voteRepository.getCurrentUserVote(reportId, userId, new com.vlabs.routeguard.repositories.VoteFirestoreRepository.GetCurrentUserVoteCallback() {
            @Override
            public void onSuccess(ReportVote vote) {
                callback.onSuccess(vote);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Starts a real-time listener for the aggregated vote count.
     *
     * @param reportId The ID of the report.
     * @param callback The callback to return the VoteCount.
     * @return The ListenerRegistration.
     */
    public com.google.firebase.firestore.ListenerRegistration startVoteCountListener(String reportId, GetVoteCountCallback callback) {
        return voteRepository.startVotesListener(reportId, new com.vlabs.routeguard.repositories.VoteFirestoreRepository.GetVotesCallback() {
            @Override
            public void onSuccess(java.util.List<ReportVote> votes) {
                VoteCount voteCount = voteRepository.calculateVoteCount(votes);
                callback.onSuccess(voteCount);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Starts a real-time listener for the current user's vote.
     *
     * @param reportId The ID of the report.
     * @param userId The ID of the user.
     * @param callback The callback to return the ReportVote.
     * @return The ListenerRegistration.
     */
    public com.google.firebase.firestore.ListenerRegistration startCurrentUserVoteListener(String reportId, String userId, GetCurrentUserVoteCallback callback) {
        return voteRepository.startCurrentUserVoteListener(reportId, userId, new com.vlabs.routeguard.repositories.VoteFirestoreRepository.GetCurrentUserVoteCallback() {
            @Override
            public void onSuccess(ReportVote vote) {
                callback.onSuccess(vote);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Calculates the trust score based on the aggregated vote counts.
     * Keeps business logic outside the model and allows future weighted trust algorithms.
     *
     * @param voteCount The aggregated vote count.
     * @return The calculated trust score.
     */
    public int calculateTrustScore(VoteCount voteCount) {
        if (voteCount == null) {
            return 0;
        }
        return voteCount.getUpvoteCount() - voteCount.getDownvoteCount();
    }

    /**
     * Calculates the trust score combining Votes, Confirmations, and Freshness.
     * Weights: 30% Votes, 60% Confirmations, 10% Freshness.
     */
    public int calculateTrustScore(VoteCount voteCount, com.vlabs.routeguard.models.ConfirmationCount confirmCount, long reportTimestampMs) {
        float weightVotes = 0.30f;
        float weightConfirm = 0.60f;
        float weightFreshness = 0.10f;

        int votesScore = (voteCount != null) ? (voteCount.getUpvoteCount() - voteCount.getDownvoteCount()) : 0;
        int confirmScore = (confirmCount != null) ? (confirmCount.getActiveCount() - confirmCount.getGoneCount()) : 0;

        float normalizedVotes = votesScore * (weightVotes * 10); // 3x multiplier
        float normalizedConfirms = confirmScore * (weightConfirm * 10); // 6x multiplier

        float normalizedFreshness = 0f;
        long ageMs = System.currentTimeMillis() - reportTimestampMs;
        if (ageMs < 3600000) { // < 1 hr
            normalizedFreshness = 1.0f * (weightFreshness * 10); // +1
        } else if (ageMs > 10800000) { // > 3 hrs
            normalizedFreshness = -1.0f * (weightFreshness * 10); // -1
        }

        return Math.round(normalizedVotes + normalizedConfirms + normalizedFreshness);
    }
}
