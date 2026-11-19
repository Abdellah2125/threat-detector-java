package com.threatdetector.model;

import java.util.List;

/** Result of reading a CSV batch, including user-facing warnings. */
public record CsvLoadReport(
        String fileName,
        List<LoadedRow> rows,
        int validCount,
        int invalidCount,
        List<String> warnings) {
    public CsvLoadReport {
        rows = List.copyOf(rows);
        warnings = List.copyOf(warnings);
    }
}
