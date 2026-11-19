package com.threatdetector.ui;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;

/** Monochrome geometric mark used in the rail. */
public final class BrandMark extends Pane {
    public BrandMark() {
        setMinSize(46, 46);
        setPrefSize(46, 46);
        setMaxSize(46, 46);

        Polygon outer = polygon(
                23, 1, 43, 12, 43, 34, 23, 45, 3, 34, 3, 12);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(Color.web("#e8a33d"));
        outer.setStrokeWidth(1.6);

        Polygon inner = polygon(
                23, 9, 36, 16, 36, 30, 23, 37, 10, 30, 10, 16);
        inner.setFill(Color.rgb(232, 163, 61, 0.07));
        inner.setStroke(Color.web("#e8a33d", 0.45));
        inner.setStrokeWidth(1);

        Circle core = new Circle(23, 23, 4.2, Color.web("#ff8a7a"));
        Circle halo = new Circle(23, 23, 8.0, Color.rgb(255, 138, 122, 0.12));
        getChildren().addAll(outer, inner, halo, core);
        setPickOnBounds(false);
    }

    private static Polygon polygon(double... points) {
        Polygon polygon = new Polygon();
        for (double point : points) polygon.getPoints().add(point);
        return polygon;
    }
}
