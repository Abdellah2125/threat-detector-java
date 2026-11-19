package com.threatdetector.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** The plain-language headline shown above every workspace mode. */
public final class Hero {
    private final HBox node = new HBox(44);

    public Hero() {
        node.getStyleClass().add("hero");
        node.setAlignment(Pos.CENTER_LEFT);

        VBox copy = new VBox();
        copy.getStyleClass().add("hero-copy");
        copy.getChildren().add(Ui.micro("PLAIN-LANGUAGE SIGNAL CHECKS"));

        VBox title = new VBox(0);
        title.setPadding(new Insets(14, 0, 18, 0));
        javafx.scene.control.Label first = Ui.label("Check a link.", "hero-title");
        first.setPadding(Insets.EMPTY);
        javafx.scene.control.Label understand = Ui.label("Understand", "hero-title", "serif");
        understand.setPadding(Insets.EMPTY);
        javafx.scene.control.Label rest = Ui.label(" the risk.", "hero-title");
        rest.setPadding(Insets.EMPTY);
        HBox secondLine = new HBox(understand, rest);
        title.getChildren().addAll(first, secondLine);

        javafx.scene.control.Label lede = Ui.label(
                "Paste a URL or a message and get a clear score in seconds. Four worker "
                        + "threads check it against 17 rules, show what they found, and never "
                        + "call anything safe by default.",
                "hero-lede");

        HBox pipeline = new HBox();
        pipeline.getStyleClass().add("pipeline");
        String[] steps = {"Paste", "Check", "Score", "Review"};
        String[] numbers = {"01", "02", "03", "04"};
        for (int i = 0; i < steps.length; i++) {
            HBox item = new HBox(7);
            item.getStyleClass().add("pipeline-item");
            item.getChildren().addAll(Ui.label(numbers[i], "pipeline-num"), Ui.label(steps[i]));
            pipeline.getChildren().add(item);
        }
        copy.getChildren().addAll(title, lede, pipeline);

        HBox meta = new HBox(16);
        meta.getStyleClass().add("hero-meta");
        meta.getChildren().addAll(
                meta("04", "worker threads, joined before every report"),
                meta("17", "local rules, no model and no network"),
                meta("0–100", "score with named evidence"));

        HBox.setHgrow(copy, Priority.ALWAYS);
        node.getChildren().addAll(copy, meta);
    }

    private static VBox meta(String value, String description) {
        VBox box = new VBox(6);
        box.getStyleClass().add("meta");
        box.getChildren().addAll(Ui.label(value, "meta-value"), Ui.label(description, "meta-label"));
        return box;
    }

    public HBox getNode() {
        return node;
    }
}
