package com.vlabs.routeguard.managers;

import android.content.Context;

import com.vlabs.routeguard.models.PhotoAttachment;

/**
 * Foundation architecture for photo compression prior to upload.
 */
public class PhotoCompressionManager {

    private final Context context;

    public PhotoCompressionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Prepares the architecture for compressing a photo attachment.
     * Currently acts as a pass-through stub.
     *
     * @param attachment The original PhotoAttachment to compress.
     * @return The compressed PhotoAttachment (currently returns the unmodified original).
     */
    public PhotoAttachment compress(PhotoAttachment attachment) {
        if (attachment == null || attachment.getUri() == null) {
            return attachment;
        }

        try {
            android.net.Uri sourceUri = attachment.getUri();
            
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            java.io.InputStream is = context.getContentResolver().openInputStream(sourceUri);
            if (is == null) return attachment;
            android.graphics.BitmapFactory.decodeStream(is, null, options);
            is.close();

            int reqSize = 1024;
            int inSampleSize = 1;
            if (options.outHeight > reqSize || options.outWidth > reqSize) {
                final int halfHeight = options.outHeight / 2;
                final int halfWidth = options.outWidth / 2;
                while ((halfHeight / inSampleSize) >= reqSize && (halfWidth / inSampleSize) >= reqSize) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            is = context.getContentResolver().openInputStream(sourceUri);
            if (is == null) return attachment;
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is, null, options);
            is.close();

            if (bitmap == null) return attachment;

            java.io.File cacheDir = context.getCacheDir();
            java.io.File tempFile = java.io.File.createTempFile("compressed_", ".jpg", cacheDir);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
            bitmap.recycle();

            android.net.Uri compressedUri = android.net.Uri.fromFile(tempFile);
            return new PhotoAttachment.Builder().setUri(compressedUri).build();

        } catch (Exception e) {
            android.util.Log.e("PhotoCompression", "Compression failed", e);
            return attachment;
        }
    }
}
