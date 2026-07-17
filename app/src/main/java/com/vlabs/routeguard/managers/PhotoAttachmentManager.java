package com.vlabs.routeguard.managers;

import com.vlabs.routeguard.models.PhotoAttachment;

/**
 * Manages the temporary state of a photo attachment during the report creation flow.
 */
public class PhotoAttachmentManager {

    private PhotoAttachment currentAttachment;

    public void setAttachment(PhotoAttachment attachment) {
        this.currentAttachment = attachment;
    }

    public PhotoAttachment getAttachment() {
        return currentAttachment;
    }

    public boolean hasAttachment() {
        return currentAttachment != null;
    }

    public void clear() {
        this.currentAttachment = null;
    }
}
