package com.threatdetector.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** White top navigation bar modelled on the dashboard reference. */
public final class TopNav extends HBox {
    private final Map<Mode, Button> navButtons = new EnumMap<>(Mode.class);
    private final Button riskButton = Ui.button("Risk Levels", "navlink risk-levels");
    private final Region statusDot = new Region();
    private final javafx.scene.control.Label statusName = Ui.label("Idle", "nav-status-name");
    private final javafx.scene.control.Label statusDetail = Ui.label("No active check", "nav-status-detail");

    public TopNav(Consumer<Mode> onModeChange) {
        getStyleClass().add("app-header");
        setAlignment(Pos.CENTER_LEFT);

        HBox navbar = new HBox(12);
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(9, new BrandMark(), Ui.label("Threat Detector", "brand-name"));
        brand.getStyleClass().add("brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        HBox links = new HBox(6);
        links.getStyleClass().add("nav-links");
        links.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(links, Priority.ALWAYS);

        navButton(links, Mode.INSPECT, "Dashboard", onModeChange);
        navButton(links, Mode.METHOD, "Settings", onModeChange);
        navButton(links, Mode.CORE, "Hardware", onModeChange);
        navButton(links, Mode.BATCH, "History", onModeChange);

        HBox status = new HBox(8, statusDot, statusName, statusDetail);
        status.getStyleClass().add("nav-status");
        status.setAlignment(Pos.CENTER_LEFT);
        statusDot.getStyleClass().add("status-dot");
        setStatus("Idle", "No active check", false);

        riskButton.setAccessibleText("Open the risk level guide");
        riskButton.setOnAction(event -> onModeChange.accept(Mode.METHOD));

        navbar.getChildren().addAll(brand, links, status, riskButton);
        getChildren().add(navbar);
    }

    private void navButton(HBox parent, Mode mode, String text, Consumer<Mode> onModeChange) {
        Button button = Ui.button(text, "navlink");
        button.setAccessibleText(text);
        button.setOnAction(event -> onModeChange.accept(mode));
        navButtons.put(mode, button);
        parent.getChildren().add(button);
    }

    public void setActive(Mode mode) {
        navButtons.forEach((key, button) -> {
            button.getStyleClass().remove("is-current");
            if (key == mode) button.getStyleClass().add("is-current");
        });
        riskButton.getStyleClass().remove("is-current");
        if (mode == Mode.METHOD) riskButton.getStyleClass().add("is-current");
    }

    public void setStatus(String name, String detail, boolean live) {
        statusName.setText(name);
        statusDetail.setText(detail);
        statusDot.getStyleClass().remove("live");
        if (live) statusDot.getStyleClass().add("live");
    }
}
