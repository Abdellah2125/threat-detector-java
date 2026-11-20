package com.threatdetector.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.threatdetector.model.Finding;
import com.threatdetector.model.ItemResult;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.ScanReport;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Full session history view (reference "History" page), showing all scan results accumulated
 * during this run. This is presentation-only over existing in-memory scan results; it does not
 * change detection/runtime logic and does not persist across restarts (no such logic exists).
 */
public final class HistoryPanel {
    private record Entry(ItemResult result, String time) {
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<Entry> allEntries = new ArrayList<>();
    private final ObservableList<Entry> rows = FXCollections.observableArrayList();
    private final Panel panel = new Panel("HISTORY", "Scan History", "—", "Session results");

    private final Label totalTile = tileValue("0");
    private final Label highTile = tileValue("0");
    private final Label mediumTile = tileValue("0");
    private final Label lowTile = tileValue("0");
    private final Label naTile = tileValue("0");
    private final Label meta = Ui.label("No scans recorded in this session yet.", "panel-hint");

    public HistoryPanel() {
        HBox tiles = new HBox(12,
                tile(totalTile, "TOTAL", null),
                tile(highTile, "HIGH RISK", "summary-high"),
                tile(mediumTile, "MEDIUM RISK", "summary-medium"),
                tile(lowTile, "LOW RISK", "summary-low"),
                tile(naTile, "NOT VERIFIED", "summary-none"));
        tiles.setAlignment(Pos.CENTER_LEFT);
        for (var child : tiles.getChildren()) {
            if (child instanceof Region r) {
                HBox.setHgrow(r, Priority.ALWAYS);
                r.setMaxWidth(Double.MAX_VALUE);
            }
        }

        Label subtitle = Ui.label(
                "All results from scans performed in this session. History is kept in memory and "
                        + "resets when the application closes (no persistence logic is modified).",
                "panel-hint");
        subtitle.setWrapText(true);

        TableView<Entry> table = new TableView<>(rows);
        table.getStyleClass().add("history-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(520);
        table.setPlaceholder(Ui.label("No results yet. Run a scan from the Dashboard.", "table-placeholder"));

        table.getColumns().addAll(
                column("INPUT", e -> e.result().target()),
                column("KIND", e -> kindText(e.result())),
                styledColumn("RISK LEVEL", e -> riskText(e.result()), e -> riskClass(e.result())),
                column("SCORE", e -> scoreText(e.result())),
                styledColumn("STATUS", e -> statusText(e.result()), e -> statusClass(e.result())),
                column("DETAILS", e -> details(e.result())),
                column("TIME", e -> e.time()));

        panel.body().getChildren().addAll(subtitle, tiles, table, meta);
    }

    private static Label tileValue(String value) {
        Label l = Ui.label(value, "summary-value");
        return l;
    }

    private static VBox tile(Label value, String label, String tone) {
        if (tone != null) value.getStyleClass().add(tone);
        VBox box = new VBox(6, value, Ui.micro(label));
        box.getStyleClass().add("summary-card");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static TableColumn<Entry, String> column(String title,
            java.util.function.Function<Entry, String> value) {
        TableColumn<Entry, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private static TableColumn<Entry, String> styledColumn(
            String title,
            java.util.function.Function<Entry, String> value,
            java.util.function.Function<Entry, String> style) {
        TableColumn<Entry, String> column = column(title, value);
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
                Entry row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null) getStyleClass().add(style.apply(row));
            }
        });
        return column;
    }

    /** Append all results from a completed scan (newest first). */
    public void add(ScanReport report) {
        String time = LocalDateTime.now().format(TIME);
        for (ItemResult result : report.results()) {
            allEntries.add(0, new Entry(result, time));
        }
        rows.setAll(allEntries);
        refreshSummary();
    }

    public void clear() {
        allEntries.clear();
        rows.clear();
        refreshSummary();
    }

    private void refreshSummary() {
        int total = 0, high = 0, medium = 0, low = 0, na = 0;
        for (Entry e : allEntries) {
            total++;
            if (e.result().outcome() instanceof ScanOutcome.Scored scored) {
                switch (scored.risk()) {
                    case HIGH -> high++;
                    case MEDIUM -> medium++;
                    case LOW -> low++;
                }
            } else {
                na++;
            }
        }
        totalTile.setText(String.valueOf(total));
        highTile.setText(String.valueOf(high));
        mediumTile.setText(String.valueOf(medium));
        lowTile.setText(String.valueOf(low));
        naTile.setText(String.valueOf(na));
        meta.setText(total == 0
                ? "No scans recorded in this session yet."
                : "Showing " + total + " result(s) from this session (newest first).");
    }

    public Region getNode() {
        return panel;
    }

    private static String kindText(ItemResult result) {
        return result.kind() == null ? "—" : result.kind().name();
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

    private static String scoreText(ItemResult result) {
        if (result.outcome() instanceof ScanOutcome.Scored scored) {
            return scored.score() + "/100";
        }
        return "—";
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
            List<String> titles = scored.findings().stream().map(Finding::title).limit(3).toList();
            return String.join(", ", titles);
        }
        ScanOutcome.NotVerified unverified = (ScanOutcome.NotVerified) result.outcome();
        return unverified.reason();
    }
}