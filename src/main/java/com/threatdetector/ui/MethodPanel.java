package com.threatdetector.ui;

import com.threatdetector.detection.RuleCatalog;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Workspace 03: verdict scale, concurrency contract, rules and limits. */
public final class MethodPanel {
    private record Card(ChamferPane pane, VBox body) {
    }

    private final Panel panel = new Panel("03 / METHOD", "Rules, limits, and guarantees", "03", "Method");

    public MethodPanel() {
        GridPane top = grid();
        Card scale = card("Verdict scale", "Every item is scored",
                "Points from each finding are added, then capped at 100. The final band decides the verdict. There is no Safe level.");
        scale.body().getChildren().add(scaleList());
        Card contract = card("Concurrency contract", "Parallel, still deterministic",
                "A scoped Java worker pool owns the checks. The UI only renders reports and live telemetry, so a slow screen can never reorder or drop results.");
        contract.body().getChildren().add(ticks(List.of(
                "Scoped worker threads all join before the report returns.",
                "One atomic cursor hands each item to exactly one worker.",
                "Results keep intake order even when workers finish out of order.",
                "A failure inside one item is isolated and reported as Not verified.",
                "Start, item, and finish telemetry streams while the scan runs.")));
        top.add(scale.pane(), 0, 0);
        top.add(contract.pane(), 1, 0);

        GridPane rules = grid();
        Card urlRules = card("URL rules", null, null);
        urlRules.body().getChildren().add(rulesTable(RuleCatalog.URL_RULES));
        Card messageRules = card("Message rules", null, null);
        messageRules.body().getChildren().add(rulesTable(RuleCatalog.MESSAGE_RULES));
        rules.add(urlRules.pane(), 0, 0);
        rules.add(messageRules.pane(), 1, 0);

        GridPane bottom = grid();
        Card limits = card("Operating limits", "Bounded on purpose", null);
        limits.body().getChildren().add(ticks(List.of(
                "Four worker threads, never more than the item count.",
                "5,000 rows and 5 MB per CSV.",
                "2,048 characters per URL, 4,000 per message.",
                "No network calls: every verdict is computed locally.")));
        Card reading = card("Reading a verdict", "Unknown is not safe",
                "Rule-based screening can miss hostile signals, so a low score is only an absence of evidence. "
                        + "Inputs the engine cannot evaluate are reported as Not verified and must be treated as unknown.");
        Label callout = Ui.label(RuleCatalog.URL_RULES.size() + RuleCatalog.MESSAGE_RULES.size()
                + " deterministic rules run per item: " + RuleCatalog.URL_RULES.size()
                + " URL checks and " + RuleCatalog.MESSAGE_RULES.size() + " message checks.", "callout-text");
        callout.setWrapText(true);
        VBox calloutBox = new VBox(callout);
        calloutBox.getStyleClass().add("callout");
        reading.body().getChildren().add(calloutBox);
        bottom.add(limits.pane(), 0, 0);
        bottom.add(reading.pane(), 1, 0);

        VBox content = new VBox(18, top, rules, bottom);
        content.getStyleClass().add("method");
        panel.body().getChildren().add(content);
        for (Card card : List.of(scale, contract, urlRules, messageRules, limits, reading)) {
            GridPane.setHgrow(card.pane(), Priority.ALWAYS);
        }
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("method-grid");
        ColumnConstraints left = new ColumnConstraints();
        ColumnConstraints right = new ColumnConstraints();
        left.setPercentWidth(50);
        right.setPercentWidth(50);
        left.setHgrow(Priority.ALWAYS);
        right.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(left, right);
        return grid;
    }

    private static Card card(String kicker, String title, String body) {
        ChamferPane pane = new ChamferPane(10);
        pane.getStyleClass().addAll("panel", "method-card");
        VBox content = new VBox(8);
        content.getChildren().add(Ui.micro(kicker));
        if (title != null) content.getChildren().add(Ui.label(title, "method-card__title"));
        if (body != null) {
            Label text = Ui.label(body, "method-card__body");
            text.setWrapText(true);
            content.getChildren().add(text);
        }
        pane.getChildren().add(content);
        return new Card(pane, content);
    }

    private static Region scaleList() {
        VBox list = new VBox(9);
        list.getStyleClass().add("scale-list");
        list.getChildren().addAll(
                scaleRow("high", "High risk", "60–100"),
                scaleRow("medium", "Medium risk", "25–59"),
                scaleRow("low", "Low risk", "0–24"),
                scaleRow("none", "Not verified", "Unscored"));
        return list;
    }

    private static Region scaleRow(String tone, String name, String range) {
        Region dot = new Region();
        dot.getStyleClass().addAll("scale-row__dot", "scale-dot", tone);
        HBox row = new HBox(11, dot, Ui.label(name, "scale-row__name"), Ui.spacer(),
                Ui.mono(range, "scale-row__range"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("scale-row");
        return row;
    }

    private static Region ticks(List<String> lines) {
        VBox list = new VBox(10);
        list.getStyleClass().add("tick-list");
        for (String line : lines) {
            Label tick = Ui.label("✓  " + line, "tick");
            tick.setWrapText(true);
            list.getChildren().add(tick);
        }
        return list;
    }

    private static TableView<RuleCatalog.RuleSpec> rulesTable(List<RuleCatalog.RuleSpec> rules) {
        TableColumn<RuleCatalog.RuleSpec, String> id = new TableColumn<>("Rule");
        id.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));
        TableColumn<RuleCatalog.RuleSpec, String> signal = new TableColumn<>("Signal");
        signal.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().signal()));
        signal.setPrefWidth(300);
        TableColumn<RuleCatalog.RuleSpec, Integer> points = new TableColumn<>("Points");
        points.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().points()));
        TableView<RuleCatalog.RuleSpec> table = new TableView<>();
        table.getStyleClass().add("rules-table");
        table.getColumns().addAll(id, signal, points);
        table.getItems().setAll(rules);
        table.setPrefHeight(250);
        return table;
    }

    public Region getNode() {
        return panel;
    }
}
