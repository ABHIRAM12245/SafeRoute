package com.vlabs.routeguard.models;

import android.net.Uri;

/**
 * Represents a photo attached to a road report.
 * Uses the Builder pattern to allow easy addition of future fields (e.g., size, format, metadata)
 * without breaking the existing API.
 */
public class PhotoAttachment {
    private final Uri uri;

    private PhotoAttachment(Builder builder) {
        this.uri = builder.uri;
    }

    public Uri getUri() {
        return uri;
    }

    public static class Builder {
        private Uri uri;

        public Builder setUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        public PhotoAttachment build() {
            return new PhotoAttachment(this);
        }
    }
}
