package com.vlabs.routeguard.managers;

import android.content.Context;

import com.vlabs.routeguard.models.RoadReport;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import com.vlabs.routeguard.managers.ConfidenceEngine;
import com.vlabs.routeguard.managers.ConfidenceEngine.ConfidenceLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarkerManager {

    public interface OnReportClickListener {
        void onReportClick(String reportId);
    }

    private final MapView mapView;
    private final Context context;
    private final Map<String, Marker> activeMarkers;
    private OnReportClickListener listener;
    private Set<String> currentFilters = new HashSet<>();
    private List<RoadReport> currentReports = new ArrayList<>();
    private GeoPoint currentLocation;
    private double currentRadiusKm = 5.0;

    private final android.os.Handler refreshHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            applyUpdates();
            refreshHandler.postDelayed(this, 30_000L);
        }
    };

    public MarkerManager(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.activeMarkers = new HashMap<>();
        refreshHandler.postDelayed(refreshRunnable, 30_000L);
    }
    
    public void destroy() {
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    public void setOnReportClickListener(OnReportClickListener listener) {
        this.listener = listener;
    }

    public void setFilters(Set<String> filters) {
        if (filters != null) {
            this.currentFilters = new HashSet<>(filters);
        } else {
            this.currentFilters = new HashSet<>();
        }
        applyUpdates();
    }

    public void setLocationAndRadius(GeoPoint location, double radiusKm) {
        this.currentLocation = location;
        this.currentRadiusKm = radiusKm;
        applyUpdates();
    }

    public void setReports(List<RoadReport> reports) {
        if (reports != null) {
            this.currentReports = new ArrayList<>(reports);
        } else {
            this.currentReports = new ArrayList<>();
        }
        applyUpdates();
    }

    private boolean isExpired(RoadReport report) {
        // Add 60s grace period
        long EXPIRY_GRACE_MS = 60_000L;
        boolean expired = (report.getExpiresAt() + EXPIRY_GRACE_MS) < System.currentTimeMillis();
        
        android.util.Log.e("MARKER_TEST", "--- EXPIRATION TRACE ---");
        android.util.Log.e("MARKER_TEST", "Report ID: " + report.getReportId());
        android.util.Log.e("MARKER_TEST", "Report Type: " + report.getType());
        android.util.Log.e("MARKER_TEST", "Created Timestamp: " + report.getTimestamp());
        android.util.Log.e("MARKER_TEST", "Expiration Duration: " + (report.getExpiresAt() - report.getTimestamp()));
        android.util.Log.e("MARKER_TEST", "Calculated Expiration Time: " + report.getExpiresAt());
        android.util.Log.e("MARKER_TEST", "Current Time: " + System.currentTimeMillis());
        android.util.Log.e("MARKER_TEST", "isExpired() result: " + expired);
        
        return expired;
    }

    private boolean isWithinRadius(RoadReport report) {
        if (currentLocation == null) {
            android.util.Log.e("MARKER_TEST", "isWithinRadius: currentLocation is null, returning true");
            return true; // Skip filter if no location
        }
        GeoPoint reportLocation = new GeoPoint(report.getLatitude(), report.getLongitude());
        double distanceMeters = currentLocation.distanceToAsDouble(reportLocation);
        boolean isWithin = distanceMeters <= currentRadiusKm * 1000.0;
        android.util.Log.e("MARKER_TEST", "isWithinRadius for " + report.getReportId() + ": distance=" + distanceMeters + "m, limit=" + (currentRadiusKm * 1000.0) + "m. isWithin=" + isWithin);
        return isWithin;
    }

    private List<RoadReport> getVisibleReports() {
        android.util.Log.e("MARKER_TEST", "currentReports.size(): " + currentReports.size());
        List<RoadReport> visible = new ArrayList<>();
        for (RoadReport report : currentReports) {
            // 1. Nearby Radius
            if (!isWithinRadius(report)) {
                android.util.Log.e("MARKER_TEST", "Rejected by Radius: " + report.getReportId());
                continue;
            }

            // 2. Category Filter
            android.util.Log.e("MARKER_TEST", "currentFilters = " + currentFilters);
            android.util.Log.e("MARKER_TEST", "Report Type = " + report.getType());
            if (report.getType() != null) {
                android.util.Log.e("MARKER_TEST", "Report Type Name = " + report.getType().name());
            }
            for (Object filter : currentFilters) {
                android.util.Log.e("MARKER_TEST", "Filter Value = " + filter + " Class = " + filter.getClass().getSimpleName());
            }
            if (!currentFilters.isEmpty() && !currentFilters.contains(report.getType().name())) {
                android.util.Log.e("MARKER_TEST", "Rejected by Category: " + report.getReportId());
                continue;
            }
            
            // 3. Expiration Filter
            if (isExpired(report)) {
                android.util.Log.e("MARKER_TEST", "Rejected by Expiration: " + report.getReportId());
                continue;
            }
            
            android.util.Log.e("MARKER_TEST", "Survives filtering: " + report.getReportId());
            visible.add(report);
        }
        return visible;
    }

    private static class DebugMarker extends Marker {
        public DebugMarker(MapView mapView) {
            super(mapView);
        }

        @Override
        public void draw(android.graphics.Canvas canvas, MapView osmv, boolean shadow) {
            android.util.Log.d("MarkerRendering", "==================================");
            android.util.Log.d("MarkerRendering", "Attempting to draw marker: " + this.getId() + " Hash: " + this.hashCode());
            android.util.Log.d("MarkerRendering", "Map Zoom Level: " + osmv.getZoomLevelDouble());
            org.osmdroid.util.BoundingBox bbox = osmv.getBoundingBox();
            if (bbox != null) {
                android.util.Log.d("MarkerRendering", "Visible Bounding Box: N:" + bbox.getLatNorth() + " S:" + bbox.getLatSouth() + " E:" + bbox.getLonEast() + " W:" + bbox.getLonWest());
                GeoPoint pos = (GeoPoint) this.getPosition();
                if (pos != null) {
                    android.util.Log.d("MarkerRendering", "Marker Geographic Position: Lat:" + pos.getLatitude() + " Lon:" + pos.getLongitude());
                    boolean isOutside = pos.getLatitude() > bbox.getLatNorth() || pos.getLatitude() < bbox.getLatSouth() || 
                                        pos.getLongitude() > bbox.getLonEast() || pos.getLongitude() < bbox.getLonWest();
                    android.util.Log.d("MarkerRendering", "Is marker outside viewport? " + isOutside);
                }
            } else {
                android.util.Log.d("MarkerRendering", "BoundingBox is null!");
            }
            super.draw(canvas, osmv, shadow);
        }
    }

    private void applyUpdates() {
        android.util.Log.e("MARKER_TEST", "Reached applyUpdates()");
        List<RoadReport> visibleReports = getVisibleReports();
        android.util.Log.e("MARKER_TEST", "visibleReports.size(): " + visibleReports.size());
        Set<String> incomingIds = new HashSet<>();
        
        for (RoadReport report : visibleReports) {
            String reportId = report.getReportId();
            incomingIds.add(reportId);
            
            android.util.Log.d("MarkerRendering", "----------------------------------");
            android.util.Log.d("MarkerRendering", "REPORT LOADED: " + reportId);
            android.util.Log.d("MarkerRendering", "Latitude: " + report.getLatitude());
            android.util.Log.d("MarkerRendering", "Longitude: " + report.getLongitude());
            android.util.Log.d("MarkerRendering", "Confidence: " + report.getConfidenceScore());

            if (activeMarkers.containsKey(reportId)) {
                Marker marker = activeMarkers.get(reportId);
                marker.setPosition(new GeoPoint(report.getLatitude(), report.getLongitude()));
                
                android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation);
                marker.setIcon(icon);
                
                android.util.Log.d("MarkerRendering", "Marker icon intrinsic width: " + (icon != null ? icon.getIntrinsicWidth() : "null"));
                android.util.Log.d("MarkerRendering", "Marker icon intrinsic height: " + (icon != null ? icon.getIntrinsicHeight() : "null"));
                try {
                    android.util.Log.d("MarkerRendering", "Marker alpha: " + marker.getAlpha());
                } catch (Exception e) {
                    android.util.Log.d("MarkerRendering", "Marker alpha: not supported in this osmdroid version");
                }
                android.util.Log.d("MarkerRendering", "Marker position: " + marker.getPosition().getLatitude() + "," + marker.getPosition().getLongitude());
                android.util.Log.d("MarkerRendering", "Marker anchor: 0.5, 0.5");
                android.util.Log.d("MarkerRendering", "Overlay count: " + mapView.getOverlays().size());
                
                marker.setInfoWindow(null);
                marker.setOnMarkerClickListener((m, map) -> {
                    if (listener != null) listener.onReportClick(reportId);
                    return true;
                });
            } else {
                android.util.Log.e("MARKER_TEST", "Report ID: " + reportId);
                android.util.Log.e("MARKER_TEST", "Latitude: " + report.getLatitude());
                android.util.Log.e("MARKER_TEST", "Longitude: " + report.getLongitude());
                DebugMarker marker = new DebugMarker(mapView);
                android.util.Log.e("MARKER_TEST", "Marker Created");
                marker.setPosition(new GeoPoint(report.getLatitude(), report.getLongitude()));
                
                android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation);
                marker.setIcon(icon);
                
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setId(reportId);
                marker.setInfoWindow(null);
                marker.setOnMarkerClickListener((m, map) -> {
                    if (listener != null) listener.onReportClick(reportId);
                    return true; 
                });
                
                android.util.Log.d("MarkerRendering", "Marker icon intrinsic width: " + (icon != null ? icon.getIntrinsicWidth() : "null"));
                android.util.Log.d("MarkerRendering", "Marker icon intrinsic height: " + (icon != null ? icon.getIntrinsicHeight() : "null"));
                try {
                    android.util.Log.d("MarkerRendering", "Marker alpha: " + marker.getAlpha());
                } catch (Exception e) {
                    android.util.Log.d("MarkerRendering", "Marker alpha: not supported");
                }
                android.util.Log.d("MarkerRendering", "Marker position: " + marker.getPosition().getLatitude() + "," + marker.getPosition().getLongitude());
                android.util.Log.d("MarkerRendering", "Marker anchor: 0.5, 0.5");
                android.util.Log.d("MarkerRendering", "Overlay count BEFORE add: " + mapView.getOverlays().size());
                
                activeMarkers.put(reportId, marker);
                mapView.getOverlays().add(marker);
                android.util.Log.e("MARKER_TEST", "Marker added for Report ID = " + reportId);
                android.util.Log.e("MARKER_TEST", "Current activeMarkers.size(): " + activeMarkers.size());
                android.util.Log.e("MARKER_TEST", "Current mapView.getOverlays().size(): " + mapView.getOverlays().size());
                
                android.util.Log.d("MarkerRendering", "Immediately after mapView.getOverlays().add(marker):");
                android.util.Log.d("MarkerRendering", "Marker hashCode: " + marker.hashCode());
                int index = mapView.getOverlays().indexOf(marker);
                android.util.Log.d("MarkerRendering", "Overlay index: " + index);
                if (index >= 0) {
                    android.util.Log.d("MarkerRendering", "Overlay class: " + mapView.getOverlays().get(index).getClass().getName());
                }
            }
        }
        
        android.util.Log.e("MARKER_TEST", "Final activeMarkers.size(): " + activeMarkers.size());
        android.util.Log.e("MARKER_TEST", "Final overlay count: " + mapView.getOverlays().size());

        Iterator<Map.Entry<String, Marker>> iterator = activeMarkers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            if (!incomingIds.contains(entry.getKey())) {
                mapView.getOverlays().remove(entry.getValue());
                iterator.remove();
            }
        }

        android.util.Log.e("MARKER_TEST", "Calling mapView.invalidate()");
        mapView.invalidate();
    }
}
