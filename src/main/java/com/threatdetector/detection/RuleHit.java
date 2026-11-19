package com.threatdetector.detection;

import java.util.List;

/** A rule match before it is converted into a user-visible Finding. */
public record RuleHit(String ruleId, String title, int points, List<String> evidence) {
    public RuleHit {
        evidence = List.copyOf(evidence);
    }
}
