package com.vlabs.routeguard.repositories;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.vlabs.routeguard.models.ReportConfirmation;
import java.util.ArrayList;
import java.util.List;

public class ConfirmationRepository {

    private final FirebaseFirestore db;

    public ConfirmationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onComplete(boolean success, String error);
    }

    public void saveConfirmation(ReportConfirmation confirmation, SaveCallback callback) {
        if (confirmation == null || confirmation.getReportId() == null || confirmation.getUserId() == null) {
            callback.onComplete(false, "Invalid confirmation data.");
            return;
        }

        String docId = confirmation.getReportId() + "_" + confirmation.getUserId();

        db.collection("report_confirmations")
          .document(docId)
          .set(confirmation, SetOptions.merge())
          .addOnSuccessListener(aVoid -> callback.onComplete(true, null))
          .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
    }

    public interface GetConfirmationsCallback {
        void onSuccess(List<ReportConfirmation> confirmations);
        void onFailure(Exception e);
    }

    public com.google.firebase.firestore.ListenerRegistration startConfirmationsListener(String reportId, GetConfirmationsCallback callback) {
        if (reportId == null || reportId.isEmpty()) {
            return null;
        }

        return db.collection("report_confirmations")
          .whereEqualTo("reportId", reportId)
          .addSnapshotListener((value, error) -> {
              if (error != null) {
                  callback.onFailure(error);
                  return;
              }
              List<ReportConfirmation> confirmations = new ArrayList<>();
              if (value != null) {
                  for (com.google.firebase.firestore.DocumentSnapshot document : value.getDocuments()) {
                      try {
                          ReportConfirmation conf = document.toObject(ReportConfirmation.class);
                          if (conf != null) {
                              confirmations.add(conf);
                          }
                      } catch (Exception e) {
                          // Skip invalid
                      }
                  }
              }
              callback.onSuccess(confirmations);
          });
    }

    public interface GetCurrentUserConfirmationCallback {
        void onSuccess(ReportConfirmation confirmation);
        void onFailure(Exception e);
    }

    public void getConfirmations(String reportId, GetConfirmationsCallback callback) {
        if (reportId == null || reportId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Invalid reportId"));
            return;
        }

        db.collection("report_confirmations")
          .whereEqualTo("reportId", reportId)
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              List<ReportConfirmation> confirmations = new ArrayList<>();
              if (queryDocumentSnapshots != null) {
                  for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                      try {
                          ReportConfirmation conf = document.toObject(ReportConfirmation.class);
                          if (conf != null) {
                              confirmations.add(conf);
                          }
                      } catch (Exception e) {
                          // Skip invalid
                      }
                  }
              }
              callback.onSuccess(confirmations);
          })
          .addOnFailureListener(callback::onFailure);
    }

    public com.google.firebase.firestore.ListenerRegistration startCurrentUserConfirmationListener(String reportId, String userId, GetCurrentUserConfirmationCallback callback) {
        if (reportId == null || reportId.isEmpty() || userId == null || userId.isEmpty()) {
            return null;
        }

        String docId = reportId + "_" + userId;
        return db.collection("report_confirmations")
          .document(docId)
          .addSnapshotListener((documentSnapshot, error) -> {
              if (error != null) {
                  callback.onFailure(error);
                  return;
              }
              if (documentSnapshot != null && documentSnapshot.exists()) {
                  try {
                      ReportConfirmation conf = documentSnapshot.toObject(ReportConfirmation.class);
                      callback.onSuccess(conf);
                  } catch (Exception e) {
                      callback.onFailure(e);
                  }
              } else {
                  callback.onSuccess(null);
              }
          });
    }

    public void getCurrentUserConfirmation(String reportId, String userId, GetCurrentUserConfirmationCallback callback) {
        if (reportId == null || reportId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Invalid reportId or userId"));
            return;
        }

        String docId = reportId + "_" + userId;
        db.collection("report_confirmations")
          .document(docId)
          .get()
          .addOnSuccessListener(documentSnapshot -> {
              if (documentSnapshot != null && documentSnapshot.exists()) {
                  try {
                      ReportConfirmation conf = documentSnapshot.toObject(ReportConfirmation.class);
                      callback.onSuccess(conf);
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
