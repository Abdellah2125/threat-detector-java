package com.threatdetector.model;

/** Result of one item, including which worker produced it. */
public record ItemResult(
        int index,
        Integer rowNumber,
        TargetKind kind,
        String target,
        ScanOutcome outcome,
        long durationMs,
        int workerId) {
}
