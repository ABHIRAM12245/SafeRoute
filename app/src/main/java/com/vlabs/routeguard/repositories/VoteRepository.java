package com.vlabs.routeguard.repositories;

import com.vlabs.routeguard.models.ReportVote;
import com.vlabs.routeguard.models.VoteResult;
import java.util.List;
import com.vlabs.routeguard.models.VoteCount;
import com.vlabs.routeguard.managers.VotingManager.VoteType;

/**
 * Foundation architecture for Firestore vote operations.
 */
public class VoteRepository {

    private final VoteFirestoreRepository firestoreRepository;

    public VoteRepository() {
        this.firestoreRepository = new VoteFirestoreRepository();
    }

    /**
     * Saves a vote to Firestore.
     *
     * @param reportVote The ReportVote to save.
     * @return true if the vote was successfully submitted locally.
     */
    public boolean saveVote(ReportVote reportVote) {
        if (reportVote == null || reportVote.getReportId() == null || reportVote.getUserId() == null) {
            return false;
        }

        VoteResult result = firestoreRepository.saveVote(reportVote);
        return result != null && result.isSuccess();
    }

    /**
     * Retrieves raw votes by delegating to the data layer.
     *
     * @param reportId The ID of the report.
     * @param callback The callback to handle the asynchronous result.
     */
    public void getVotes(String reportId, VoteFirestoreRepository.GetVotesCallback callback) {
        firestoreRepository.getVotes(reportId, callback);
    }

    /**
     * Retrieves the current user's vote by delegating to the data layer.
     *
     * @param reportId The ID of the report.
     * @param userId The ID of the user.
     * @param callback The callback to handle the asynchronous result.
     */
    public void getCurrentUserVote(String reportId, String userId, VoteFirestoreRepository.GetCurrentUserVoteCallback callback) {
        firestoreRepository.getCurrentUserVote(reportId, userId, callback);
    }

    /**
     * Calculates the aggregated vote count from a raw list of votes.
     *
     * @param votes The raw list of votes to process.
     * @return A VoteCount model containing the totals.
     */
    public VoteCount calculateVoteCount(List<ReportVote> votes) {
        if (votes == null || votes.isEmpty()) {
            return new VoteCount(0, 0);
        }

        int upvotes = 0;
        int downvotes = 0;

        for (ReportVote vote : votes) {
            if (vote != null && vote.getVoteType() != null) {
                if (vote.getVoteType() == VoteType.UPVOTE) {
                    upvotes++;
                } else if (vote.getVoteType() == VoteType.DOWNVOTE) {
                    downvotes++;
                }
            }
        }

        return new VoteCount(upvotes, downvotes);
    }
}
