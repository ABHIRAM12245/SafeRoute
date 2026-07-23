package com.vlabs.routeguard.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vlabs.routeguard.managers.SessionManager;
import com.vlabs.routeguard.managers.TravelModeManager;
import com.vlabs.routeguard.managers.UserLocationManager;
import com.vlabs.routeguard.models.RoadReport;
import com.vlabs.routeguard.repositories.ReportRepository;

import org.osmdroid.util.GeoPoint;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TravelModeWorker extends Worker {

    private static final String TAG = "TravelModeWorker";

    public TravelModeWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        TravelModeManager travelModeManager = TravelModeManager.getInstance();

        // 1. Check if Travel Mode is active
        if (!travelModeManager.isTravelModeActive(context)) {
            Log.d(TAG, "Travel mode is not active. Stopping worker execution.");
            travelModeManager.disableTravelMode(context);
            return Result.success();
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = currentUser != null ? currentUser.getUid() : "";

        CountDownLatch latch = new CountDownLatch(1);
        UserLocationManager locationManager = new UserLocationManager(context);

        // 2. Fetch current background location
        locationManager.checkAndFetchLocation(new UserLocationManager.LocationCallback() {
            @Override
            public void onLocationFound(GeoPoint point) {
                if (point != null) {
                    // Fetch active reports from ReportRepository
                    new ReportRepository().fetchRoadReports(new ReportRepository.ReportCallback() {
                        @Override
                        public void onSuccess(List<RoadReport> reports) {
                            // Refinement #3: Lightweight worker delegating business logic to TravelModeManager
                            travelModeManager.evaluateAndNotifyReports(
                                    context,
                                    point.getLatitude(),
                                    point.getLongitude(),
                                    reports,
                                    currentUserId
                            );
                            latch.countDown();
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Failed to fetch reports in background worker: " + error);
                            latch.countDown();
                        }
                    });
                } else {
                    latch.countDown();
                }
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Background location error: " + error);
                latch.countDown();
            }

            @Override
            public void onGpsDisabled() {
                Log.w(TAG, "GPS disabled during background work execution.");
                latch.countDown();
            }
        });

        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted while waiting for location/reports", e);
        }

        return Result.success();
    }
}
