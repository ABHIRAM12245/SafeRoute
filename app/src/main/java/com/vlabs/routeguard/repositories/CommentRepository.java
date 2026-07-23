package com.vlabs.routeguard.repositories;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.vlabs.routeguard.models.Comment;
import com.vlabs.routeguard.utils.FirestoreCollections;
import com.vlabs.routeguard.utils.FirestoreFields;

import java.util.ArrayList;
import java.util.List;

public class CommentRepository extends BaseRepository {

    private final FirebaseFirestore db;

    public interface GetCommentsCallback {
        void onSuccess(List<Comment> comments);
        void onFailure(Exception e);
    }

    public interface AddCommentCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public CommentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public ListenerRegistration startCommentsListener(String reportId, GetCommentsCallback callback) {
        return db.collection(FirestoreCollections.REPORTS)
                .document(reportId)
                .collection(FirestoreCollections.COMMENTS)
                .orderBy(FirestoreFields.TIMESTAMP, Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("CommentPipeline", "Firestore query error: " + error.getMessage());
                        callback.onFailure(error);
                        return;
                    }

                    List<Comment> comments = new ArrayList<>();
                    if (value != null && !value.isEmpty()) {
                        comments = value.toObjects(Comment.class);
                    }
                    callback.onSuccess(comments);
                });
    }

    public void addComment(String reportId, Comment comment, AddCommentCallback callback) {
        WriteBatch batch = db.batch();

        DocumentReference reportRef = db.collection(FirestoreCollections.REPORTS).document(reportId);
        DocumentReference newCommentRef = reportRef.collection(FirestoreCollections.COMMENTS).document();

        comment.setCommentId(newCommentRef.getId());

        // Batch operation 1: Add the comment
        batch.set(newCommentRef, comment);
        batch.update(newCommentRef, FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());

        // Batch operation 2: Update the parent document
        batch.update(reportRef, FirestoreFields.COMMENT_COUNT, FieldValue.increment(1));
        batch.update(reportRef, FirestoreFields.LAST_COMMENT_AT, FieldValue.serverTimestamp());

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e));
    }
}
