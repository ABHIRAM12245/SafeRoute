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
        void onReportClick(java.util.List<String> reportIds);
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
        if (report == null) return true;
        long expiresAt = report.getExpiresAt();
        long now = System.currentTimeMillis();
        boolean expired = (expiresAt > 0L && now >= expiresAt);

        android.util.Log.d("LIFECYCLE_EXPIRY_TRACE", "[MarkerManager] Report ID: " + report.getReportId()
                + " | createdAt: " + report.getTimestamp()
                + " | expiresAt: " + expiresAt
                + " | currentTime: " + now
                + " | isExpired: " + expired);

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

    private static class ReportCluster {
        String clusterId;
        GeoPoint center;
        List<RoadReport> reports = new ArrayList<>();
    }
    
    private android.graphics.drawable.Drawable createBadgeIcon(android.content.Context context, android.graphics.drawable.Drawable baseIcon, int count) {
        if (count <= 1 || baseIcon == null) return baseIcon;
        
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                Math.max(baseIcon.getIntrinsicWidth(), 100), 
                Math.max(baseIcon.getIntrinsicHeight(), 100), 
                android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        baseIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        baseIcon.draw(canvas);
        
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.RED);
        paint.setStyle(android.graphics.Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        float radius = 25f;
        float cx = canvas.getWidth() - radius;
        float cy = radius;
        canvas.drawCircle(cx, cy, radius, paint);
        
        paint.setColor(android.graphics.Color.WHITE);
        paint.setTextSize(30f);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        
        android.graphics.Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = cy - ((fm.descent + fm.ascent) / 2);
        
        canvas.drawText(String.valueOf(count), cx, textY, paint);
        
        return new android.graphics.drawable.BitmapDrawable(context.getResources(), bitmap);
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
        List<RoadReport> visibleReports = getVisibleReports();
        
        List<ReportCluster> clusters = new ArrayList<>();
        for (RoadReport report : visibleReports) {
            boolean added = false;
            for (ReportCluster cluster : clusters) {
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                    cluster.center.getLatitude(), cluster.center.getLongitude(),
                    report.getLatitude(), report.getLongitude(), results);
                if (results[0] <= 100) {
                    cluster.reports.add(report);
                    added = true;
                    break;
                }
            }
            if (!added) {
                ReportCluster newCluster = new ReportCluster();
                newCluster.clusterId = report.getReportId();
                newCluster.center = new GeoPoint(report.getLatitude(), report.getLongitude());
                newCluster.reports.add(report);
                clusters.add(newCluster);
            }
        }
        
        Set<String> incomingIds = new HashSet<>();
        
        for (ReportCluster cluster : clusters) {
            String clusterId = cluster.clusterId;
            incomingIds.add(clusterId);
            
            RoadReport primaryReport = cluster.reports.get(0);
            int count = cluster.reports.size();
            ArrayList<String> clusterReportIds = new ArrayList<>();
            for (RoadReport r : cluster.reports) clusterReportIds.add(r.getReportId());
            
            if (activeMarkers.containsKey(clusterId)) {
                Marker marker = activeMarkers.get(clusterId);
                marker.setPosition(cluster.center);
                
                ConfidenceLevel level = ConfidenceEngine.getConfidenceLevel(primaryReport.getConfidenceScore());
                android.graphics.drawable.Drawable icon = MarkerFactory.getMarkerIcon(context, primaryReport.getType(), level);
                marker.setIcon(createBadgeIcon(context, icon, count));
                
                marker.setInfoWindow(null);
                marker.setOnMarkerClickListener((m, map) -> {
                    if (listener != null) listener.onReportClick(clusterReportIds);
                    return true;
                });
            } else {
                DebugMarker marker = new DebugMarker(mapView);
                marker.setPosition(cluster.center);
                
                ConfidenceLevel level = ConfidenceEngine.getConfidenceLevel(primaryReport.getConfidenceScore());
                android.graphics.drawable.Drawable icon = MarkerFactory.getMarkerIcon(context, primaryReport.getType(), level);
                marker.setIcon(createBadgeIcon(context, icon, count));
                
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                marker.setId(clusterId);
                marker.setInfoWindow(null);
                marker.setOnMarkerClickListener((m, map) -> {
                    if (listener != null) listener.onReportClick(clusterReportIds);
                    return true; 
                });
                
                activeMarkers.put(clusterId, marker);
                mapView.getOverlays().add(marker);
            }
        }


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
