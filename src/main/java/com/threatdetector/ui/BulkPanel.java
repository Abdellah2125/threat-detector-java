package com.threatdetector.ui;

import com.threatdetector.intake.LoadException;
import com.threatdetector.runtime.ScanObserver;
import com.threatdetector.model.CsvLoadReport;
import com.threatdetector.model.ItemResult;
import com.threatdetector.model.LoadedRow;
import com.threatdetector.model.ScanEvents;
import com.threatdetector.model.ScanReport;
import com.threatdetector.runtime.ScanCoordinator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.function.Consumer;

/** Workspace 02: load a CSV, preview validated rows, scan them concurrently. */
public final class BulkPanel {
    private static final int PREVIEW_ROWS = 6;

    private final ScanCoordinator coordinator;
    private final ScanUiBridge bridge;
    private final Consumer<Mode> onModeChange;
    private final Panel panel = new Panel("SCAN", "CSV Batch", "02", "Batch input");
    private final VBox fileArea = new VBox(12);
    private final VBox warnings = new VBox(6);
    private final Label previewCount = Ui.mono("NO FILE");
    private final TableView<LoadedRow> preview = new TableView<>();
    private final ScanProgress progress = new ScanProgress();
    private final Label error = Ui.label("", "banner banner-error");
    private final Button run = Ui.button("Analyze", "button-primary");
    private CsvLoadReport loaded;

    public BulkPanel(ScanCoordinator coordinator, ScanUiBridge bridge, Consumer<Mode> onModeChange) {
        this.coordinator = coordinator;
        this.bridge = bridge;
        this.onModeChange = onModeChange;

        ToggleGroup modeGroup = new ToggleGroup();
        ToggleButton singleToggle = new ToggleButton("Single Input");
        ToggleButton batchToggle = new ToggleButton("CSV Batch");
        singleToggle.setToggleGroup(modeGroup);
        batchToggle.setToggleGroup(modeGroup);
        batchToggle.setSelected(true);
        singleToggle.getStyleClass().add("seg__opt");
        batchToggle.getStyleClass().add("seg__opt");
        singleToggle.setOnAction(event -> onModeChange.accept(Mode.INSPECT));
        batchToggle.setOnAction(event -> onModeChange.accept(Mode.BATCH));
        HBox modeSwitch = new HBox(2, singleToggle, batchToggle);
        modeSwitch.getStyleClass().add("seg");

        buildPreview();
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        run.setDisable(true);

        Label previewTitle = Ui.micro("Preview");
        HBox previewHead = new HBox(10, previewTitle, Ui.spacer(), previewCount);
        previewHead.setAlignment(Pos.CENTER_LEFT);

        Button clear = Ui.button("Clear", "button-secondary");
        HBox foot = new HBox(12,
                Ui.label("Invalid rows stay Not verified — never reported as safe.", "panel-hint"),
                Ui.spacer(), run, clear);
        foot.setAlignment(Pos.CENTER_LEFT);

        panel.body().getChildren().addAll(
                modeSwitch,
                fileArea,
                warnings,
                new VBox(8, previewHead, preview),
                error,
                progress,
                foot);

        rebuildFileArea();
        run.setOnAction(event -> startScan());
        clear.setOnAction(event -> reset());
    }

    private void rebuildFileArea() {
        fileArea.getChildren().clear();
        if (loaded == null) {
            VBox drop = new VBox(14);
            drop.getStyleClass().add("file-drop");
            Label icon = Ui.label("▤", "file-drop-icon");
            VBox copy = new VBox(4,
                    Ui.label("Drag & drop a CSV file here", "file-drop-title"),
                    Ui.label("or click to browse · columns: type (url or message) and target · up to 5,000 rows / 5 MB",
                            "file-drop-hint"));
            HBox.setHgrow(copy, Priority.ALWAYS);
            Button choose = Ui.button("Choose file", "button-secondary");
            drop.getChildren().addAll(new HBox(14, icon, copy), choose);
            choose.setOnAction(event -> chooseFile());
            fileArea.getChildren().add(drop);
        } else {
            HBox row = new HBox(14);
            row.getStyleClass().add("file-loaded");
            row.setAlignment(Pos.CENTER_LEFT);
            VBox copy = new VBox(4,
                    Ui.label(loaded.fileName(), "file-name"),
                    Ui.label(comma(loaded.validCount()) + " ready · "
                            + comma(loaded.invalidCount()) + " not verified", "file-meta"));
            HBox.setHgrow(copy, Priority.ALWAYS);
            Button replace = Ui.button("Replace", "button-ghost");
            Button clear = Ui.button("✕", "button-ghost");
            clear.setAccessibleText("Remove the loaded CSV");
            replace.setOnAction(event -> chooseFile());
            clear.setOnAction(event -> reset());
            row.getChildren().addAll(Ui.label("▤", "file-drop-icon"), copy, replace, clear);
            fileArea.getChildren().add(row);
        }
    }

    private void buildPreview() {
        TableColumn<LoadedRow, Integer> line = new TableColumn<>("Row");
        line.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().rowNumber()));
        TableColumn<LoadedRow, String> kind = new TableColumn<>("Kind");
        kind.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().kind() == null ? "—" : cell.getValue().kind().name()));
        TableColumn<LoadedRow, String> target = new TableColumn<>("Target");
        target.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().target()));
        target.setPrefWidth(360);
        TableColumn<LoadedRow, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().outcome() == null ? "Ready" : "Not verified"));
        preview.getColumns().addAll(line, kind, target, status);
        preview.setPrefHeight(190);
        preview.setPlaceholder(Ui.micro("No rows loaded yet."));
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a CSV batch");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(getNode().getScene().getWindow());
        if (file == null) return;
        try {
            loaded = coordinator.loadCsv(file.toPath());
            preview.getItems().setAll(loaded.rows().subList(0, Math.min(PREVIEW_ROWS, loaded.rows().size())));
            warnings.getChildren().clear();
            for (String warning : loaded.warnings()) {
                Label label = Ui.label("⚠  " + warning, "banner banner-warning");
                label.setWrapText(true);
                warnings.getChildren().add(label);
            }
            hideError();
            run.setDisable(loaded.rows().isEmpty());
            progress.reset();
            bridge.onReset();
            rebuildFileArea();
            refreshPreviewHead();
        } catch (LoadException exception) {
            loaded = null;
            run.setDisable(true);
            preview.getItems().clear();
            warnings.getChildren().clear();
            rebuildFileArea();
            showError(exception.getMessage());
        }
    }

    private void refreshPreviewHead() {
        previewCount.setText(loaded == null ? "NO FILE"
                : "first " + Math.min(PREVIEW_ROWS, loaded.rows().size())
                + " of " + comma(loaded.rows().size()) + " rows");
    }

    private void startScan() {
        if (loaded == null || loaded.rows().isEmpty()) {
            showError("This file has no rows that can be evaluated.");
            return;
        }
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
                ScanReport report = coordinator.scanBatch(loaded, observer);
                Platform.runLater(() -> {
                    progress.finish(report);
                    bridge.onFinished(report, false, "");
                    run.setDisable(false);
                });
            } catch (RuntimeException exception) {
                Platform.runLater(() -> {
                    showError(exception.getMessage() == null ? "Batch check failed." : exception.getMessage());
                    bridge.onFailed("Batch failed");
                    run.setDisable(false);
                });
            }
        }, "batch-scan");
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
        loaded = null;
        preview.getItems().clear();
        warnings.getChildren().clear();
        progress.reset();
        hideError();
        run.setDisable(true);
        rebuildFileArea();
        refreshPreviewHead();
    }
}
