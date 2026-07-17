package com.vlabs.routeguard.managers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.vlabs.routeguard.models.PhotoAttachment;

import java.io.File;
import java.io.InputStream;

/**
 * Foundation architecture for validating photo attachments.
 */
public class PhotoValidationManager {

    private final Context context;

    public PhotoValidationManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Validates that the photo attachment exists, is readable, and has a size > 0.
     *
     * @param attachment The PhotoAttachment to validate.
     * @return true if valid, false otherwise.
     */
    public boolean validatePhoto(PhotoAttachment attachment) {
        if (attachment == null || attachment.getUri() == null) {
            return false;
        }
        
        Uri uri = attachment.getUri();

        // Path for "file://" URIs
        if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
            File file = new File(uri.getPath());
            return file.exists() && file.canRead() && file.length() > 0;
        }
        
        // Path for "content://" URIs
        return validateContentUri(uri);
    }
    
    private boolean validateContentUri(Uri uri) {
        boolean isReadable = false;
        
        // Check if file exists and is readable
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is != null) {
                isReadable = true;
            }
        } catch (Exception e) {
            return false;
        }
        
        if (!isReadable) {
            return false;
        }
        
        // Check if file size is > 0
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    long size = cursor.getLong(sizeIndex);
                    return size > 0;
                }
            }
        } catch (Exception e) {
            // Return false if size verification fails
        }
        
        return false;
    }
}
