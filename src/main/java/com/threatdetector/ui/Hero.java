package com.threatdetector.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** White hero card with the reference dashboard's green edge accent. */
public final class Hero {
    private final HBox node = new HBox(24);

    public Hero() {
        node.getStyleClass().add("hero");
        node.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(10);
        copy.getStyleClass().add("hero-copy");
        copy.getChildren().addAll(
                Ui.label("Threat Detector", "hero-title"),
                Ui.label("Check suspicious URLs and messages for potential threats.", "hero-lede"),
                Ui.badge("LOCAL RULES  ·  4 WORKERS  ·  OFFLINE", "live"));
        HBox.setHgrow(copy, Priority.ALWAYS);

        node.getChildren().addAll(copy, new BrandMark());
    }

    public HBox getNode() {
        return node;
    }
}
