package com.vlabs.routeguard.models;

public class ConfirmationCount {
    private int activeCount;
    private int goneCount;
    private long lastConfirmedTimestamp;

    public ConfirmationCount(int activeCount, int goneCount, long lastConfirmedTimestamp) {
        this.activeCount = activeCount;
        this.goneCount = goneCount;
        this.lastConfirmedTimestamp = lastConfirmedTimestamp;
    }

    public int getActiveCount() { return activeCount; }
    public int getGoneCount() { return goneCount; }
    public long getLastConfirmedTimestamp() { return lastConfirmedTimestamp; }
}
