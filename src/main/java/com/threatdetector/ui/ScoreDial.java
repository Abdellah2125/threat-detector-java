package com.threatdetector.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/** Circular score gauge ported from the original ScoreDial component. */
public final class ScoreDial extends Canvas {
    private Integer score;
    private Color tone = Color.web("#55606A");
    private boolean lightTheme;

    public ScoreDial() {
        super(140, 140);
        widthProperty().addListener((obs, old, value) -> draw());
        heightProperty().addListener((obs, old, value) -> draw());
        draw();
    }

    public void setScore(Integer value, Color riskTone) {
        this.score = value;
        this.tone = riskTone == null ? Color.web("#55606A") : riskTone;
        draw();
    }

    public void setLightTheme(boolean light) {
        this.lightTheme = light;
        // Keep arc color consistent when toggling theme on an already-displayed score.
        if (light) {
            if (is(tone, "#FF5A5F")) tone = Color.web("#C2263B");
            else if (is(tone, "#FFB020")) tone = Color.web("#B45309");
            else if (is(tone, "#3DDC84")) tone = Color.web("#127A48");
            else if (is(tone, "#8A93A0")) tone = Color.web("#55606A");
        } else {
            if (is(tone, "#C2263B")) tone = Color.web("#FF5A5F");
            else if (is(tone, "#B45309")) tone = Color.web("#FFB020");
            else if (is(tone, "#127A48")) tone = Color.web("#3DDC84");
            else if (is(tone, "#55606A")) tone = Color.web("#8A93A0");
        }
        draw();
    }

    private static boolean is(Color c, String hex) {
        return c != null && c.toString().equalsIgnoreCase(Color.web(hex).toString());
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

        Color track = lightTheme ? Color.web("#D7DCE1") : Color.web("#2A3328");
        Color inner = lightTheme ? Color.web("#FFFFFF") : Color.web("#0E120D");
        Color text = lightTheme ? Color.web("#10161C") : Color.web("#F2F5F7");
        Color muted = lightTheme ? Color.web("#66727B") : Color.web("#A6B1BB");
        Color unscored = lightTheme ? Color.web("#55606A") : Color.web("#8A93A0");

        g.setLineWidth(stroke);
        g.setStroke(track);
        g.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, 0, 360, javafx.scene.shape.ArcType.OPEN);

        if (score != null) {
            double sweep = 360.0 * Math.max(0, Math.min(100, score)) / 100.0;
            g.setStroke(tone);
            g.strokeArc(cx - radius, cy - radius, radius * 2, radius * 2, -90, sweep, javafx.scene.shape.ArcType.OPEN);
        }

        g.setFill(inner);
        g.fillOval(cx - radius + stroke / 2, cy - radius + stroke / 2,
                (radius - stroke / 2) * 2, (radius - stroke / 2) * 2);

        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(score == null ? unscored : text);
        g.setFont(Font.font("Chakra Petch", 30));
        g.fillText(score == null ? "—" : String.valueOf(score), cx, cy + 4);

        g.setFill(muted);
        g.setFont(Font.font("JetBrains Mono", 10.5));
        g.fillText(score == null ? "UNSCORED" : "/ 100", cx, cy + 24);
    }
}
