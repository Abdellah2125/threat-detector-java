package com.threatdetector.model;

/** Telemetry events streamed to the UI while a scan runs. */
public final class ScanEvents {
    private ScanEvents() {
    }

    public record ScanStartedEvent(int scanId, int total, int workers) {
    }

    public record ScanItemEvent(int scanId, int index, int completed, int total, ItemResult result) {
    }

    public record ScanFinishedEvent(int scanId, Summary summary, int workersUsed, long durationMs) {
    }
}
