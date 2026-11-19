package com.threatdetector.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/** Circular score gauge ported from the original ScoreDial component. */
public final class ScoreDial extends Canvas {
    private Integer score;
    private Color tone = Color.web("#a6a49d");

    public ScoreDial() {
        super(140, 140);
        widthProperty().addListener((obs, old, value) -> draw());
        heightProperty().addListener((obs, old, value) -> draw());
        draw();
    }

    public void setScore(Integer value, Color riskTone) {
        this.score = value;
        this.tone = riskTone == null ? Color.web("#a6a49d") : riskTone;
        draw();
    }

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        double size = Math.min(w, h);
        double cx = w / 2;
        double cy = h / 2;
        double radius = size / 2 - 6;
        double stroke = Math.max(8, size * 0.085);

        g.setLineWidth(stroke);
        g.setStroke(Color.web("#181c21"));
        g.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 0, 360, javafx.scene.shape.ArcType.OPEN);

        if (score != null) {
            double sweep = 360.0 * Math.max(0, Math.min(100, score)) / 100.0;
            g.setStroke(tone);
            g.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, -90, sweep, javafx.scene.shape.ArcType.OPEN);
        }

        g.setFill(Color.web("#0e1013"));
        g.fillOval(cx - radius + stroke / 2, cy - radius + stroke / 2,
                (radius - stroke / 2) * 2, (radius - stroke / 2) * 2);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(score == null ? Color.web("#a6a49d") : Color.web("#f0eee8"));
        g.setFont(Font.font("Chakra Petch", 30));
        g.fillText(score == null ? "—" : String.valueOf(score), cx, cy + 4);

        g.setFill(Color.web("#787670"));
        g.setFont(Font.font("JetBrains Mono", 10.5));
        g.fillText(score == null ? "UNSCORED" : "/ 100", cx, cy + 24);
    }
}
