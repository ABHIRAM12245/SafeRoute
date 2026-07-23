package com.vlabs.routeguard.managers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Foundation architecture for handling camera permissions and settings redirects.
 */
public class CameraPermissionManager {

    private final Context context;

    public CameraPermissionManager(Context context) {
        this.context = context;
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

    /**
     * Checks if a rationale dialog should be shown before requesting camera permission.
     *
     * @param activity The host activity.
     * @return true if rationale should be shown.
     */
    public boolean shouldShowRationale(Activity activity) {
        if (activity == null) return false;
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
    }

    /**
     * Redirects the user to App Settings to enable camera permission manually.
     *
     * @param activity The host activity.
     */
    public void openAppSettings(Activity activity) {
        if (activity == null) return;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}
