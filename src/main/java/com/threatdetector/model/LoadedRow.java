package com.threatdetector.model;

/** A validated CSV row; invalid rows keep a NotVerified outcome. */
public record LoadedRow(int rowNumber, TargetKind kind, String target, ScanOutcome outcome) {
}
