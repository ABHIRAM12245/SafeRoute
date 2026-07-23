package com.vlabs.routeguard.managers;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;

/**
 * Foundation architecture for live camera capture.
 * Handles the creation and cleanup of temporary photo files.
 */
public class CameraCaptureManager {

    private final Context context;
    private File tempPhotoFile;
    private Uri tempPhotoUri;

    public CameraCaptureManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public void createTempPhoto() throws IOException {
        File cacheDir = context.getCacheDir();
        tempPhotoFile = File.createTempFile("TEMP_PHOTO_", ".jpg", cacheDir);
        tempPhotoUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".fileprovider",
                tempPhotoFile
        );
    }

    public Uri getTempPhotoUri() {
        return tempPhotoUri;
    }

    public void deleteTempPhoto() {
        if (tempPhotoFile != null && tempPhotoFile.exists()) {
            tempPhotoFile.delete();
        }
        tempPhotoFile = null;
        tempPhotoUri = null;
    }
}
