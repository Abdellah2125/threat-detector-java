package com.threatdetector.runtime;

import com.threatdetector.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {

    private static ScanItem item(String target) {
        return new ScanItem(0, null, TargetKind.URL, target);
    }

    @Test
    void local_service_scores_a_hostile_url() {
        ScanOutcome outcome = new LocalRuleEvaluationService().evaluate(item("http://192.168.1.10/login"));
        assertEquals(55, ((ScanOutcome.Scored) outcome).score());
    }

    @Test
    void missing_api_key_is_reported_as_unavailable() {
        CheckUnavailableException error = assertThrows(CheckUnavailableException.class,
                () -> new ExternalApiEvaluationService(ExternalApiEvaluationService.Mode.MISSING_KEY)
                        .evaluate(item("https://example.com")));
        assertEquals("API_KEY_MISSING", error.code());
    }

    @Test
    void rate_limit_is_reported_as_unavailable() {
        CheckUnavailableException error = assertThrows(CheckUnavailableException.class,
                () -> new ExternalApiEvaluationService(ExternalApiEvaluationService.Mode.RATE_LIMITED)
                        .evaluate(item("https://example.com")));
        assertEquals("API_RATE_LIMITED", error.code());
    }

    @Test
    void timeout_is_reported_as_unavailable() {
        CheckUnavailableException error = assertThrows(CheckUnavailableException.class,
                () -> new ExternalApiEvaluationService(ExternalApiEvaluationService.Mode.TIMEOUT)
                        .evaluate(item("https://example.com")));
        assertEquals("API_TIMEOUT", error.code());
    }

    @Test
    void ai_layer_fails_gracefully_when_unavailable() {
        AiExplanationService ai = new AiExplanationService(false);
        assertTrue(ai.explain(ScanOutcome.scored(10, RiskLevel.LOW, List.of())).isEmpty());
    }

    @Test
    void ai_layer_is_only_a_secondary_assessment() {
        AiExplanationService ai = new AiExplanationService(true);
        ScanOutcome.Scored scored = (ScanOutcome.Scored) new LocalRuleEvaluationService()
                .evaluate(item("http://192.168.1.10/login"));
        assertTrue(ai.explain(scored).isPresent());
        assertEquals(55, scored.score());
    }

    @Test
    void coordinator_preserves_invalid_csv_rows() throws Exception {
        com.threatdetector.intake.CsvLoader loader = new com.threatdetector.intake.CsvLoader();
        CsvLoadReport report = loader.parse("batch.csv", """
                type,target
                url,https://example.com
                unknown,https://example.com
                """);
        ScanCoordinator coordinator = new ScanCoordinator(loader, new ScanEngine(),
                new LocalRuleEvaluationService(), new AiExplanationService(false));
        ScanReport result = coordinator.scanBatch(report, ScanObserver.NO_OP);
        assertEquals(2, result.results().size());
        assertEquals("INVALID_CSV_ROW",
                ((ScanOutcome.NotVerified) result.results().get(1).outcome()).code());
    }
}
