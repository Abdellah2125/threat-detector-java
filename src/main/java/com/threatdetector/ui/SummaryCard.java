package com.threatdetector.ui;

import com.threatdetector.model.ScanReport;
import com.threatdetector.model.Summary;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Compact "Threat Summary" card shown in the right dashboard column. */
public final class SummaryCard extends VBox {
    private final Label statusTitle = Ui.label("No scans yet", "summary-status-title");
    private final Label statusSub = Ui.label("Run a check to see a risk summary.", "summary-status-sub");
    private final Label scansToday = Ui.label("0", "stat-value");
    private final Label highRisk = Ui.label("0", "stat-value stat-high");
    private final Label mediumRisk = Ui.label("0", "stat-value stat-medium");
    private int scanCount;

    public SummaryCard() {
        getStyleClass().addAll("card", "summary-panel", "dash-reveal");
        setSpacing(18);

        VBox status = new VBox(6, statusTitle, statusSub);
        status.getStyleClass().add("summary-status");

        HBox stats = new HBox(10,
                stat(scansToday, "SCANS"),
                stat(highRisk, "HIGH-RISK"),
                stat(mediumRisk, "MEDIUM-RISK"));
        stats.getStyleClass().add("stat-grid");

        getChildren().addAll(
                Ui.label("Threat Summary", "card-title"),
                status,
                stats);
    }

    private static VBox stat(Label value, String label) {
        VBox box = new VBox(6, value, Ui.micro(label));
        box.getStyleClass().add("stat");
        return box;
    }

    public void show(ScanReport report) {
        Summary data = report.summary();
        scanCount += Math.max(1, data.total());
        scansToday.setText(String.valueOf(scanCount));
        highRisk.setText(String.valueOf(data.high()));
        mediumRisk.setText(String.valueOf(data.medium()));

        statusTitle.getStyleClass().removeAll("risk-high", "risk-medium", "risk-low", "risk-na");
        if (data.high() > 0) {
            statusTitle.setText("Threat detected");
            statusTitle.getStyleClass().add("risk-high");
            statusSub.setText(data.high() + " high-risk result"
                    + (data.high() == 1 ? "" : "s") + " in recent scans");
        } else if (data.medium() > 0) {
            statusTitle.setText("Review advised");
            statusTitle.getStyleClass().add("risk-medium");
            statusSub.setText(data.medium() + " medium-risk result"
                    + (data.medium() == 1 ? "" : "s") + " need a closer look");
        } else if (data.notVerified() > 0) {
            statusTitle.setText("Unable to verify");
            statusTitle.getStyleClass().add("risk-na");
            statusSub.setText(data.notVerified() + " item"
                    + (data.notVerified() == 1 ? "" : "s") + " could not be verified");
        } else {
            statusTitle.setText("No threats found");
            statusTitle.getStyleClass().add("risk-low");
            statusSub.setText("Local rules found no suspicious evidence in the latest scan.");
        }
    }

    public void clear() {
        scanCount = 0;
        scansToday.setText("0");
        highRisk.setText("0");
        mediumRisk.setText("0");
        statusTitle.getStyleClass().removeAll("risk-high", "risk-medium", "risk-low", "risk-na");
        statusTitle.setText("No scans yet");
        statusSub.setText("Run a check to see a risk summary.");
    }

    public void setLightTheme(boolean light) {
        // Summary uses CSS token-based style classes; kept for API consistency.
    }
}
