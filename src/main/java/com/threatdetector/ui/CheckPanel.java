package com.threatdetector.ui;

import com.threatdetector.runtime.ScanObserver;
import com.threatdetector.model.ScanEvents;
import com.threatdetector.model.ScanReport;
import com.threatdetector.model.TargetKind;
import com.threatdetector.detection.RuleCatalog;
import com.threatdetector.runtime.ScanCoordinator;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** Workspace 01: one URL or message, checked by the local rule engine. */
public final class CheckPanel {
    private record Sample(String label, String value) {
    }

    private static final List<Sample> URL_SAMPLES = List.of(
            new Sample("Phishing URL", "http://192.168.10.24:8080/paypal/login-verify"),
            new Sample("Nested domain", "https://accounts.google.com.verify-login.tk/session"),
            new Sample("Clean URL", "https://github.com/tauri-apps/tauri"));
    private static final List<Sample> MESSAGE_SAMPLES = List.of(
            new Sample("Credential request", "URGENT: your bank account is locked. Verify your password within 24 hours or lose access: http://bit.ly/x9f2"),
            new Sample("Prize bait", "Congratulations! You have been selected for a free wallet prize. Claim now at http://tinyurl.com/claim"),
            new Sample("Clean message", "The library closes at 18:00 today."));

    private static final int MAX_URL = 2048;
    private static final int MAX_MESSAGE = 4000;

    private final ScanCoordinator coordinator;
    private final ScanUiBridge bridge;
    private final Consumer<Mode> onModeChange;
    private final Panel panel = new Panel("SCAN", "Start a Scan", "01", "Single input");

    private final ToggleGroup kindGroup = new ToggleGroup();
    private final ToggleButton urlToggle = new ToggleButton("URL");
    private final ToggleButton messageToggle = new ToggleButton("Message");
    private final TextField urlField = new TextField();
    private final TextArea messageField = new TextArea();
    private final StackPane fields = new StackPane();
    private final Label fieldLabel = Ui.label("URL to inspect", "field-label");
    private final Label fieldHint = Ui.label("Tip: full URLs work best (e.g. https://example.com)", "field-hint");
    private final Label counter = Ui.label("0 / 2,048", "field-count");
    private final HBox examples = new HBox(8);
    private final VBox scope = new VBox(8);
    private final Label error = Ui.label("", "banner banner-error");
    private final ScanProgress progress = new ScanProgress();
    private final Button run = Ui.button("Analyze", "button-primary");

    public CheckPanel(ScanCoordinator coordinator, ScanUiBridge bridge, Consumer<Mode> onModeChange) {
        this.coordinator = coordinator;
        this.bridge = bridge;
        this.onModeChange = onModeChange;

        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton singleToggle = new ToggleButton("Single Input");
        ToggleButton batchToggle = new ToggleButton("CSV Batch");
        singleToggle.setToggleGroup(modeGroup);
        batchToggle.setToggleGroup(modeGroup);
        singleToggle.setSelected(true);
        singleToggle.getStyleClass().add("seg__opt");
        batchToggle.getStyleClass().add("seg__opt");
        singleToggle.setOnAction(event -> onModeChange.accept(Mode.INSPECT));
        batchToggle.setOnAction(event -> onModeChange.accept(Mode.BATCH));
        HBox modeSwitch = new HBox(2, singleToggle, batchToggle);
        modeSwitch.getStyleClass().add("seg");

        urlToggle.setToggleGroup(kindGroup);
        messageToggle.setToggleGroup(kindGroup);
        urlToggle.getStyleClass().add("segment");
        messageToggle.getStyleClass().add("segment");
        urlToggle.setSelected(true);
        HBox.setHgrow(urlToggle, Priority.ALWAYS);
        HBox.setHgrow(messageToggle, Priority.ALWAYS);

        HBox segmented = new HBox(3, urlToggle, messageToggle);
        segmented.getStyleClass().add("segmented");
        segmented.setMaxWidth(Double.MAX_VALUE);

        urlField.getStyleClass().add("input");
        urlField.setPromptText("https://example.com/login");
        messageField.getStyleClass().add("textarea");
        messageField.setPromptText("Paste the message exactly as received…");
        messageField.setPrefRowCount(4);
        messageField.setWrapText(true);
        fields.getChildren().addAll(urlField, messageField);

        HBox fieldFoot = new HBox(10,
                Ui.micro("Whitespace is trimmed before evaluation."), Ui.spacer(), counter);
        fieldFoot.setAlignment(Pos.CENTER_LEFT);

        HBox examplesRow = new HBox(10, Ui.micro("Try"), examples);
        examplesRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(examples, Priority.SOMETIMES);

        Button clear = Ui.button("Clear", "button-secondary");
        HBox foot = new HBox(12,
                Ui.label("Demo mode · fictional heuristic checks, no live analysis.", "panel-hint"),
                Ui.spacer(), run, clear);
        foot.setAlignment(Pos.CENTER_LEFT);

        error.setVisible(false);
        error.setManaged(false);
        error.setWrapText(true);

        panel.body().getChildren().addAll(
                modeSwitch,
                segmented,
                new VBox(4, fieldLabel, fieldHint, fields, fieldFoot),
                examplesRow,
                scope,
                error,
                progress,
                foot);

        urlToggle.setOnAction(event -> updateKind());
        messageToggle.setOnAction(event -> updateKind());
        urlField.textProperty().addListener((obs, old, value) -> updateCounter());
        messageField.textProperty().addListener((obs, old, value) -> updateCounter());
        urlField.setOnAction(event -> startScan());
        messageField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                event.consume();
                startScan();
            }
        });
        run.setOnAction(event -> startScan());
        clear.setOnAction(event -> reset());

        updateKind();
    }

    private void updateKind() {
        boolean url = urlToggle.isSelected();
        urlToggle.getStyleClass().remove("active");
        messageToggle.getStyleClass().remove("active");
        (url ? urlToggle : messageToggle).getStyleClass().add("active");

        urlField.setVisible(url);
        urlField.setManaged(url);
        messageField.setVisible(!url);
        messageField.setManaged(!url);
        fieldLabel.setText(url ? "URL or message" : "Message to inspect");
        fieldHint.setText(url
                ? "Tip: full URLs work best (e.g. https://example.com)"
                : "Tip: paste the message exactly as received");
        updateCounter();
        rebuildExamples(url);
        rebuildScope(url);
    }

    private void updateCounter() {
        int limit = urlToggle.isSelected() ? MAX_URL : MAX_MESSAGE;
        int used = urlToggle.isSelected() ? urlField.getText().length() : messageField.getText().length();
        counter.setText(comma(used) + " / " + comma(limit));
    }

    private void rebuildExamples(boolean url) {
        examples.getChildren().clear();
        for (Sample sample : url ? URL_SAMPLES : MESSAGE_SAMPLES) {
            Button chip = Ui.button(sample.label(), "chip chip-button");
            chip.setOnAction(event -> {
                if (url) urlField.setText(sample.value());
                else messageField.setText(sample.value());
                hideError();
            });
            examples.getChildren().add(chip);
        }
    }

    private void rebuildScope(boolean url) {
        List<RuleCatalog.RuleSpec> rules = url ? RuleCatalog.URL_RULES : RuleCatalog.MESSAGE_RULES;
        scope.getChildren().clear();
        HBox head = new HBox(10,
                Ui.micro("Coverage · " + (url ? "URL" : "Message")),
                Ui.spacer(),
                Ui.micro(rules.size() + " rules · 0–100"));
        head.setAlignment(Pos.CENTER_LEFT);
        Label copy = Ui.label(url
                        ? "Scheme, host, port, domain shape, path wording, and bait terms are scored in order."
                        : "Urgency, credential requests, financial bait, pressure, links, and callbacks are scored in order.",
                "scope-copy");
        copy.setWrapText(true);
        HBox chips = new HBox(6);
        for (int i = 0; i < Math.min(4, rules.size()); i++) {
            chips.getChildren().add(Ui.mono(rules.get(i).id(), "scope-chip"));
        }
        if (rules.size() > 4) chips.getChildren().add(Ui.mono("+" + (rules.size() - 4), "scope-more"));
        scope.getChildren().addAll(head, copy, chips);
    }

    private void startScan() {
        boolean url = urlToggle.isSelected();
        String target = url ? urlField.getText() : messageField.getText();
        int limit = url ? MAX_URL : MAX_MESSAGE;
        if (target == null || target.isBlank()) {
            showError("Enter a URL or message before starting the check.");
            return;
        }
        if (target.length() > limit) {
            showError("This signal is too long. The limit is " + comma(limit) + " characters.");
            return;
        }

        TargetKind kind = url ? TargetKind.URL : TargetKind.MESSAGE;
        run.setDisable(true);
        hideError();
        bridge.onReset();
        progress.reset();

        ScanObserver observer = new ScanObserver() {
            @Override
            public void onStarted(ScanEvents.ScanStartedEvent event) {
                Platform.runLater(() -> {
                    progress.start(event.total(), event.workers());
                    bridge.onStarted(event.total(), event.workers());
                });
            }

            @Override
            public void onItem(ScanEvents.ScanItemEvent event) {
                Platform.runLater(() -> {
                    progress.item(event.completed(), event.total(), event.result());
                    bridge.onItem(event.result(), event.completed(), event.total());
                });
            }
        };

        Thread worker = new Thread(() -> {
            try {
                ScanReport report = coordinator.scanSingle(kind, target, observer);
                String explanation = coordinator.ai().explain(report.results().get(0).outcome()).orElse("");
                Platform.runLater(() -> {
                    progress.finish(report);
                    bridge.onFinished(report, true, explanation);
                    run.setDisable(false);
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showError(exception.getMessage() == null ? "Check failed." : exception.getMessage());
                    bridge.onFailed("Check failed");
                    run.setDisable(false);
                });
            }
        }, "check-scan");
        worker.setDaemon(true);
        worker.start();
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
    }

    private void hideError() {
        error.setText("");
        error.setVisible(false);
        error.setManaged(false);
    }

    private static String comma(int value) {
        return String.format("%,d", value);
    }

    public Region getNode() {
        return panel;
    }

    public void reset() {
        urlField.clear();
        messageField.clear();
        urlToggle.setSelected(true);
        updateKind();
        hideError();
        progress.reset();
        run.setDisable(false);
    }
}
