package com.threatdetector.detection;

import com.threatdetector.model.Finding;
import com.threatdetector.model.RiskLevel;
import com.threatdetector.model.ScanOutcome;

/** Maps cumulative rule points to a risk band. Pure and deterministic. */
public final class Scoring {
    public static final int HIGH_THRESHOLD = 60;
    public static final int MEDIUM_THRESHOLD = 25;
    public static final int MAX_SCORE = 100;

    private Scoring() {
    }

    public static ScanOutcome.Scored finalize(int rawScore, java.util.List<Finding> findings) {
        int score = Math.min(MAX_SCORE, Math.max(0, rawScore));
        RiskLevel risk = score >= HIGH_THRESHOLD ? RiskLevel.HIGH
                : score >= MEDIUM_THRESHOLD ? RiskLevel.MEDIUM
                : RiskLevel.LOW;
        return new ScanOutcome.Scored(score, risk, findings);
    }
}
