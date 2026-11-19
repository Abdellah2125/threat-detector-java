package com.threatdetector.model;

import java.util.List;

/** Complete report returned after every worker has joined. */
public record ScanReport(
        int scanId,
        List<ItemResult> results,
        Summary summary,
        int workersUsed,
        long durationMs) {
    public ScanReport {
        results = List.copyOf(results);
    }
}
