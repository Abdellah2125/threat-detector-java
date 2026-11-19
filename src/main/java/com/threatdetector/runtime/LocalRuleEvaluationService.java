package com.threatdetector.runtime;

import com.threatdetector.model.ScanItem;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.detection.RuleSet;

/** Option A: fully local, deterministic rule-based evaluation. */
public final class LocalRuleEvaluationService implements ThreatEvaluationService {
    private final RuleSet ruleSet;

    public LocalRuleEvaluationService() {
        this(new RuleSet());
    }

    public LocalRuleEvaluationService(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    public RuleSet ruleSet() {
        return ruleSet;
    }

    @Override
    public ScanOutcome evaluate(ScanItem item) {
        return ruleSet.evaluate(item);
    }
}
