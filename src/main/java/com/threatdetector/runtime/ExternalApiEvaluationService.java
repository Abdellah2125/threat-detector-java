package com.threatdetector.runtime;

import com.threatdetector.model.ScanItem;
import com.threatdetector.model.ScanOutcome;

/**
 * Option B adapter. No network call is made by this course project, but the
 * failure modes required by the brief are modelled explicitly: a missing key,
 * HTTP 429, a timeout or an invalid response all raise CheckUnavailableException
 * so the engine can report NOT VERIFIED instead of assuming safety.
 */
public final class ExternalApiEvaluationService implements ThreatEvaluationService {
    public enum Mode { AVAILABLE, MISSING_KEY, RATE_LIMITED, TIMEOUT, INVALID_RESPONSE }

    private final Mode mode;

    public ExternalApiEvaluationService() {
        this(Mode.MISSING_KEY);
    }

    public ExternalApiEvaluationService(Mode mode) {
        this.mode = mode;
    }

    @Override
    public ScanOutcome evaluate(ScanItem item) throws CheckUnavailableException {
        switch (mode) {
            case MISSING_KEY -> throw new CheckUnavailableException("API_KEY_MISSING",
                    "Threat intelligence API key is missing.");
            case RATE_LIMITED -> throw new CheckUnavailableException("API_RATE_LIMITED",
                    "Threat intelligence API returned HTTP 429.");
            case TIMEOUT -> throw new CheckUnavailableException("API_TIMEOUT",
                    "Threat intelligence API did not respond in time.");
            case INVALID_RESPONSE -> throw new CheckUnavailableException("API_INVALID_RESPONSE",
                    "Threat intelligence API returned an invalid response.");
            case AVAILABLE -> {
                // A real implementation would call the API here. Offline by design:
                // an item whose external check cannot be completed is never "safe".
                throw new CheckUnavailableException("CHECK_UNAVAILABLE",
                        "External check is unavailable in this offline build.");
            }
            default -> throw new IllegalStateException("Unknown mode");
        }
    }
}
