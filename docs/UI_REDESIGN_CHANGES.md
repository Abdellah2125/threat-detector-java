# UI Redesign Change Log (DAC reference adaptation)

This document explains and records **every UI/styling/structural presentation change** made to adapt the JavaFX interface of `threat-detector-java` to the attached DAC reference UI (`dac_threat_detector-main`, HTML/CSS/JS demo) while **preserving all existing project logic and behavior**.

- **Date of change set:** working-tree changes documented from current repository state.
- **Scope:** presentation layer only (JavaFX views, stylesheet, navigation structure, and in-memory history presentation). No detection, scoring, intake, parsing, threading, engine, observer, or model logic was modified.
- **Reference sources consulted for target look/structure:** `dac_threat_detector-main/index.html`, `history.html`, `hardware.html`, `settings.html`, `css/core.css`, `css/nav.css`, `css/pages/*.css`.

---

## 1. Verification (post-change)

Per the completed verification run for this change set:

| Check | Result |
|---|---|
| `mvn -DskipTests compile` | **BUILD SUCCESS** (exit 0) |
| `mvn test` | **Tests run: 52, Failures: 0, Errors: 0, Skipped: 0**, **BUILD SUCCESS** (exit 0) |

No test or production logic code outside the UI presentation layer was changed; all existing tests continue to pass.

---

## 2. Design/translation decisions (reference → JavaFX)

| Reference (web) | JavaFX adaptation | Notes |
|---|---|---|
| Dark/OLED tokens in `css/core.css` (`--bg #000000`, `--bg-card #0E120D`, `--accent #8DEB6B`, `--text`, risk colors, borders, focus ring, etc.) | Defined as JavaFX looked-up colors in `.root` of `app.css` (`-bg`, `-bg-card`, `-accent`, `-low/-medium/-high/-na`, `-focus-ring`, etc.) and applied across components | Achieves the same dark + green accent visual language within JavaFX/CSS capabilities. |
| Sticky nav/header (`.app-header`, `.navbar`, nav links) | Restyled `.app-header`, `.navbar`, `.brand`, `.navlink`, status chip/dot in `app.css`; navigation rebuilt in `TopNav` | Keeps JavaFX `TopNav`/shell structure, changes appearance and link set/order. |
| Pages: Dashboard / History / Hardware / Settings | Nav items reordered/relabeled to **Dashboard, History, Hardware, Settings** mapped to modes `INSPECT`, `HISTORY`, `CORE`, `METHOD` | `INSPECT` and `BATCH` remain the Dashboard scan sub-modes (single input vs CSV batch) driven by existing panels/toggles and `onModeChange`; Dashboard nav stays active for both. |
| Cards, inputs, tables, badges, progress, drop zones, result states | Restyled existing JavaFX selectors/classes (`.card`, `.panel`, `.input`, `.textarea`, `.table-view`, `.badge-*`, `.progress`, `.file-drop`, `.verdict`, `.summary-*`, etc.) | Preserves control types and bindings; only look-and-feel changes. |
| Risk palette (Low/Medium/High/N.A.) | Dark-theme palette aligned to reference (`-low #3DDC84`, `-medium #FFB020`, `-high #FF5A5F`, `-na #8A93A0`) in CSS and in code-drawn colors | `VerdictCard.toneColor`, `ScoreDial` canvas strokes/colors, summary/risk CSS classes updated consistently. |
| Web-only demo behaviors (theme/density/accent/settings toggles, detection/privacy toggles, simulated hardware metrics, client-side persistence/services) | **Not ported** | Omitted intentionally to avoid adding fake/non-functional UI or misrepresenting behavior; only visuals/structure backed by existing project logic were adapted. |

**Logic preserved:** `ScanCoordinator`, `ScanEngine`, `ScanObserver`/progress callbacks, `ThreatEvaluationService`/rules, intake (`CsvLoader`/file loading), threading/worker pool behavior, and domain models (`ScanReport`, `ItemResult`, `ScanOutcome`, `Finding`, etc.) are unchanged. UI changes only affect layout, styling, labels/presentation state, and how existing in-memory results are displayed.

---

## 3. File-by-file changes

### 3.1 New file

#### `src/main/java/com/threatdetector/ui/HistoryPanel.java` (new)
- **What:** Added a full session-history view/panel (reference "History" page) that displays **all scan results accumulated during the current run** in a `TableView` with summary tiles (TOTAL / HIGH / MEDIUM / LOW / NOT VERIFIED).
  - Renders per-result rows (input, kind, risk level, score, status, details, timestamp) using existing model types: `ScanReport`, `ItemResult`, `ScanOutcome.Scored`/`NotVerified`, `Finding`.
  - `add(ScanReport)` prepends each result from a completed scan (newest first) and refreshes counts; `clear()` resets state.
  - Includes an explanatory subtitle stating history is in-memory and resets on close.
- **Why:** To provide the dedicated **History** page present in the reference navigation, using data the application already produces.
- **Logic preservation:** Presentation-only over existing in-memory results; performs no detection, no persistence, and introduces no runtime/IO logic. It does not alter how scans execute or how reports are produced.

### 3.2 Modified files

#### `src/main/resources/app.css` (rewritten; ~1335 lines → ~696 lines)
- **What (major rewrite):**
  - Replaced the previous light dashboard stylesheet with a **dark, reference-inspired JavaFX theme** (file header notes mapping and links to this doc).
  - **Design tokens in `.root`:** looked-up colors mirroring reference `core.css` (dark/oled): backgrounds (`-bg #000000`, `-bg-nav #161616`, `-bg-card #0E120D`, `-bg-card-2 #141A11`, `-bg-input #121715`), borders/text (`-border`, `-border-strong`, `-text`, `-text-muted`, `-text-faint`), focus (`-focus-ring #7BD5FF`), accent/green (`-accent #8DEB6B`, `-accent-strong`, `-accent-soft`, `-glow`), and risk/soft variants (`-low/-medium/-high/-na` and corresponding `*-soft`).
  - **Base/control defaults:** unified dark text/fill defaults; transparent backgrounds for scroll panes/virtual flows/fillers/empty rows; dark scroll-bar thumb styling.
  - **Shared typography helpers:** `.micro`, `.mono/.cell-mono`, `.panel-hint/.card-subtitle/.field-hint/.muted` recolored/restyled for dark UI.
  - **Shell/layout:** `.app-shell`, `.app-header`, `.navbar`, `.brand*`, `.navlink` (+ hover/current/risk-levels variants with accent glow), `.nav-status*`, `.status-dot(.live)` restyled to match reference header/nav feel.
  - **Layout containers:** `.main-scroll/.main-content/.dashboard-layout/.main-column/.side-column/.dash-aside` on dark background.
  - **Cards/panels:** `.card/.panel/.results/.verdict`, titles/subtitles/heads/steps/bodies restyled (dark surfaces, borders, radii, shadow) keeping existing class names.
  - **Hero:** `.hero` restyled with dark surface and left accent border treatment (`-fx-border-color: -border -border -border -accent`), updated typography/colors.
  - **Badges/pills:** `.badge*` variants mapped to accent/risk/neutral/NA soft backgrounds and borders.
  - **Segmented controls:** `.seg*`, `.segmented`, `.segment` restyled (track, hover, selected/active states using accent fill and contrast text).
  - **Inputs:** `.field-*`, `.input/.textarea/.text-field/.text-area` (+ focus glow, prompt/highlight colors, transparent text-area internals), `.chips/.chip*`, `.scope*` restyled for dark forms.
  - **Banners & buttons:** `.banner-error/.banner-warning` use risk soft tones; `.button-primary` (accent fill), `.button-secondary`, `.button-ghost` (hover behaviors) restyled.
  - **Progress & file intake:** `.scan-*`, `.progress` track/bar, `.file-drop*`, `.file-loaded*`, `.file-name/.file-meta` restyled.
  - **Tables:** `.table-view`, headers, rows (normal/odd/selected), cells, placeholders restyled for dark theme.
  - **Results/report:** `.results*`, placeholders, filters/chips, summary grid/cards/values, distribution bar segments, verdict layout/typography, fact blocks, finding rows/evidence chips, callouts restyled.
  - **Right-column/summary/history:** `.summary-panel/.history-panel`, status, `.stat*`, `.history-table`, `.status-verified/.status-unverified` recolored.
  - **Method/Settings-like info:** `.method*`, `.scale-*`, `.tick*`, `.rules-table` restyled.
  - **Hardware/Thread Core:** `.thread-core-card`, head/phase badges (idle/running/complete/failed), `.thread-canvas` (dark surface), HUD/stats, `.progress-console`, `.worker-strip/.worker-chip(.active)` and dot, `.thread-foot` restyled.
  - **Footer:** `.foot*` restyled.
  - **Focus visibility:** added explicit focused-state rules for buttons/toggles/text inputs/table using `-focus-ring`.
- **Why:** To implement the reference design system (dark surfaces, green accent, risk palette, component treatments) across all existing UI components.
- **Logic preservation:** No selectors/classes were renamed in a way that breaks existing node wiring except where components already used these classes; controls, tables, and handlers remain the same objects/behavior—only visual properties changed.

#### `src/main/java/com/threatdetector/ui/ThreatDetectorApp.java`
- **What:**
  - Added field/instantiation for the new `HistoryPanel historyPanel` (`ui/ThreatDetectorApp.java`).
  - On scan completion (`onScanComplete`), forwards the produced `ScanReport` to `historyPanel.add(report)` in addition to existing `summaryCard.show(report)` and `historyCard.add(report)`.
  - In `switchMode(Mode)`: added handling for `Mode.HISTORY` → `showFullWidth(historyPanel.getNode())`; annotated `INSPECT`/`BATCH` as Dashboard scan sub-modes (single input vs CSV batch).
  - In `resetUi()`: added `historyPanel.clear()` alongside existing clears/resets.
- **Why:** To wire the new History view into the app shell and keep its session data synchronized with completed scans/resets, matching reference navigation without altering scan execution.
- **Logic preservation:** Scan execution path (`ScanCoordinator`/progress/result handling) and existing result consumers (`results`, `summaryCard`, `historyCard`, core panel phase/progress updates) are unchanged; `historyPanel` is an additional presentation sink only.

#### `src/main/java/com/threatdetector/ui/TopNav.java`
- **What:**
  - Updated class doc comment to reflect dark/reference styling.
  - Increased nav-links spacing (`HBox(8)`).
  - Reordered/redefined navigation buttons to mirror reference: **Dashboard → `Mode.INSPECT`**, **History → `Mode.HISTORY`**, **Hardware → `Mode.CORE`**, **Settings → `Mode.METHOD`** (replacing previous mapping where `BATCH` showed as "History" and order differed).
  - Updated risk-levels button accessible text to "Open risk level and method information".
  - `setActive(Mode)`: treats `INSPECT` and `BATCH` as the same active Dashboard context (`key == Mode.INSPECT && mode == Mode.BATCH`) so Dashboard remains highlighted for both scan sub-modes; keeps `METHOD` highlighting `riskButton` as before.
- **Why:** Align navigation structure/labels/active states with the reference pages while preserving access to existing scan sub-modes.
- **Logic preservation:** Still invokes the provided `onModeChange` callback with existing `Mode` values; no change to detection/scan logic. Visibility of CSV batch remains controlled by existing panel behavior (Dashboard scan sub-mode toggle/`onModeChange`).

#### `src/main/java/com/threatdetector/ui/Mode.java`
- **What:**
  - Added enum constant `HISTORY("Session History")`.
  - Adjusted comments/descriptions to clarify roles: `INSPECT` = "Single-input scan (Dashboard)", `BATCH` = "CSV batch scan (Dashboard sub-mode)", `METHOD` = "Method and rules (Settings)".
- **Why:** Support the dedicated History view/mode and document mode intent aligned with reference navigation.
- **Logic preservation:** Existing modes and their identities remain; addition is additive and used only for UI routing/presentation.

#### `src/main/java/com/threatdetector/ui/BrandMark.java`
- **What (visual-only):**
  - Shield fill color changed `#10161C` → `#0E120D` (dark card tone).
  - Shield/accent stroke and core dot changed `#1E7D3C` → `#8DEB6B` (reference green accent).
  - Outline stroke opacity slightly increased (`0.22` → `0.30`).
  - Kept same geometry, sizes, and component structure.
- **Why:** Match the reference dark/green brand mark treatment.
- **Logic preservation:** Pure drawing/presentation; no behavior.

#### `src/main/java/com/threatdetector/ui/ChamferPane.java`
- **What (visual-only):**
  - Default border stroke changed `#5D6873` → `#6A7269`.
  - Default fill changed `#FFFFFF` → `#0E120D`.
  - Kept same pane shape, stroke widths, and API.
- **Why:** Align card/chamfer primitive with dark theme surfaces and stronger borders.
- **Logic preservation:** No layout/behavior logic changed.

#### `src/main/java/com/threatdetector/ui/Hero.java`
- **What:**
  - Updated hero mark construction: `BrandMark` instantiated at `36, 36` (larger) instead of default size.
  - Simplified mark container styling: removed white/translucent background, border, and fixed 58px box; now transparent container with centered alignment only.
  - Replaced previous paragraph copy with: "Run rule-based checks on URLs and messages, review risk levels and findings, and inspect worker activity — all locally, with results you can trace."
  - Kept hero layout structure, title, and action button wiring (`onCheck`, `Mode.INSPECT`).
- **Why:** Better fit the reference hero presentation on dark background and provide concise, accurate copy for current capabilities.
- **Logic preservation:** "Start a check" action and callback remain unchanged.

#### `src/main/java/com/threatdetector/ui/MethodPanel.java`
- **What:**
  - Panel header/subtitle relabeled from `"METHOD"` / `"Checking Method"` to `"SETTINGS"` / `"Method & Rules"`; hint changed to `"Project reference"`.
  - Kept all informational content intact: explanatory body, verdict scale rows (High/Medium/Low 60–100/25–59/0–24), concurrency contract bullets, and rule table (factual to project).
- **Why:** Align this view with the reference **Settings** page label while continuing to present the project's actual method/rules (no fake settings controls added).
- **Logic preservation:** No behavioral logic; content remains descriptive of existing system behavior.

#### `src/main/java/com/threatdetector/ui/RecentHistoryCard.java`
- **What (presentation-only):**
  - Table "Time" column now displays `t.time()` only (removed appended `" Today"` suffix).
- **Why:** Avoid implying day context that was not derived from real data; keeps display accurate for session entries.
- **Logic preservation:** In-memory history storage, add/clear behavior, and columns/data sources unchanged.

#### `src/main/java/com/threatdetector/ui/ScoreDial.java`
- **What (visual-only, canvas drawing):**
  - Track stroke color `#C9D3CC` → `#2A3328`.
  - Arc/risk colors updated to dark palette: high `#C2263B` → `#FF5A5F`, medium `#B45309` → `#FFB020`, low `#127A48` → `#3DDC84`, NA `#55606A` → `#8A93A0`.
  - Inner text/labels colors updated: score text `#10161C` → `#F2F5F7`, "SCORE" label `#66727B` → `#A6B1BB`.
  - Kept geometry, animation/tween logic, and `set(...)` API/behavior.
- **Why:** Match reference-aligned risk/accent colors on dark surfaces.
- **Logic preservation:** Rendering/animation logic unchanged.

#### `src/main/java/com/threatdetector/ui/SummaryCard.java`
- **What (presentation-only):**
  - Stat label changed from `"CHECKS"` to `"SCANS"` (clarifies session scan-count metric).
  - On `show(...)`: dynamically styles the status title text color to match risk tone (`-high/-medium/-low/-na` / green `#3DDC84` for safe) and sets copy to `"Last scan result (this session)."`.
  - On `clear()`: resets status title text color to muted (`#A6B1BB`) in addition to existing resets.
- **Why:** Improve clarity and align status emphasis with dark-theme risk palette.
- **Logic preservation:** Uses the same `ScanReport`/summary data already passed in; no calculation or scan behavior changed.

#### `src/main/java/com/threatdetector/ui/ThreadCorePanel.java`
- **What (label/copy only):**
  - Panel title changed `"THREAD CORE · LIVE CONCURRENCY"` → `"HARDWARE · WORKER POOL"`.
  - Header caption changed `"Live concurrency"` → `"Live worker activity"`.
  - Kept all real worker-pool visualization, progress, stats, and observer wiring intact.
- **Why:** Align naming with reference "Hardware" page while accurately describing the existing worker-pool visualization (not simulated hardware metrics).
- **Logic preservation:** `ThreadCoreView`, `ScanObserver` updates, and all runtime presentation logic unchanged.

#### `src/main/java/com/threatdetector/ui/VerdictCard.java`
- **What (visual-only):**
  - `toneColor(String)` updated to dark-theme risk palette: high `#C2263B` → `#FF5A5F`, medium `#B45309` → `#FFB020`, low `#127A48` → `#3DDC84`, default/NA `#55606A` → `#8A93A0`.
  - Added comment noting alignment with reference dark tokens.
  - Kept verdict composition, score dial binding, findings/evidence rendering, and fact mapping logic unchanged.
- **Why:** Consistent risk coloring with the redesigned theme and canvas dial.
- **Logic preservation:** No data/decision logic changed.

---

## 4. Changes intentionally **not** made (to preserve logic/accuracy)

- No modifications to detection rules, scoring, intake/parsing, scan coordination/engine, threading model, observers, or domain models.
- No fake or non-functional controls ported from the reference demo (e.g. theme/accent/density toggles, detection/privacy settings, simulated system metrics, downloadable/report-export flows not backed by logic).
- No persistence layer added; scan history remains **session/in-memory only** (as before), now surfaced in the new History view.
- No changes to build configuration, tests, or non-UI runtime behavior.

---

## 5. Summary of affected files

| Status | File |
|---|---|
| Added | `src/main/java/com/threatdetector/ui/HistoryPanel.java` |
| Modified | `src/main/resources/app.css` |
| Modified | `src/main/java/com/threatdetector/ui/BrandMark.java` |
| Modified | `src/main/java/com/threatdetector/ui/ChamferPane.java` |
| Modified | `src/main/java/com/threatdetector/ui/Hero.java` |
| Modified | `src/main/java/com/threatdetector/ui/MethodPanel.java` |
| Modified | `src/main/java/com/threatdetector/ui/Mode.java` |
| Modified | `src/main/java/com/threatdetector/ui/RecentHistoryCard.java` |
| Modified | `src/main/java/com/threatdetector/ui/ScoreDial.java` |
| Modified | `src/main/java/com/threatdetector/ui/SummaryCard.java` |
| Modified | `src/main/java/com/threatdetector/ui/ThreadCorePanel.java` |
| Modified | `src/main/java/com/threatdetector/ui/ThreatDetectorApp.java` |
| Modified | `src/main/java/com/threatdetector/ui/TopNav.java` |
| Modified | `src/main/java/com/threatdetector/ui/VerdictCard.java` |
| Added (doc) | `docs/UI_REDESIGN_CHANGES.md` |

All changes above are confined to UI presentation, styling, and view wiring; project logic and verified behavior remain intact.
