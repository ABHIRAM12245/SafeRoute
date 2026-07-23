package com.vlabs.routeguard.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.vlabs.routeguard.managers.TravelModeManager;

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
