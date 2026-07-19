package com.vlabs.routeguard.managers;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.Marker;
import android.content.Context;
import androidx.core.content.ContextCompat;
import com.vlabs.routeguard.R;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.overlay.MapEventsOverlay;

import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import android.location.Location;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;

public class MapManager {

    private final MapView mapView;
    private final IMapController mapController;
    private RotationGestureOverlay rotationGestureOverlay;
    private MyLocationNewOverlay myLocationOverlay;
    private CustomLocationProvider customLocationProvider;
    private final MarkerManager markerManager;

    public MapManager(Context context, MapView mapView) {
        this.mapView = mapView;
        this.mapController = mapView.getController();
        this.markerManager = new MarkerManager(context, mapView);
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

    public void setCurrentLocationMarker(Context context, GeoPoint point) {
        if (point == null) return;
        if (myLocationOverlay == null) {
            customLocationProvider = new CustomLocationProvider();
            myLocationOverlay = new MyLocationNewOverlay(customLocationProvider, mapView);
            
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_current_location_marker);
            Bitmap bitmap = null;
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable != null) {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            if (bitmap != null) {
                myLocationOverlay.setPersonIcon(bitmap);
                myLocationOverlay.setDirectionIcon(bitmap);
            }
            
            myLocationOverlay.enableMyLocation();
            mapView.getOverlays().add(myLocationOverlay);
        }
        
        Location location = new Location("CustomProvider");
        location.setLatitude(point.getLatitude());
        location.setLongitude(point.getLongitude());
        location.setAccuracy(15.0f); // Default accuracy for visual circle
        
        customLocationProvider.updateLocation(location);
        mapView.invalidate();
    }

    private static class CustomLocationProvider implements IMyLocationProvider {
        private IMyLocationConsumer myLocationConsumer;
        private Location lastLocation;

        @Override
        public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
            this.myLocationConsumer = myLocationConsumer;
            return true;
        }

        @Override
        public void stopLocationProvider() {
            this.myLocationConsumer = null;
        }

        @Override
        public Location getLastKnownLocation() {
            return lastLocation;
        }

        @Override
        public void destroy() {
            this.myLocationConsumer = null;
        }

        public void updateLocation(Location location) {
            lastLocation = location;
            if (myLocationConsumer != null) {
                myLocationConsumer.onLocationChanged(location, this);
            }
        }
    }

    public void setOnMapLongClickListener(MapEventsReceiver receiver) {
        MapEventsOverlay overlay = new MapEventsOverlay(receiver);
        mapView.getOverlays().add(0, overlay); // Add at bottom so markers can still be clicked
    }

    public void setReports(java.util.List<com.vlabs.routeguard.models.RoadReport> reports) {
        markerManager.setReports(reports);
    }

    public void setOnReportClickListener(MarkerManager.OnReportClickListener listener) {
        markerManager.setOnReportClickListener(listener);
    }

    public void setFilters(java.util.Set<String> filters) {
        markerManager.setFilters(filters);
    }

    public void setLocationAndRadius(GeoPoint location, double radiusKm) {
        markerManager.setLocationAndRadius(location, radiusKm);
    }
    
    public void destroy() {
        if (markerManager != null) {
            markerManager.destroy();
        }
    }
}
