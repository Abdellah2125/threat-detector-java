package com.threatdetector.ui;

import com.threatdetector.model.ItemResult;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.ScanReport;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Compact progress indicator embedded in each scan panel. */
public final class ScanProgress extends VBox {
    private final Label status = Ui.label("Idle.", "scan-row");
    private final Label count = Ui.label("0 / 0", "scan-count");
    private final ProgressBar bar = new ProgressBar(0);

    public ScanProgress() {
        getStyleClass().add("scan-progress");
        HBox row = new HBox(10, status, Ui.spacer(), count);
        row.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("progress");
        bar.setMaxWidth(Double.MAX_VALUE);
        getChildren().addAll(row, bar);
        reset();
    }

    public void start(int total, int workers) {
        Platform.runLater(() -> {
            bar.setProgress(0);
            status.setText("Checking " + total + " item(s) on " + workers + " worker(s)…");
            count.setText("0 / " + total);
            show(true);
        });
    }

    public void item(int completed, int total, ItemResult result) {
        Platform.runLater(() -> {
            bar.setProgress(total == 0 ? 1 : completed / (double) total);
            count.setText(completed + " / " + total);
            String verdict = result.outcome() instanceof ScanOutcome.NotVerified
                    ? "not verified" : "scored";
            status.setText("Worker #" + (result.workerId() + 1) + " finished · " + verdict);
        });
    }

    public void finish(ScanReport report) {
        Platform.runLater(() -> {
            bar.setProgress(1);
            count.setText(report.summary().total() + " / " + report.summary().total());
            status.setText("Check complete in " + report.durationMs() + " ms on "
                    + report.workersUsed() + " worker(s).");
        });
    }

    public void reset() {
        bar.setProgress(0);
        status.setText("Idle.");
        count.setText("0 / 0");
        show(false);
    }

    private void show(boolean visible) {
        setVisible(visible);
        setManaged(visible);
    }
}
