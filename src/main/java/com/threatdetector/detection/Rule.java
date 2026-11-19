package com.threatdetector.detection;

import java.util.Optional;

/** One local, deterministic detection rule. */
public interface Rule {
    Optional<RuleHit> evaluate(RuleInput input);
}
