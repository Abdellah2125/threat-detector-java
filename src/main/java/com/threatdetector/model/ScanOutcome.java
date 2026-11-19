package com.threatdetector.model;

import java.util.List;

/**
 * The result of evaluating one item.
 *
 * <p>An item that cannot be verified is NEVER represented as a scored/low
 * outcome: it always carries a distinct NotVerified status.</p>
 */
public sealed interface ScanOutcome permits ScanOutcome.Scored, ScanOutcome.NotVerified {

    record Scored(int score, RiskLevel risk, List<Finding> findings) implements ScanOutcome {
        public Scored {
            findings = List.copyOf(findings);
        }
    }

    record NotVerified(String code, String reason) implements ScanOutcome {
    }

    static Scored scored(int score, RiskLevel risk, List<Finding> findings) {
        return new Scored(score, risk, findings);
    }

    static NotVerified notVerified(String code, String reason) {
        return new NotVerified(code, reason);
    }
}
