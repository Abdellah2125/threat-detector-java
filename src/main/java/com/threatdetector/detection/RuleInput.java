package com.threatdetector.detection;

import java.net.URI;

/** The input given to every rule. Rules ignore the variants they do not handle. */
public sealed interface RuleInput permits RuleInput.Url, RuleInput.Message {

    record Url(String raw, URI uri) implements RuleInput {
    }

    record Message(String text) implements RuleInput {
    }
}
