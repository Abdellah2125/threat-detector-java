package com.threatdetector.model;

/** An input supplied by the user or by a CSV row. */
public record ScanInput(TargetKind kind, String target, Integer rowNumber) {
}
