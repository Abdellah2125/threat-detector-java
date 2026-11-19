package com.threatdetector.runtime;

import com.threatdetector.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScanEngineTest {

    private List<ScanItem> items(int count) {
        List<ScanItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new ScanItem(i, i + 2, TargetKind.URL, "https://example.com/" + i));
        }
        return items;
    }

    private static ScanObserver countingObserver(AtomicInteger started, AtomicInteger itemEvents,
                                                 AtomicInteger finished) {
        return new ScanObserver() {
            @Override
            public void onStarted(ScanEvents.ScanStartedEvent event) {
                started.incrementAndGet();
            }

            @Override
            public void onItem(ScanEvents.ScanItemEvent event) {
                itemEvents.incrementAndGet();
            }

            @Override
            public void onFinished(ScanEvents.ScanFinishedEvent event) {
                finished.incrementAndGet();
            }
        };
    }

    @Test
    void empty_input_returns_zero_workers() {
        ScanReport report = new ScanEngine().scan(List.of(), item -> {
            throw new AssertionError("should not be called");
        }, ScanObserver.NO_OP);
        assertEquals(0, report.workersUsed());
        assertTrue(report.results().isEmpty());
    }

    @Test
    void workers_are_capped_at_item_count() {
        ScanReport report = new ScanEngine().scan(items(2),
                item -> ScanOutcome.scored(0, RiskLevel.LOW, List.of()), ScanObserver.NO_OP);
        assertEquals(2, report.workersUsed());
    }

    @Test
    void results_keep_intake_order() {
        ScanReport report = new ScanEngine().scan(items(40),
                item -> ScanOutcome.scored(0, RiskLevel.LOW, List.of()), ScanObserver.NO_OP);
        for (int i = 0; i < 40; i++) {
            assertEquals(i, report.results().get(i).index());
        }
    }

    @Test
    void worker_ids_stay_within_pool_size() {
        ScanReport report = new ScanEngine().scan(items(25),
                item -> ScanOutcome.scored(0, RiskLevel.LOW, List.of()), ScanObserver.NO_OP);
        assertTrue(report.results().stream().allMatch(r -> r.workerId() >= 0 && r.workerId() < 4));
    }

    @Test
    void panic_is_isolated_as_not_verified() {
        ScanReport report = new ScanEngine().scan(items(6), item -> {
            if (item.index() == 3) throw new IllegalStateException("boom");
            return ScanOutcome.scored(0, RiskLevel.LOW, List.of());
        }, ScanObserver.NO_OP);
        ScanOutcome outcome = report.results().get(3).outcome();
        assertEquals("RULE_EXCEPTION", ((ScanOutcome.NotVerified) outcome).code());
        assertEquals(1, report.summary().notVerified());
        assertEquals(5, report.summary().scored());
    }

    @Test
    void unavailable_check_is_mapped_to_not_verified() {
        ScanReport report = new ScanEngine().scan(items(3), item -> {
            throw new CheckUnavailableException("API_TIMEOUT", "timed out");
        }, ScanObserver.NO_OP);
        assertEquals("API_TIMEOUT",
                ((ScanOutcome.NotVerified) report.results().get(0).outcome()).code());
        assertEquals(3, report.summary().notVerified());
    }

    @Test
    void telemetry_reports_start_item_and_finish() {
        AtomicInteger started = new AtomicInteger(), itemsSeen = new AtomicInteger(), finished = new AtomicInteger();
        ScanEngine observerEngine = new ScanEngine();
        ScanReport report = observerEngine.scan(items(12),
                item -> ScanOutcome.scored(0, RiskLevel.LOW, List.of()),
                countingObserver(started, itemsSeen, finished));
        assertEquals(1, started.get());
        assertEquals(12, itemsSeen.get());
        assertEquals(1, finished.get());
        assertEquals(12, report.summary().total());
    }

    @Test
    void workers_really_run_concurrently() throws Exception {
        AtomicInteger active = new AtomicInteger(), maxActive = new AtomicInteger();
        ThreatEvaluationService slow = item -> {
            int current = active.incrementAndGet();
            maxActive.accumulateAndGet(current, Math::max);
            try {
                Thread.sleep(15);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                active.decrementAndGet();
            }
            return ScanOutcome.scored(0, RiskLevel.LOW, List.of());
        };
        ScanReport report = new ScanEngine().scan(items(16), slow, ScanObserver.NO_OP);
        assertTrue(maxActive.get() > 1, "expected concurrency, got " + maxActive.get());
        assertEquals(16, report.results().size());
    }
}
