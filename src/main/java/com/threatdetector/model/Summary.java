package com.threatdetector.model;

import java.util.List;

/** Aggregate counters for a scan. */
public record Summary(int total, int scored, int high, int medium, int low, int notVerified) {

    public static Summary summarize(List<ScanOutcome> outcomes) {
        int high = 0, medium = 0, low = 0, notVerified = 0;
        for (ScanOutcome outcome : outcomes) {
            if (outcome instanceof ScanOutcome.NotVerified) {
                notVerified++;
            } else if (outcome instanceof ScanOutcome.Scored scored) {
                switch (scored.risk()) {
                    case HIGH -> high++;
                    case MEDIUM -> medium++;
                    case LOW -> low++;
                }
            }
        }
        return new Summary(outcomes.size(), outcomes.size() - notVerified, high, medium, low, notVerified);
    }
}
