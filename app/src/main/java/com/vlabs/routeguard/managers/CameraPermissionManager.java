package com.vlabs.routeguard.managers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

/**
 * Foundation architecture for handling camera permissions.
 */
public class CameraPermissionManager {

    private final Context context;

    public CameraPermissionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Checks if the camera permission is currently granted.
     * 
     * @return true if granted, false otherwise.
     */
    public boolean isCameraPermissionGranted() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }
}
