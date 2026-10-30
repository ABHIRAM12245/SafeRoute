package com.vlabs.routeguard.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vlabs.routeguard.managers.TravelModeManager;

/**
 * System Boot Broadcast Receiver.
 *
 * Listens for the {@link Intent#ACTION_BOOT_COMPLETED} system broadcast after the Android device restarts.
 *
 * Responsibility & Flow:
 * - Registered in {@code AndroidManifest.xml} with permission {@code RECEIVE_BOOT_COMPLETED}.
 * - Invokes {@link TravelModeManager#handleDeviceReboot(Context)} upon device startup.
 * - If Travel Mode was active prior to reboot and its 3-hour expiry has not elapsed,
 *   reschedules the {@link com.vlabs.routeguard.workers.TravelModeWorker} periodic background job;
 *   otherwise, purges stale Travel Mode preferences.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "BOOT_COMPLETED received. Checking Travel Mode status...");
            // Refinement #8: Handle device reboot by validating expiry and rescheduling WorkManager only if active
            TravelModeManager.getInstance().handleDeviceReboot(context);
        }
    }
}
