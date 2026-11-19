package com.threatdetector.runtime;

import com.threatdetector.model.ItemResult;
import com.threatdetector.model.ScanEvents;
import com.threatdetector.model.ScanItem;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.ScanReport;
import com.threatdetector.model.Summary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scoped worker pool: one atomic cursor hands each item to exactly one worker,
 * results keep intake order, and all threads join before the report returns.
 */
public final class ScanEngine {
    public static final int DEFAULT_WORKERS = 4;

    private final int maxWorkers;

    public ScanEngine() {
        this(DEFAULT_WORKERS);
    }

    public ScanEngine(int maxWorkers) {
        this.maxWorkers = Math.max(1, maxWorkers);
    }

    public ScanReport scan(List<ScanItem> items, ThreatEvaluationService evaluator, ScanObserver observer) {
        long started = System.nanoTime();
        int total = items.size();
        int workers = Math.min(maxWorkers, Math.max(1, total));
        observer.onStarted(new ScanEvents.ScanStartedEvent(0, total, workers));
        if (total == 0) {
            observer.onFinished(new ScanEvents.ScanFinishedEvent(0, new Summary(0, 0, 0, 0, 0, 0), 0, 0));
            return new ScanReport(0, List.of(), new Summary(0, 0, 0, 0, 0, 0), 0, 0);
        }

        AtomicInteger cursor = new AtomicInteger(0);
        AtomicInteger completed = new AtomicInteger(0);
        ItemResult[] results = new ItemResult[total];
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        List<Future<?>> futures = new ArrayList<>();

        for (int workerId = 0; workerId < workers; workerId++) {
            final int id = workerId;
            futures.add(pool.submit(() -> {
                while (true) {
                    int index = cursor.getAndIncrement();
                    if (index >= total) break;
                    ScanItem item = items.get(index);
                    long itemStart = System.nanoTime();
                    ScanOutcome outcome;
                    try {
                        outcome = evaluator.evaluate(item);
                    } catch (CheckUnavailableException exception) {
                        outcome = ScanOutcome.notVerified(exception.code(), exception.getMessage());
                    } catch (Throwable throwable) {
                        outcome = ScanOutcome.notVerified("RULE_EXCEPTION",
                                "Rule evaluation failed: " + throwable);
                    }
                    long durationMs = Math.max(1, (System.nanoTime() - itemStart) / 1_000_000);
                    ItemResult result = new ItemResult(index, item.rowNumber(), item.kind(), item.target(),
                            outcome, durationMs, id);
                    results[index] = result;
                    int done = completed.incrementAndGet();
                    observer.onItem(new ScanEvents.ScanItemEvent(0, index, done, total, result));
                    if (outcome instanceof ScanOutcome.NotVerified) observer.onError(result);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Worker failed", exception);
            }
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException exception) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        List<ItemResult> ordered = List.of(results);
        Summary summary = Summary.summarize(ordered.stream().map(ItemResult::outcome).toList());
        long durationMs = Math.max(1, (System.nanoTime() - started) / 1_000_000);
        ScanReport report = new ScanReport(0, ordered, summary, workers, durationMs);
        observer.onFinished(new ScanEvents.ScanFinishedEvent(0, summary, workers, durationMs));
        return report;
    }

    public static int resolveWorkerCount(int requested, int itemCount) {
        return Math.max(1, Math.min(Math.max(1, requested), Math.max(1, itemCount)));
    }
}
