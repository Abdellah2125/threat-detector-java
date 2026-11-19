package com.threatdetector.model;

/** A numbered unit of work handed to the worker pool. */
public record ScanItem(int index, Integer rowNumber, TargetKind kind, String target) {
}
