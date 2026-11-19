package com.threatdetector.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/** Small factories that keep the JavaFX markup close to the React classes. */
public final class Ui {
    private Ui() {
    }

    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    public static Label micro(String text) {
        return label(text, "micro");
    }

    public static Label mono(String text, String... extra) {
        Label label = new Label(text);
        label.getStyleClass().add("mono");
        label.getStyleClass().addAll(extra);
        return label;
    }

    public static Label badge(String text, String tone) {
        Label label = new Label(text);
        label.getStyleClass().addAll("badge", "badge-" + tone);
        return label;
    }

    public static Button button(String text, String style) {
        Button button = new Button(text);
        for (String part : style.split("\\s+")) {
            if (!part.isBlank()) {
                button.getStyleClass().add(part);
            }
        }
        return button;
    }

    public static Region spacer() {
        Region region = new Region();
        region.setMinWidth(0);
        region.setPrefWidth(0);
        region.setMaxWidth(Double.MAX_VALUE);
        return region;
    }
}
