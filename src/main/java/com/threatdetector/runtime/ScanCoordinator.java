package com.threatdetector.runtime;

import com.threatdetector.intake.CsvLoader;
import com.threatdetector.intake.LoadException;
import com.threatdetector.model.CsvLoadReport;
import com.threatdetector.model.LoadedRow;
import com.threatdetector.model.ScanItem;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.ScanReport;
import com.threatdetector.model.TargetKind;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Application service: coordinates file loading and scanning for the UI. */
public final class ScanCoordinator {
    private final CsvLoader csvLoader;
    private final ScanEngine engine;
    private final ThreatEvaluationService evaluator;
    private final AiExplanationService ai;
    private final AtomicInteger scanIds = new AtomicInteger(1);

    public ScanCoordinator() {
        this(new CsvLoader(), new ScanEngine(), new LocalRuleEvaluationService(), new AiExplanationService(false));
    }

    public ScanCoordinator(CsvLoader csvLoader, ScanEngine engine,
                           ThreatEvaluationService evaluator, AiExplanationService ai) {
        this.csvLoader = csvLoader;
        this.engine = engine;
        this.evaluator = evaluator;
        this.ai = ai;
    }

    public CsvLoadReport loadCsv(Path path) throws LoadException {
        return csvLoader.load(path);
    }

    public AiExplanationService ai() {
        return ai;
    }

    public ScanReport scanSingle(TargetKind kind, String target, ScanObserver observer) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Input is empty.");
        }
        List<ScanItem> items = List.of(new ScanItem(0, null, kind, target));
        return engine.scan(items, evaluator, observer);
    }

    public ScanReport scanBatch(CsvLoadReport report, ScanObserver observer) {
        Map<Integer, ScanOutcome> preverified = new ConcurrentHashMap<>();
        List<ScanItem> items = new ArrayList<>();
        for (LoadedRow row : report.rows()) {
            int index = items.size();
            TargetKind kind = row.kind() == null ? TargetKind.MESSAGE : row.kind();
            items.add(new ScanItem(index, row.rowNumber(), kind,
                    row.target() == null ? "" : row.target()));
            if (row.outcome() != null) preverified.put(index, row.outcome());
        }
        if (items.isEmpty()) throw new IllegalArgumentException("No items to scan.");

        ThreatEvaluationService wrapped = item -> {
            ScanOutcome fixed = preverified.get(item.index());
            return fixed != null ? fixed : evaluator.evaluate(item);
        };
        return engine.scan(items, wrapped, observer);
    }
}
