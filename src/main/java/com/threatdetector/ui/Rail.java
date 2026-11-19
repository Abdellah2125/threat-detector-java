package com.threatdetector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** Left-hand navigation, live scan status and engine facts. */
public final class Rail extends VBox {
    private final Map<Mode, Button> navButtons = new EnumMap<>(Mode.class);
    private final Region statusDot = new Region();
    private final Label statusName = Ui.label("Idle", "status-name");
    private final Label statusDetail = Ui.label("No active check", "status-detail");

    public Rail(Consumer<Mode> onModeChange) {
        getStyleClass().add("rail");
        setAlignment(Pos.TOP_LEFT);

        HBox brand = new HBox(12, new BrandMark(), brandCopy());
        brand.getStyleClass().add("rail-brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        VBox nav = new VBox(8);
        nav.getChildren().add(Ui.micro("WORKSPACE"));
        navButton(Mode.INSPECT, "◎", "Check", "One link or message", "01", onModeChange, nav);
        navButton(Mode.BATCH, "▤", "Bulk scan", "Many items from a CSV", "02", onModeChange, nav);
        navButton(Mode.METHOD, "▣", "How it works", "Rules, limits, guarantees", "03", onModeChange, nav);

        Region flex = new Region();
        VBox.setVgrow(flex, Priority.ALWAYS);

        VBox status = new VBox(8);
        status.getStyleClass().add("rail-status");
        HBox statusLine = new HBox(9, statusDot, statusName);
        statusLine.setAlignment(Pos.CENTER_LEFT);
        status.getChildren().addAll(statusLine, statusDetail);
        statusDot.getStyleClass().add("status-dot");
        setStatus("Idle", "No active check", false);

        GridPane specs = new GridPane();
        specs.getStyleClass().add("spec-grid");
        spec(specs, 0, "METHOD", "Rule-based");
        spec(specs, 1, "WORKERS", "4 threads");
        spec(specs, 2, "NETWORK", "None");
        spec(specs, 3, "SCALE", "0–100");

        Label foot = Ui.label(
                "Works offline. Nothing leaves this device. A low score is not a guarantee.",
                "rail-foot");
        foot.setWrapText(true);

        getChildren().addAll(brand, nav, flex, status, specs, foot);
    }

    private static VBox brandCopy() {
        VBox box = new VBox(2);
        Label name = Ui.label("Threat Detector", "rail-name");
        Label dot = Ui.label("signal intelligence", "rail-tag");
        box.getChildren().addAll(name, dot);
        return box;
    }

    private void navButton(Mode mode, String icon, String label, String description, String ordinal,
                           Consumer<Mode> onModeChange, VBox parent) {
        Label iconLabel = Ui.label(icon, "nav-icon");
        VBox text = new VBox(0, Ui.label(label, "nav-label"), Ui.label(description, "rail-tag"));
        HBox.setHgrow(text, Priority.ALWAYS);
        Label order = Ui.label(ordinal, "nav-ord");
        HBox graphic = new HBox(10, iconLabel, text, order);
        graphic.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(graphic);
        button.setAccessibleText(label);
        button.getStyleClass().add("nav-item");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> onModeChange.accept(mode));
        navButtons.put(mode, button);
        parent.getChildren().add(button);
    }

    private static void spec(GridPane grid, int row, String key, String value) {
        grid.add(Ui.label(key, "spec-label"), 0, row);
        grid.add(Ui.label(value, "spec-value"), 1, row);
    }

    public void setActive(Mode mode) {
        navButtons.forEach((key, button) -> {
            button.getStyleClass().remove("active");
            if (key == mode) button.getStyleClass().add("active");
        });
    }

    public void setStatus(String name, String detail, boolean live) {
        statusName.setText(name);
        statusDetail.setText(detail);
        statusDot.getStyleClass().remove("live");
        if (live) statusDot.getStyleClass().add("live");
    }
}
