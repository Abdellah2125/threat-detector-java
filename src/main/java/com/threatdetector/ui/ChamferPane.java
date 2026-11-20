package com.threatdetector.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/** Quiet panel with a subtle accent corner mark, recolored for the dark reference theme. */
public class ChamferPane extends Pane {
    private static final double INSET = 7;
    private static final double LENGTH = 14;

    // Accent corner uses reference dark accent (#8DEB6B) at low opacity for a subtle glow hint
    private final Line topHorizontal = line(Color.web("#8DEB6B", 0.28));
    private final Line topVertical = line(Color.web("#8DEB6B", 0.28));
    private final Line bottomHorizontal = line(Color.TRANSPARENT);
    private final Line bottomVertical = line(Color.TRANSPARENT);

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
