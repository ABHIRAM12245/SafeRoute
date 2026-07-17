package com.vlabs.routeguard.repositories;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vlabs.routeguard.models.ReportVote;
import com.vlabs.routeguard.models.VoteResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated architecture for Firestore operations exclusively related to votes.
 */
public class VoteFirestoreRepository {

    private final FirebaseFirestore db;

    public VoteFirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Saves a vote directly to Firestore.
     * Enforces one active vote per user per report.
     *
     * @param reportVote The ReportVote to save.
     * @return A VoteResult indicating local submission status.
     */
    public VoteResult saveVote(ReportVote reportVote) {
        if (reportVote == null || reportVote.getReportId() == null || reportVote.getUserId() == null || 
            reportVote.getReportOwnerId() == null || reportVote.getReportOwnerId().isEmpty()) {
            return new VoteResult(false, "Invalid vote data.");
        }

        try {
            db.collection("reports")
              .document(reportVote.getReportId())
              .collection("votes")
              .document(reportVote.getUserId())
              .set(reportVote, SetOptions.merge());
            return new VoteResult(true, null);
        } catch (Exception e) {
            return new VoteResult(false, e.getMessage());
        }
    }

    public interface GetVotesCallback {
        void onSuccess(List<ReportVote> votes);
        void onFailure(Exception e);
    }

    /**
     * Retrieves all votes belonging to a report from Firestore.
     * Uses asynchronous callbacks to return the result.
     *
     * @param reportId The ID of the report.
     * @param callback The callback to return the list of ReportVote objects.
     */
    public void getVotes(String reportId, GetVotesCallback callback) {
        if (reportId == null || reportId.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection("reports")
          .document(reportId)
          .collection("votes")
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              List<ReportVote> votes = new ArrayList<>();
              if (queryDocumentSnapshots != null) {
                  for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                      try {
                          ReportVote vote = document.toObject(ReportVote.class);
                          if (vote != null) {
                              votes.add(vote);
                          }
                      } catch (Exception e) {
                          // Skip invalid documents safely
                      }
                  }
              }
              callback.onSuccess(votes);
          })
          .addOnFailureListener(callback::onFailure);
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
     * @param callback The callback to return the ReportVote or null if not found.
     */
    public void getCurrentUserVote(String reportId, String userId, GetCurrentUserVoteCallback callback) {
        if (reportId == null || reportId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onSuccess(null);
            return;
        }

        db.collection("reports")
          .document(reportId)
          .collection("votes")
          .document(userId)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot != null && documentSnapshot.exists()) {
                  try {
                      ReportVote vote = documentSnapshot.toObject(ReportVote.class);
                      callback.onSuccess(vote);
                  } catch (Exception e) {
                      callback.onFailure(e);
                  }
              } else {
                  callback.onSuccess(null);
              }
          })
          .addOnFailureListener(callback::onFailure);
    }
}
