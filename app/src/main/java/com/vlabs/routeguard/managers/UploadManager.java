package com.vlabs.routeguard.managers;

import android.content.Context;

import com.vlabs.routeguard.models.PhotoAttachment;
import com.vlabs.routeguard.repositories.StorageRepository;

/**
 * Orchestrates the full photo upload pipeline.
 */
public class UploadManager {

    private final PhotoValidationManager validationManager;
    private final PhotoCompressionManager compressionManager;
    private final StorageRepository storageRepository;

    public UploadManager(Context context) {
        this.validationManager = context != null ? new PhotoValidationManager(context) : null;
        this.compressionManager = context != null ? new PhotoCompressionManager(context) : null;
        this.storageRepository = new StorageRepository();
    }

    public UploadManager() {
        this(null);
    }

    /**
     * Executes the upload pipeline: Validation -> Compression -> Storage.
     * Legacy synchronous stub method.
     *
     * @param attachment The PhotoAttachment to process and upload.
     * @return A simulated remote download URL.
     */
    public String upload(PhotoAttachment attachment) {
        if (validationManager != null && !validationManager.validatePhoto(attachment)) {
            return "";
        }
        PhotoAttachment compressedAttachment = attachment;
        if (compressionManager != null) {
            compressedAttachment = compressionManager.compress(attachment);
        }
        return storageRepository.upload(compressedAttachment);
    }

    /**
     * Executes the upload pipeline asynchronously.
     *
     * @param attachment The PhotoAttachment to process and upload.
     * @param callback The callback to receive the download URL or failure.
     */
    public void upload(PhotoAttachment attachment, StorageRepository.UploadCallback callback) {
        if (validationManager != null && !validationManager.validatePhoto(attachment)) {
            callback.onFailure(new Exception("Validation failed"));
            return;
        }
        PhotoAttachment compressedAttachment = attachment;
        if (compressionManager != null) {
            compressedAttachment = compressionManager.compress(attachment);
        }
        storageRepository.upload(compressedAttachment, callback);
    }
}
