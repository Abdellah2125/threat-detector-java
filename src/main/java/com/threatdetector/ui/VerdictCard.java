package com.threatdetector.ui;

import com.threatdetector.model.Finding;
import com.threatdetector.model.ItemResult;
import com.threatdetector.model.RiskLevel;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.ScanReport;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** Detailed verdict for one signal, including the circular score dial. */
public final class VerdictCard extends ChamferPane {
    private final ScoreDial dial = new ScoreDial();
    private final Label badge = Ui.badge("NOT VERIFIED", "unverified");
    private final Label riskTitle = Ui.label("Not verified", "verdict-risk");
    private final Label description = Ui.label(riskDescription("none"), "verdict-desc");
    private final Label target = Ui.label("", "verdict-target-value");
    private final HBox facts = new HBox(28);
    private final Label scoreFactValue = Ui.label("—", "fact-value");
    private final VBox findings = new VBox(10);

    public VerdictCard() {
        super(16);
        getStyleClass().add("verdict");

        VBox side = new VBox(14, dial, badge, riskTitle, description);
        side.getStyleClass().add("verdict-side");

        VBox main = new VBox(16);
        main.getStyleClass().add("verdict-main");
        main.getChildren().addAll(
                new VBox(4, Ui.micro("INSPECTED TARGET"), target),
                facts,
                Ui.micro("EVIDENCE · EVERY POINT HAS A RULE"),
                findings);
        HBox.setHgrow(main, Priority.ALWAYS);

        facts.setAlignment(Pos.CENTER_LEFT);
        target.setWrapText(true);
        description.setWrapText(true);

        HBox root = new HBox(26, side, main);
        root.setAlignment(Pos.TOP_LEFT);
        getChildren().add(root);
    }

    public void show(ScanReport report, String secondaryExplanation) {
        ItemResult item = report.results().get(0);
        target.setText(item.target());
        facts.getChildren().setAll(
                fact("Kind", targetKind(item)),
                fact("Score", scoreFactValue),
                fact("Duration", item.durationMs() + " ms"),
                fact("Worker", "W" + (item.workerId() + 1)),
                fact("Row", item.rowNumber() == null ? "—" : "#" + item.rowNumber()));
        findings.getChildren().clear();

        if (item.outcome() instanceof ScanOutcome.Scored scored) {
            String tone = tone(scored.risk());
            dial.setScore(scored.score(), toneColor(tone));
            badge.setText(fullLabel(tone));
            badge.getStyleClass().setAll("badge", "badge-" + tone);
            riskTitle.setText(fullLabel(tone));
            riskTitle.getStyleClass().setAll("verdict-risk", "risk-" + tone);
            description.setText(riskDescription(tone));
            scoreFactValue.setText(scored.score() + " / 100");

            if (scored.findings().isEmpty()) {
                findings.getChildren().add(Ui.label(
                        "No named rule fired. This is not a safety guarantee.", "method-body"));
            }
            for (Finding finding : scored.findings()) {
                findings.getChildren().add(findingRow(finding));
            }
        } else {
            ScanOutcome.NotVerified unverified = (ScanOutcome.NotVerified) item.outcome();
            dial.setScore(null, toneColor("none"));
            badge.setText("NOT VERIFIED");
            badge.getStyleClass().setAll("badge", "badge-unverified");
            riskTitle.setText("Not verified");
            riskTitle.getStyleClass().setAll("verdict-risk", "risk-none");
            description.setText(riskDescription("none"));

            VBox callout = new VBox(6);
            callout.getStyleClass().add("callout");
            callout.getChildren().add(Ui.label(
                    "Not evaluated. " + unverified.reason() + " Treat this signal as unknown, not safe.",
                    "method-body"));
            callout.getChildren().add(Ui.mono(unverified.code()));
            findings.getChildren().add(callout);
        }
        if (secondaryExplanation != null && !secondaryExplanation.isBlank()) {
            findings.getChildren().add(Ui.label(
                    "Secondary assessment (optional): " + secondaryExplanation, "method-body"));
        }
        setVisible(true);
        setManaged(true);
    }

    public void clear() {
        setVisible(false);
        setManaged(false);
    }

    private static VBox fact(String label, String value) {
        return fact(label, Ui.label(value, "fact-value"));
    }

    private static VBox fact(String label, Label value) {
        return new VBox(3, Ui.label(label, "fact-label"), value);
    }

    private static VBox findingRow(Finding finding) {
        VBox row = new VBox(5);
        row.getStyleClass().add("finding-row");
        HBox head = new HBox(12, Ui.mono(finding.ruleId(), "finding-id"), Ui.spacer(),
                Ui.label("+" + finding.points(), "finding-points"));
        head.setAlignment(Pos.CENTER_LEFT);
        Label title = Ui.label(finding.title(), "finding-title");
        title.setWrapText(true);
        row.getChildren().addAll(head, title);
        if (!finding.evidence().isEmpty()) {
            FlowPane chips = new FlowPane(6, 4);
            chips.getStyleClass().add("finding-evidence");
            for (String evidence : finding.evidence()) {
                chips.getChildren().add(Ui.mono(evidence, "evidence-chip"));
            }
            row.getChildren().add(chips);
        }
        return row;
    }

    private static String targetKind(ItemResult item) {
        return item.kind().name().equals("MESSAGE") ? "Message" : "URL";
    }

    private static String fullLabel(String tone) {
        return switch (tone) {
            case "high" -> "High risk";
            case "medium" -> "Medium risk";
            case "low" -> "Low risk";
            default -> "Not verified";
        };
    }

    private static String riskDescription(String tone) {
        return switch (tone) {
            case "high" -> "The cumulative score reached 60 or more. Treat this signal as hostile.";
            case "medium" -> "Suspicious evidence reached the 25–59 band. Review the findings before interacting.";
            case "low" -> "Rules fired, but the cumulative score stayed below 25. Low risk is not a guarantee of safety.";
            default -> "The input could not be evaluated. This is unknown, not safe — never act on it as if it passed.";
        };
    }

    private static String tone(RiskLevel risk) {
        return switch (risk) {
            case HIGH -> "high";
            case MEDIUM -> "medium";
            case LOW -> "low";
        };
    }

    private static Color toneColor(String tone) {
        return switch (tone) {
            case "high" -> Color.web("#C2263B");
            case "medium" -> Color.web("#B45309");
            case "low" -> Color.web("#127A48");
            default -> Color.web("#55606A");
        };
    }
}
