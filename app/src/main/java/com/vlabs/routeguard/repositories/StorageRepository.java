package com.vlabs.routeguard.repositories;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vlabs.routeguard.models.PhotoAttachment;

/**
 * Foundation architecture for Firebase Storage operations.
 */
public class StorageRepository {

    private final FirebaseStorage storage;

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    public StorageRepository() {
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Legacy synchronous stub method. Maintained so UploadManager does not break.
     * @param attachment The PhotoAttachment to upload.
     * @return An empty string.
     */
    public String upload(PhotoAttachment attachment) {
        return "";
    }

    /**
     * Uploads a photo attachment to Firebase Storage asynchronously.
     *
     * @param attachment The PhotoAttachment to upload.
     * @param callback The callback to return success (download URL) or failure.
     */
    public void upload(PhotoAttachment attachment, UploadCallback callback) {
        if (attachment == null || attachment.getUri() == null) {
            callback.onFailure(new IllegalArgumentException("Invalid attachment or URI"));
            return;
        }

        StorageReference storageRef = storage.getReference();
        // Store under reports folder with a timestamp-based unique name
        StorageReference photoRef = storageRef.child("reports/" + System.currentTimeMillis() + ".jpg");

        photoRef.putFile(attachment.getUri())
                .addOnSuccessListener(taskSnapshot -> {
                    // Fetch the download URL upon successful upload
                    photoRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);
    }
}

