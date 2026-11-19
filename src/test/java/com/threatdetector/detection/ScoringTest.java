package com.threatdetector.detection;

import com.threatdetector.model.Finding;
import com.threatdetector.model.RiskLevel;
import com.threatdetector.model.ScanOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoringTest {

    private static Finding finding(String id, int points) {
        return new Finding(id, id, points, List.of("evidence"));
    }

    @Test
    void score_is_capped_at_one_hundred() {
        ScanOutcome.Scored result = Scoring.finalize(160, List.of(finding("a", 80), finding("b", 80)));
        assertEquals(100, result.score());
        assertEquals(RiskLevel.HIGH, result.risk());
    }

    @Test
    void sixty_is_high() {
        assertEquals(RiskLevel.HIGH, Scoring.finalize(60, List.of()).risk());
    }

    @Test
    void twenty_five_is_medium() {
        assertEquals(RiskLevel.MEDIUM, Scoring.finalize(25, List.of()).risk());
    }

    @Test
    void twenty_four_is_low() {
        assertEquals(RiskLevel.LOW, Scoring.finalize(24, List.of()).risk());
    }

    @Test
    void negative_score_is_clamped_to_zero() {
        assertEquals(0, Scoring.finalize(-5, List.of()).score());
    }
}
