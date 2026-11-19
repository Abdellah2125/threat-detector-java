package com.threatdetector.detection;

import com.threatdetector.model.Finding;
import com.threatdetector.model.ScanItem;
import com.threatdetector.model.ScanOutcome;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/** Composes all local rules and enforces input limits. */
public final class RuleSet {
    public static final int MAX_URL_CHARS = 2048;
    public static final int MAX_MESSAGE_CHARS = 5000;

    private final List<Rule> rules;

    public RuleSet() {
        List<Rule> all = new ArrayList<>();
        all.addAll(UrlRules.create());
        all.addAll(MessageRules.create());
        this.rules = List.copyOf(all);
    }

    public List<Rule> rules() {
        return rules;
    }

    public ScanOutcome evaluate(ScanItem item) {
        String raw = item.target() == null ? "" : item.target().trim();
        if (raw.isEmpty()) {
            return ScanOutcome.notVerified("EMPTY_INPUT", "Input is empty.");
        }

        RuleInput input;
        switch (item.kind()) {
            case URL -> {
                if (raw.chars().count() > MAX_URL_CHARS) {
                    return ScanOutcome.notVerified("INPUT_TOO_LONG", "URL is too long.");
                }
                try {
                    input = new RuleInput.Url(raw, parseUrl(raw));
                } catch (URISyntaxException | IllegalArgumentException exception) {
                    return ScanOutcome.notVerified("INVALID_URL", "Malformed or unsupported URL.");
                }
            }
            case MESSAGE -> {
                if (raw.chars().count() > MAX_MESSAGE_CHARS) {
                    return ScanOutcome.notVerified("INPUT_TOO_LONG", "Message is too long.");
                }
                input = new RuleInput.Message(raw);
            }
            default -> throw new IllegalStateException("Unknown target kind");
        }

        int score = 0;
        List<Finding> findings = new ArrayList<>();
        for (Rule rule : rules) {
            rule.evaluate(input).ifPresent(hit -> {
                findings.add(new Finding(hit.ruleId(), hit.title(), hit.points(), hit.evidence()));
            });
            // Recompute the score below to keep this loop side-effect free per rule.
        }
        for (Finding finding : findings) {
            score += finding.points();
        }
        return Scoring.finalize(score, findings);
    }

    private static URI parseUrl(String raw) throws URISyntaxException {
        String normalized = raw.contains("://") ? raw : "https://" + raw;
        URI uri = new URI(normalized);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new IllegalArgumentException("missing host");
        }
        return uri;
    }
}
