package com.threatdetector.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Top navigation bar styled after the reference UI, with a simple light/dark theme toggle. */
public final class TopNav extends HBox {
    private final Map<Mode, Button> navButtons = new EnumMap<>(Mode.class);
    private final Button riskButton = Ui.button("Risk Levels", "navlink risk-levels");
    private final Button themeButton = Ui.button("Light mode", "navlink");
    private final Region statusDot = new Region();
    private final javafx.scene.control.Label statusName = Ui.label("Idle", "nav-status-name");
    private final javafx.scene.control.Label statusDetail = Ui.label("No active check", "nav-status-detail");

    public TopNav(Consumer<Mode> onModeChange, Runnable onToggleTheme) {
        getStyleClass().add("app-header");
        setAlignment(Pos.CENTER_LEFT);

        HBox navbar = new HBox(12);
        navbar.getStyleClass().add("navbar");
        navbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(navbar, Priority.ALWAYS);

        HBox brand = new HBox(9, new BrandMark(), Ui.label("Threat Detector", "brand-name"));
        brand.getStyleClass().add("brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        HBox links = new HBox(8);
        links.getStyleClass().add("nav-links");
        links.setAlignment(Pos.CENTER_LEFT);

        // Pages: Dashboard, History, Settings
        navButton(links, Mode.INSPECT, "Dashboard", onModeChange);
        navButton(links, Mode.HISTORY, "History", onModeChange);
        navButton(links, Mode.METHOD, "Settings", onModeChange);

        HBox pages = new HBox(18, brand, links);
        pages.setAlignment(Pos.CENTER_LEFT);

        HBox status = new HBox(8, statusDot, statusName, statusDetail);
        status.getStyleClass().add("nav-status");
        status.setAlignment(Pos.CENTER_LEFT);
        statusDot.getStyleClass().add("status-dot");
        setStatus("Idle", "No active check", false);

        riskButton.setAccessibleText("Open risk level and method information");
        riskButton.setOnAction(event -> onModeChange.accept(Mode.METHOD));

        themeButton.setAccessibleText("Toggle light and dark mode");
        themeButton.setOnAction(event -> onToggleTheme.run());

        HBox actions = new HBox(12, status, themeButton, riskButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        navbar.getChildren().addAll(pages, spacer, actions);
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
            // INSPECT and BATCH are both "Dashboard" scan sub-modes
            boolean active = key == mode || (key == Mode.INSPECT && mode == Mode.BATCH);
            if (active) button.getStyleClass().add("is-current");
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

    public void setTheme(boolean light) {
        themeButton.setText(light ? "Dark mode" : "Light mode");
    }
}
