package com.threatdetector.ui;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/** Small green shield mark used in the top bar and hero. */
public final class BrandMark extends Pane {
    public BrandMark() {
        setMinSize(26, 26);
        setPrefSize(26, 26);
        setMaxSize(26, 26);

        Polygon shield = new Polygon(
                13, 0.5, 25, 4.6, 25, 13.0,
                13, 25.5, 1, 13.0, 1, 4.6);
        shield.setFill(Color.web("#1E7D3C"));

        Rectangle body = new Rectangle(9.2, 12.2, 7.6, 6.2);
        body.setArcWidth(2);
        body.setArcHeight(2);
        body.setFill(Color.web("#10161C"));

        javafx.scene.shape.Arc shackle = new javafx.scene.shape.Arc(
                13, 12.4, 5.6, 6.2, 180, 180);
        shackle.setFill(Color.TRANSPARENT);
        shackle.setStroke(Color.web("#10161C"));
        shackle.setStrokeWidth(1.5);

        getChildren().addAll(shield, body, shackle);
        setPickOnBounds(false);
    }
}
