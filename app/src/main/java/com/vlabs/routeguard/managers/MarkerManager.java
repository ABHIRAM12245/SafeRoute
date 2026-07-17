package com.vlabs.routeguard.managers;

import android.content.Context;

import com.vlabs.routeguard.models.RoadReport;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkerManager {

    public interface OnReportClickListener {
        void onReportClick(RoadReport report);
    }

    private final MapView mapView;
    private final Context context;
    private final Map<String, Marker> activeMarkers;
    private OnReportClickListener listener;

    public MarkerManager(Context context, MapView mapView) {
        this.context = context;
        this.mapView = mapView;
        this.activeMarkers = new HashMap<>();
    }

    public void setOnReportClickListener(OnReportClickListener listener) {
        this.listener = listener;
    }

    public void setReports(List<RoadReport> reports) {
        // Clear existing markers (future iterations will update incrementally)
        for (Marker marker : activeMarkers.values()) {
            mapView.getOverlays().remove(marker);
        }
        activeMarkers.clear();

        if (reports == null || reports.isEmpty()) {
            mapView.invalidate();
            return;
        }

        // Add new markers
        for (RoadReport report : reports) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(report.getLatitude(), report.getLongitude()));
            marker.setIcon(MarkerFactory.getMarkerIcon(context, report.getType()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            
            // Set ID for debugging or future reference
            marker.setId(report.getReportId());

            marker.setOnMarkerClickListener((m, map) -> {
                // Return true to consume the event and prevent default InfoWindow
                if (listener != null) {
                    listener.onReportClick(report);
                }
                return true; 
            });

            activeMarkers.put(report.getReportId(), marker);
            mapView.getOverlays().add(marker);
        }

        mapView.invalidate();
    }
}
