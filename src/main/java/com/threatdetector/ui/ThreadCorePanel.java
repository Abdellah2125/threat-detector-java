package com.threatdetector.ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** The live operations floor card: isometric 3D view, telemetry and worker pool. */
public final class ThreadCorePanel {
    private final ThreadCoreView core = new ThreadCoreView();
    private final VBox root;
    private final Label phaseBadge = Ui.badge("READY", "neutral");
    private final Label title = Ui.label("Your worker pool", "thread-title");
    private final Label subcopy = Ui.label(phaseText("idle"), "thread-sub");
    private final Label statusValue = Ui.label("Ready", "core-stat-value");
    private final Label workersValue = Ui.label("4", "core-stat-value");
    private final Label checkedValue = Ui.label("0 / 0", "core-stat-value");
    private final Label timeValue = Ui.label("0.0s", "core-stat-value");
    private final ProgressBar progress = new ProgressBar(0);
    private final List<Label> workerChips = new ArrayList<>();

    public ThreadCorePanel() {
        StackPane canvasHost = new StackPane(core);
        canvasHost.getStyleClass().add("thread-canvas");
        StackPane.setAlignment(core, Pos.CENTER);

        Label hudLeft = Ui.mono("INGRESS → 4 WORKERS → ARCHIVE");
        hudLeft.getStyleClass().addAll("thread-hud", "thread-hud-left");
        StackPane.setAlignment(hudLeft, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hudLeft, new javafx.geometry.Insets(0, 0, 8, 12));

        Label hudRight = Ui.mono("OPERATIONS FLOOR");
        hudRight.getStyleClass().addAll("thread-hud", "thread-hud-right");
        StackPane.setAlignment(hudRight, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(hudRight, new javafx.geometry.Insets(0, 12, 8, 0));
        canvasHost.getChildren().addAll(hudLeft, hudRight);

        VBox.setVgrow(canvasHost, Priority.ALWAYS);
        hudLeft.textProperty().bind(Bindings.concat(
                "INGRESS → ", core.workerCountProperty(), " WORKERS → ARCHIVE"));

        VBox headerCopy = new VBox(5, Ui.micro("LIVE OPERATIONS FLOOR"), title, subcopy);
        headerCopy.getStyleClass().add("thread-head-copy");
        HBox.setHgrow(headerCopy, Priority.ALWAYS);
        HBox head = new HBox(14, headerCopy, phaseBadge);
        head.getStyleClass().add("thread-head");

        GridPane stats = new GridPane();
        stats.getStyleClass().add("core-stats");
        for (int i = 0; i < 4; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(25);
            column.setHgrow(Priority.ALWAYS);
            stats.getColumnConstraints().add(column);
        }
        stats.add(stat("STATUS", statusValue), 0, 0);
        stats.add(stat("WORKERS", workersValue), 1, 0);
        stats.add(stat("CHECKED", checkedValue), 2, 0);
        stats.add(stat("TIME", timeValue), 3, 0);

        progress.getStyleClass().addAll("progress", "progress-console");
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.progressProperty().bind(Bindings.createDoubleBinding(
                () -> {
                    double done = core.completedProperty().get();
                    double total = Math.max(1, core.totalProperty().get());
                    return Math.min(1, done / total);
                },
                core.completedProperty(), core.totalProperty()));

        FlowPane workerStrip = new FlowPane(7, 6);
        workerStrip.getStyleClass().add("worker-strip");
        for (int i = 0; i < 4; i++) {
            Label chip = new Label();
            Region dot = new Region();
            dot.getStyleClass().add("worker-chip-dot");
            chip.setGraphic(dot);
            chip.getStyleClass().add("worker-chip");
            chip.setVisible(i < 4);
            chip.setManaged(i < 4);
            workerChips.add(chip);
            workerStrip.getChildren().add(chip);
        }

        Label foot = Ui.label(
                "Java worker pool — every finished item travels from the ingress module to the worker "
                        + "that scored it, then along a cable onto the archive deck.",
                "thread-foot");
        foot.setWrapText(true);

        root = new VBox(14, head, canvasHost, stats, progress, workerStrip, foot);
        root.getStyleClass().add("thread-core-card");
        root.setMinHeight(680);
        VBox.setVgrow(canvasHost, Priority.ALWAYS);

        wireBindings();
    }

    private static VBox stat(String label, Label value) {
        VBox box = new VBox(4, Ui.micro(label), value);
        box.getStyleClass().add("core-stat");
        return box;
    }

    private void wireBindings() {
        core.phaseProperty().addListener((obs, old, value) -> {
            phaseBadge.setText(phaseLabel(value));
            phaseBadge.getStyleClass().setAll("badge", "thread-phase", "thread-phase-" + value);
            title.setText("running".equals(value) ? "Workers are moving" : "Your worker pool");
            subcopy.setText(phaseText(value));
            statusValue.setText(phaseLabel(value));
        });
        core.workerCountProperty().addListener((obs, old, value) ->
                workersValue.setText(String.valueOf(value.intValue())));
        core.completedProperty().addListener((obs, old, value) -> updateChecked());
        core.totalProperty().addListener((obs, old, value) -> updateChecked());
        core.elapsedMillisProperty().addListener((obs, old, value) ->
                timeValue.setText(formatElapsed(value.longValue())));
        core.activeWorkerProperty().addListener((obs, old, value) -> {
            for (int i = 0; i < workerChips.size(); i++) {
                workerChips.get(i).getStyleClass().remove("active");
                if (i == value.intValue()) workerChips.get(i).getStyleClass().add("active");
            }
        });
        updateChecked();
    }

    private void updateChecked() {
        int done = core.completedProperty().get();
        int total = Math.max(core.totalProperty().get(), done);
        checkedValue.setText(done + " / " + total);
    }

    public Region getNode() {
        return root;
    }

    public ThreadCoreView core() {
        return core;
    }

    private static String phaseLabel(String phase) {
        return switch (phase) {
            case "running" -> "CHECKING";
            case "complete" -> "DONE";
            case "failed" -> "STOPPED";
            default -> "READY";
        };
    }

    private static String phaseText(String phase) {
        return switch (phase) {
            case "idle" -> "The floor is on standby. Start a check to watch each item leave the "
                    + "ingress, reach a worker, and settle into the archive.";
            case "running" -> "Items queue at the ingress, travel the lit route to a free worker, "
                    + "then settle into the archive in submission order.";
            case "complete" -> "Every worker has finished. The archive keeps results in the order "
                    + "you submitted them in.";
            default -> "The run stopped before a report was produced.";
        };
    }

    private static String formatElapsed(long millis) {
        double seconds = Math.max(0, millis) / 1000.0;
        if (seconds < 10) return String.format("%.1fs", seconds);
        if (seconds < 60) return Math.round(seconds) + "s";
        int minutes = (int) (seconds / 60);
        int rest = (int) (seconds % 60);
        return minutes + ":" + String.format("%02d", rest);
    }
}
