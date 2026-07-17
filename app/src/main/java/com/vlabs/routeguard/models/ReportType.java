package com.vlabs.routeguard.models;

public enum ReportType {
    POLICE_CHECKING("Police Checking"),
    ACCIDENT("Accident"),
    HEAVY_TRAFFIC("Heavy Traffic"),
    ROAD_CONSTRUCTION("Road Construction"),
    ANIMAL_ON_ROAD("Animal on Road"),
    WATER_LOGGING("Water Logging"),
    OTHER("Other");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
