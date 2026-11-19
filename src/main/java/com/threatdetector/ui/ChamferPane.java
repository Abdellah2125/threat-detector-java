package com.threatdetector.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * Square panel with the two L-shaped accent brackets used by the original
 * design: amber at the top-left, coral at the bottom-right.
 */
public class ChamferPane extends Pane {
    private static final double INSET = 7;
    private static final double LENGTH = 16;

    private final Line topHorizontal = line(Color.web("#e8a33d"));
    private final Line topVertical = line(Color.web("#e8a33d"));
    private final Line bottomHorizontal = line(Color.web("#d9705f"));
    private final Line bottomVertical = line(Color.web("#d9705f"));

    public ChamferPane(double unusedCut) {
        getChildren().addAll(topHorizontal, topVertical, bottomHorizontal, bottomVertical);
    }

    private static Line line(Color color) {
        Line result = new Line();
        result.setStroke(color);
        result.setStrokeWidth(2);
        result.setMouseTransparent(true);
        result.setManaged(false);
        return result;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        Insets insets = getInsets();
        double x = insets.getLeft();
        double y = insets.getTop();
        double innerW = Math.max(0, w - insets.getLeft() - insets.getRight());
        double innerH = Math.max(0, h - insets.getTop() - insets.getBottom());
        for (Node child : getManagedChildren()) {
            if (child.isResizable()) child.resizeRelocate(x, y, innerW, innerH);
            else child.relocate(x, y);
        }

        double left = INSET;
        double top = INSET;
        double right = w - INSET;
        double bottom = h - INSET;
        topHorizontal.setStartX(left);
        topHorizontal.setStartY(top);
        topHorizontal.setEndX(left + LENGTH);
        topHorizontal.setEndY(top);

        topVertical.setStartX(left);
        topVertical.setStartY(top);
        topVertical.setEndX(left);
        topVertical.setEndY(top + LENGTH);

        bottomHorizontal.setStartX(right - LENGTH);
        bottomHorizontal.setStartY(bottom);
        bottomHorizontal.setEndX(right);
        bottomHorizontal.setEndY(bottom);

        bottomVertical.setStartX(right);
        bottomVertical.setStartY(bottom - LENGTH);
        bottomVertical.setEndX(right);
        bottomVertical.setEndY(bottom);

        topHorizontal.toFront();
        topVertical.toFront();
        bottomHorizontal.toFront();
        bottomVertical.toFront();
    }
}
