package com.threatdetector.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Shared chamfered workspace card with a numbered header. */
public final class Panel extends ChamferPane {
    private final VBox body = new VBox(18);

    public Panel(String micro, String title, String stepNumber, String stepLabel) {
        super(14);
        getStyleClass().add("panel");

        VBox head = new VBox(6);
        head.getChildren().addAll(Ui.micro(micro), Ui.label(title, "panel-title"));

        Label number = Ui.mono(stepNumber + "  ", "panel-step-num");
        Label caption = Ui.micro(stepLabel);
        HBox step = new HBox(2, number, caption);
        step.getStyleClass().add("panel-step");
        step.setAlignment(Pos.CENTER_LEFT);

        HBox top = new HBox(16, head, step);
        top.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(head, Priority.ALWAYS);

        body.getStyleClass().add("panel-body");
        VBox.setVgrow(body, Priority.SOMETIMES);

        VBox content = new VBox(22, top, body);
        content.setFillWidth(true);
        getChildren().add(content);
    }

    public VBox body() {
        return body;
    }
}
