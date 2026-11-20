package com.threatdetector.ui;

import com.threatdetector.model.ItemResult;
import com.threatdetector.model.RiskLevel;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.ScanReport;
import com.threatdetector.model.Summary;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Report area: single verdict card or batch table with filters and distribution. */
public final class ResultsSection {
    private record ToneCount(String key, int count) {
    }

    private final VBox node = new VBox(18);
    private final Label kicker = Ui.micro("REPORT / SINGLE SIGNAL");
    private final Label heading = Ui.label("Verdict", "results-title");
    private final Label liveMeta = Ui.label("● LIVE", "results-live");
    private final Label itemsMeta = Ui.badge("0 ITEMS", "neutral");
    private final Label workersMeta = Ui.badge("— WORKERS", "neutral");
    private final Label durationMeta = Ui.badge("— MS", "neutral");
    private final StackPane body = new StackPane();
    private final VBox placeholder = new VBox(12);
    private final VerdictCard verdict = new VerdictCard();
    private final VBox batchHost = new VBox(14);
    private final HBox filters = new HBox(8);
    private final HBox summary = new HBox(10);
    private final HBox distribution = new HBox(3);
    private final TableView<ItemResult> table = new TableView<>();
    private final List<ItemResult> allRows = new ArrayList<>();
    private String activeFilter = "ALL";

    public ResultsSection() {
        node.getStyleClass().add("results");

        placeholder.getStyleClass().add("result-placeholder");
        placeholder.setAlignment(Pos.CENTER);
        placeholder.getChildren().addAll(
                Ui.label("◎", "placeholder-icon"),
                Ui.label("Results will appear here", "results-title"),
                Ui.label("Analyze a URL or message, or drop a CSV file, to see local rule results.",
                        "placeholder-copy"));

        VBox headingBox = new VBox(3, kicker, heading);
        HBox.setHgrow(headingBox, Priority.ALWAYS);
        HBox meta = new HBox(8, liveMeta, itemsMeta, workersMeta, durationMeta);
        meta.getStyleClass().add("results-meta");
        meta.setAlignment(Pos.CENTER_RIGHT);
        HBox head = new HBox(16, headingBox, meta);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("results-head");

        filters.getStyleClass().add("result-filters");
        VBox summaryWrap = new VBox(10, summary, distribution);
        summaryWrap.getStyleClass().add("summary-wrap");
        batchHost.getChildren().addAll(filters, summaryWrap, table);
        body.getChildren().addAll(verdict, batchHost, placeholder);
        node.getChildren().addAll(head, body);
        buildTable();
        clear();
    }

    private void buildTable() {
        TableColumn<ItemResult, String> index = new TableColumn<>("#");
        index.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().index() + 1)));
        TableColumn<ItemResult, String> kind = new TableColumn<>("Kind");
        kind.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().kind().name().equals("MESSAGE") ? "Message" : "URL"));
        TableColumn<ItemResult, String> target = new TableColumn<>("Target");
        target.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().target()));
        target.setPrefWidth(400);
        TableColumn<ItemResult, String> verdictColumn = new TableColumn<>("Verdict");
        verdictColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(verdictText(cell.getValue())));
        TableColumn<ItemResult, String> score = new TableColumn<>("Score");
        score.setCellValueFactory(cell -> new ReadOnlyStringWrapper(scoreText(cell.getValue())));
        TableColumn<ItemResult, String> worker = new TableColumn<>("Worker");
        worker.setCellValueFactory(cell -> new ReadOnlyStringWrapper("W" + (cell.getValue().workerId() + 1)));
        TableColumn<ItemResult, String> duration = new TableColumn<>("ms");
        duration.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().durationMs())));
        table.getColumns().addAll(index, kind, target, verdictColumn, score, worker, duration);
        table.setPrefHeight(320);
    }

    public void showSingle(ScanReport report, String explanation) {
        kicker.setText("REPORT / SINGLE SIGNAL");
        heading.setText("Verdict");
        updateMeta(report);
        verdict.show(report, explanation);
        batchHost.setVisible(false);
        batchHost.setManaged(false);
        placeholder.setVisible(false);
        placeholder.setManaged(false);
        show();
    }

    public void showBatch(ScanReport report) {
        kicker.setText("REPORT / BATCH INTAKE");
        heading.setText("Batch report");
        updateMeta(report);
        buildSummary(report.summary());
        allRows.clear();
        allRows.addAll(report.results());
        activeFilter = "ALL";
        buildFilters();
        applyFilter();
        verdict.clear();
        batchHost.setVisible(true);
        batchHost.setManaged(true);
        placeholder.setVisible(false);
        placeholder.setManaged(false);
        show();
    }

    private void updateMeta(ScanReport report) {
        itemsMeta.setText(report.summary().total() + " ITEMS");
        workersMeta.setText(report.workersUsed() + " WORKERS");
        durationMeta.setText(report.durationMs() + " MS");
        liveMeta.setText("● COMPLETE");
    }

    private void buildSummary(Summary data) {
        summary.getChildren().setAll(
                card(data.high(), "HIGH RISK", "summary-high"),
                card(data.medium(), "MEDIUM RISK", "summary-medium"),
                card(data.low(), "LOW RISK", "summary-low"),
                card(data.notVerified(), "NOT VERIFIED", "summary-none"),
                card(data.total(), "ITEMS", "summary-total"));
        distribution.getChildren().clear();
        int total = Math.max(1, data.total());
        for (ToneCount entry : List.of(
                new ToneCount("high", data.high()),
                new ToneCount("medium", data.medium()),
                new ToneCount("low", data.low()),
                new ToneCount("none", data.notVerified()))) {
            if (entry.count() <= 0) continue;
            distribution.getChildren().add(segment(entry.count(), total, "dist-" + entry.key()));
        }
    }

    private void buildFilters() {
        filters.getChildren().clear();
        for (String key : List.of("ALL", "HIGH", "MEDIUM", "LOW", "NOT VERIFIED")) {
            int count = switch (key) {
                case "HIGH" -> countTone("high");
                case "MEDIUM" -> countTone("medium");
                case "LOW" -> countTone("low");
                case "NOT VERIFIED" -> countTone("none");
                default -> allRows.size();
            };
            Label chip = Ui.label(key + "  " + count, "filter-chip");
            if (key.equals(activeFilter)) chip.getStyleClass().add("active");
            chip.setOnMouseClicked(event -> {
                activeFilter = key;
                buildFilters();
                applyFilter();
            });
            filters.getChildren().add(chip);
        }
    }

    private int countTone(String tone) {
        int count = 0;
        for (ItemResult row : allRows) {
            if (tone.equals(toneOf(row))) count++;
        }
        return count;
    }

    private void applyFilter() {
        if ("ALL".equals(activeFilter)) {
            table.getItems().setAll(allRows);
            return;
        }
        String tone = switch (activeFilter) {
            case "HIGH" -> "high";
            case "MEDIUM" -> "medium";
            case "LOW" -> "low";
            default -> "none";
        };
        List<ItemResult> filtered = new ArrayList<>();
        for (ItemResult row : allRows) {
            if (toneOf(row).equals(tone)) filtered.add(row);
        }
        table.getItems().setAll(filtered);
    }

    private static String toneOf(ItemResult result) {
        if (result.outcome() instanceof ScanOutcome.Scored scored) {
            return switch (scored.risk()) {
                case HIGH -> "high";
                case MEDIUM -> "medium";
                case LOW -> "low";
            };
        }
        return "none";
    }

    private static VBox card(int value, String label, String tone) {
        VBox box = new VBox(4, Ui.label(String.valueOf(value), "summary-value", tone), Ui.micro(label));
        box.getStyleClass().add("summary-card");
        return box;
    }

    private static Region segment(int count, int total, String style) {
        Region region = new Region();
        region.getStyleClass().addAll("dist-seg", style);
        double width = Math.max(2, 100.0 * count / total);
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    private void show() {
        node.setVisible(true);
        node.setManaged(true);
    }

    public void clear() {
        kicker.setText("REPORT");
        heading.setText("Analysis Results");
        verdict.clear();
        batchHost.setVisible(false);
        batchHost.setManaged(false);
        table.getItems().clear();
        allRows.clear();
        summary.getChildren().clear();
        distribution.getChildren().clear();
        itemsMeta.setText("0 ITEMS");
        workersMeta.setText("— WORKERS");
        durationMeta.setText("— MS");
        liveMeta.setText("");
        placeholder.setVisible(true);
        placeholder.setManaged(true);
        node.setVisible(true);
        node.setManaged(true);
    }

    public Region getNode() {
        return node;
    }

    public void setLightTheme(boolean light) {
        verdict.setLightTheme(light);
    }

    private static String verdictText(ItemResult result) {
        if (result.outcome() instanceof ScanOutcome.Scored scored) {
            return band(scored.risk());
        }
        return "NOT VERIFIED";
    }

    private static String scoreText(ItemResult result) {
        if (result.outcome() instanceof ScanOutcome.Scored scored) {
            return scored.score() + "/100";
        }
        return "—";
    }

    private static String band(RiskLevel risk) {
        return switch (risk) {
            case HIGH -> "HIGH RISK";
            case MEDIUM -> "MEDIUM RISK";
            case LOW -> "LOW RISK";
        };
    }
}
