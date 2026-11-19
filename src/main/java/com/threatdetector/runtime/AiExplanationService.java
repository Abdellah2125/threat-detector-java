package com.threatdetector.runtime;

import com.threatdetector.model.Finding;
import com.threatdetector.model.ScanOutcome;

import java.util.Optional;

/**
 * Optional extra credit: a secondary explanation layer. It never changes the
 * primary score and fails gracefully when the model/service is unavailable.
 */
public final class AiExplanationService {
    private final boolean available;

    public AiExplanationService(boolean available) {
        this.available = available;
    }

    public Optional<String> explain(ScanOutcome outcome) {
        if (!available || !(outcome instanceof ScanOutcome.Scored scored)) {
            return Optional.empty();
        }
        if (scored.findings().isEmpty()) {
            return Optional.of("No named rule fired. This is not a safety guarantee.");
        }
        StringBuilder explanation = new StringBuilder("Signals: ");
        for (Finding finding : scored.findings()) {
            explanation.append(finding.ruleId()).append(' ');
        }
        return Optional.of(explanation.toString().trim());
    }
}
