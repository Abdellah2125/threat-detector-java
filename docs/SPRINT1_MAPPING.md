# Sprint 01 — Requirement → Implementation mapping

| # | Requirement (course brief) | Implementation | Status |
|---|---|---|---|
| 1 | GUI framework (e.g. Swing, JavaFX) | `ui/ThreatDetectorApp.java` — JavaFX 17 dashboard shell with top navigation, hero, workspace and four workspace modes | ✅ |
| 2 | Load single URLs/messages and browse local CSV files | `ui/CheckPanel.java`, `ui/BulkPanel.java` (FileChooser), `intake/CsvLoader.java` | ✅ |
| 3 | Modular backend: threat rules and file parsing in dedicated service classes | `detection/`, `intake/`, `runtime/` packages; `ThreatEvaluationService` port | ✅ |
| 4 | Display selected inputs and evaluation results, risk levels and status indicators | `TableView` in `BulkPanel` and `ResultsSection`, `VerdictCard`, `badge-high/medium/low/unverified` CSS classes | ✅ |
| 5 | Progress indicators for each evaluation | `runtime/ScanObserver` telemetry → `ui/ScanProgress` plus `ui/ThreadCoreView` 3D packet animation | ✅ |
| 6 | Responsive, user-friendly interface | JavaFX layout panes with min window size 1024×640, wrapping labels, resizable tables, top navigation, summary/history cards and a light dashboard palette (`#F4F7F5`, white cards, `#1E7D3C` accent) | ✅ |
| 7 | Input/error handling: empty input, malformed URL, invalid CSV rows, missing files, unavailable API responses — no crash | `EMPTY_INPUT`, `INVALID_URL`, `INPUT_TOO_LONG`, `INVALID_CSV_ROW`, `LoadException` banner, `CheckUnavailableException` → `NotVerified` | ✅ |
| 8 | Unreachable checks reported as **Not Verified**, never safe | `ScanOutcome.NotVerified` sealed variant; `RULE_EXCEPTION` isolation; UI label “Unverified items are never safe” | ✅ |
| 9 | Option A — local rule-based detection (HTTP vs HTTPS, IP hosts, misleading subdomains, urgency, credentials), cumulative score → Low/Medium/High | 10 URL rules + 7 message rules, `Scoring.finalize` (60/25 thresholds, 100 cap) | ✅ |
| 10 | Multi-threaded processing of security checks | `ScanEngine`: fixed pool of up to 4 threads, `AtomicInteger` cursor, indexed results, `awaitTermination` before return | ✅ |
| 11 | Bonus: AI integration, graceful failure, secondary only | `runtime/AiExplanationService.java` — optional explanation layer, never changes the score, fails to `Optional.empty()`. **Structure only:** it is deterministic, not a pre-trained model; a real model/service must still be plugged in. | ⚠️ partial |

## Tests

```text
52 tests, 0 failures
CsvLoaderTest      8
ScanEngineTest     8
RuleSetTest       24
ScoringTest        5
ServiceTest        7
```

Run with `mvn test`.

## Deliberate design decisions

- **Option A was chosen** instead of an online security API. Therefore there are no live HTTP calls, rate limits or API keys in the shipped build. To still demonstrate the required error handling, `ExternalApiEvaluationService` models those failure modes explicitly and the engine maps them to `NotVerified`.
- **Java was used throughout** (JavaFX GUI and Java rule classes) to match the course wording literally.
- **No data leaves the device**: the only file read is the CSV the user picks.
