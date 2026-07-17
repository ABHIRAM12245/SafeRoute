package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.ReportVote;
import com.vlabs.routeguard.models.VoteResult;
import com.vlabs.routeguard.models.VoteCount;
import com.vlabs.routeguard.repositories.VoteRepository;

/**
 * Foundation architecture for report voting functionality.
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
}
