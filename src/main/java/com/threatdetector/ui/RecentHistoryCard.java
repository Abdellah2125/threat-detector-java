package com.threatdetector.ui;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.threatdetector.model.Finding;
import com.threatdetector.model.ItemResult;
import com.threatdetector.model.RiskLevel;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.ScanReport;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

/** Compact recent-results table for the right dashboard column. */
public final class RecentHistoryCard extends VBox {
    private record Row(ItemResult result, String time) {
    }

    private static final int MAX_ROWS = 5;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");

    private final ObservableList<Row> rows = FXCollections.observableArrayList();

    public RecentHistoryCard() {
        getStyleClass().addAll("card", "history-panel", "dash-reveal");
        setSpacing(14);

        VBox heading = new VBox(5,
                Ui.label("Analysis Results", "card-title"),
                Ui.label("Five most recent scan results, newest first", "card-subtitle"));
        heading.getStyleClass().add("card-heading");

        TableView<Row> table = new TableView<>(rows);
        table.getStyleClass().add("history-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(286);
        table.setPlaceholder(Ui.label("No results yet.", "table-placeholder"));

        table.getColumns().addAll(
                column("INPUT", row -> row.result().target()),
                styledColumn("RISK LEVEL", row -> riskText(row.result()), row -> riskClass(row.result())),
                styledColumn("STATUS", row -> statusText(row.result()), row -> statusClass(row.result())),
                column("DETAILS", row -> details(row.result())),
                column("TIME", row -> row.time()));

        getChildren().addAll(heading, table);
    }

    private static TableColumn<Row, String> column(String title, java.util.function.Function<Row, String> value) {
        TableColumn<Row, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private static TableColumn<Row, String> styledColumn(
            String title,
            java.util.function.Function<Row, String> value,
            java.util.function.Function<Row, String> style) {
        TableColumn<Row, String> column = column(title, value);
        column.setCellFactory(cell -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("risk-low", "risk-medium", "risk-high", "risk-na",
                        "status-verified", "status-unverified");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                Row row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null) getStyleClass().add(style.apply(row));
            }
        });
        return column;
    }

    public void add(ScanReport report) {
        String time = LocalTime.now().format(TIME);
        for (ItemResult result : report.results()) {
            rows.add(0, new Row(result, time));
        }
        while (rows.size() > MAX_ROWS) {
            rows.remove(rows.size() - 1);
        }
    }

    public void clear() {
        rows.clear();
    }

    private static String riskText(ItemResult result) {
        if (result.outcome() instanceof ScanOutcome.Scored scored) {
            return switch (scored.risk()) {
                case HIGH -> "High";
                case MEDIUM -> "Medium";
                case LOW -> "Low";
            };
        }
        return "N/A";
    }

    private static String riskClass(ItemResult result) {
        if (result.outcome() instanceof ScanOutcome.Scored scored) {
            return switch (scored.risk()) {
                case HIGH -> "risk-high";
                case MEDIUM -> "risk-medium";
                case LOW -> "risk-low";
            };
        }
        return "risk-na";
    }

    private static String statusText(ItemResult result) {
        return result.outcome() instanceof ScanOutcome.Scored ? "Verified" : "Not Verified";
    }

    private static String statusClass(ItemResult result) {
        return result.outcome() instanceof ScanOutcome.Scored ? "status-verified" : "status-unverified";
    }

    private static String details(ItemResult result) {
        if (result.outcome() instanceof ScanOutcome.Scored scored) {
            if (scored.findings().isEmpty()) return "No suspicious activity detected.";
            List<String> titles = scored.findings().stream().map(Finding::title).limit(2).toList();
            return String.join(", ", titles);
        }
        ScanOutcome.NotVerified unverified = (ScanOutcome.NotVerified) result.outcome();
        return unverified.reason();
    }
}
