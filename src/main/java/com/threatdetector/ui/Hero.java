package com.threatdetector.ui;

import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Hero card styled after the reference dashboard (dark card, accent edge, headline). */
public final class Hero {
    private final HBox node = new HBox(24);

    public Hero() {
        node.getStyleClass().add("hero");
        node.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox(10);
        copy.getStyleClass().add("hero-copy");

        // Headline echoes reference: "Threat Detector" with accent-highlight feel via badge/subtitle
        javafx.scene.control.Label title = Ui.label("Threat Detector", "hero-title");
        javafx.scene.control.Label lede = Ui.label(
                "Check suspicious URLs and messages for potential threats.", "hero-lede");
        lede.setWrapText(true);

        copy.getChildren().addAll(
                title,
                lede,
                Ui.badge("LOCAL RULES  ·  4 WORKERS  ·  OFFLINE", "live"));
        HBox.setHgrow(copy, Priority.ALWAYS);

        // Larger brand mark for hero presence
        BrandMark mark = new BrandMark();
        mark.setScaleX(2.2);
        mark.setScaleY(2.2);
        mark.setMinSize(57, 57);
        mark.setPrefSize(57, 57);
        mark.setMaxSize(57, 57);

        node.getChildren().addAll(copy, mark);
    }

    public HBox getNode() {
        return node;
    }
}
