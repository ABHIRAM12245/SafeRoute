package com.vlabs.routeguard.models;

import android.net.Uri;
import org.osmdroid.util.GeoPoint;

public class RoadReportDraft {
    private GeoPoint location;
    private ReportType category;
    private String description;
    private PhotoAttachment photoAttachment;
    private long timestamp;

    public RoadReportDraft() {
        this.timestamp = System.currentTimeMillis();
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public ReportType getCategory() {
        return category;
    }

    public void setCategory(ReportType category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Uri getPhotoUri() {
        return photoAttachment != null ? photoAttachment.getUri() : null;
    }

    public void setPhotoUri(Uri photoUri) {
        if (photoUri == null) {
            this.photoAttachment = null;
        } else {
            this.photoAttachment = new PhotoAttachment.Builder().setUri(photoUri).build();
        }
    }

    public PhotoAttachment getPhotoAttachment() {
        return photoAttachment;
    }

    public void setPhotoAttachment(PhotoAttachment photoAttachment) {
        this.photoAttachment = photoAttachment;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
