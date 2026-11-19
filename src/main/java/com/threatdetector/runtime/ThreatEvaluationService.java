package com.threatdetector.runtime;

import com.threatdetector.model.ScanItem;
import com.threatdetector.model.ScanOutcome;

/** Backend port. The UI never implements or calls rules directly. */
@FunctionalInterface
public interface ThreatEvaluationService {
    ScanOutcome evaluate(ScanItem item) throws CheckUnavailableException;
}
