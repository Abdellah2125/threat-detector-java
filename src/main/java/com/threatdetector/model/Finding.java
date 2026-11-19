package com.threatdetector.model;

import java.util.List;

/** One rule that fired, with the fragment that matched it. */
public record Finding(String ruleId, String title, int points, List<String> evidence) {
    public Finding {
        evidence = List.copyOf(evidence);
    }
}
