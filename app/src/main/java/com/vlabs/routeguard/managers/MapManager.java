package com.vlabs.routeguard.managers;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

public class MapManager {

    private final MapView mapView;
    private final IMapController mapController;
    private RotationGestureOverlay rotationGestureOverlay;

    public MapManager(MapView mapView) {
        this.mapView = mapView;
        this.mapController = mapView.getController();
    }

    public void initialize() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setTilesScaledToDpi(true);
        mapView.setFlingEnabled(true);
    }

    public void setCenter(GeoPoint point) {
        if (point != null) {
            mapController.setCenter(point);
        }
    }

    public void animateTo(GeoPoint point, double zoomLevel, long speedMs) {
        if (point != null) {
            mapController.animateTo(point, zoomLevel, speedMs);
        }
    }

    public void setZoom(double zoomLevel) {
        mapController.setZoom(zoomLevel);
    }

    public void enableRotation() {
        if (rotationGestureOverlay == null) {
            rotationGestureOverlay = new RotationGestureOverlay(mapView);
        }
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);
    }

    public void clearMarkers() {
        // Simple implementation to remove markers or specific overlays.
        // For a full implementation, you'd filter out specific marker classes.
        mapView.getOverlays().clear();
        if (rotationGestureOverlay != null && rotationGestureOverlay.isEnabled()) {
            mapView.getOverlays().add(rotationGestureOverlay);
        }
        mapView.invalidate();
    }
}
