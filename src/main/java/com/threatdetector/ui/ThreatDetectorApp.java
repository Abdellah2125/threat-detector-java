package com.threatdetector.ui;

import java.io.IOException;
import java.io.InputStream;

import com.threatdetector.model.ItemResult;
import com.threatdetector.model.ScanReport;
import com.threatdetector.runtime.ScanCoordinator;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** JavaFX dashboard shell: top navigation, hero, workspace, summary and report. */
public final class ThreatDetectorApp extends Application {
    private ScanCoordinator coordinator;
    private TopNav nav;
    private BorderPane root;
    private HBox dashboard;
    private VBox mainColumn;
    private VBox sideColumn;
    private CheckPanel checkPanel;
    private BulkPanel bulkPanel;
    private MethodPanel methodPanel;
    private HistoryPanel historyPanel;
    private ResultsSection results;
    private SummaryCard summaryCard;
    private RecentHistoryCard historyCard;
    private boolean lightTheme;

    @Override
    public void init() {
        coordinator = new ScanCoordinator();
        loadFont("/fonts/chakra-petch-400.ttf");
        loadFont("/fonts/chakra-petch-500.ttf");
        loadFont("/fonts/chakra-petch-600.ttf");
        loadFont("/fonts/chakra-petch-700.ttf");
        loadFont("/fonts/jetbrains-mono.ttf");
    }

    @Override
    public void start(Stage stage) {
        nav = new TopNav(this::switchMode, this::toggleTheme);
        results = new ResultsSection();
        summaryCard = new SummaryCard();
        historyCard = new RecentHistoryCard();
        historyPanel = new HistoryPanel();
        checkPanel = new CheckPanel(coordinator, bridge(), this::switchMode);
        bulkPanel = new BulkPanel(coordinator, bridge(), this::switchMode);
        methodPanel = new MethodPanel();

        Hero hero = new Hero();

        mainColumn = new VBox(20);
        mainColumn.getStyleClass().add("main-column");
        sideColumn = new VBox(20);
        sideColumn.getStyleClass().addAll("side-column", "dash-aside");
        sideColumn.setPrefWidth(320);
        sideColumn.setMinWidth(300);

        dashboard = new HBox(20, mainColumn, sideColumn);
        dashboard.getStyleClass().add("dashboard-layout");
        dashboard.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(mainColumn, Priority.ALWAYS);
        HBox.setHgrow(sideColumn, Priority.NEVER);

        HBox foot = new HBox(14,
                Ui.label("Threat Detector · local rules · four worker threads · no network", "foot-text"),
                Ui.spacer(),
                Ui.label("NOT VERIFIED ≠ SAFE", "foot-right"));
        foot.getStyleClass().add("foot");
        foot.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(20, hero.getNode(), dashboard, foot);
        content.getStyleClass().add("main-content");
        content.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("main-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(nav);
        root.setCenter(scroll);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(1024, Math.min(1440, bounds.getWidth() - 32));
        double height = Math.max(680, Math.min(960, bounds.getHeight() - 32));
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        applyTheme();
        stage.setTitle("Threat Detector");
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setScene(scene);
        stage.show();

        switchMode(Mode.INSPECT);
        nav.setActive(Mode.INSPECT);
    }

    private ScanUiBridge bridge() {
        return new ScanUiBridge() {
            @Override
            public void onStarted(int total, int workers) {
                nav.setStatus("Checking", "0 of " + total + " items", true);
            }

            @Override
            public void onItem(ItemResult result, int completed, int total) {
                nav.setStatus("Checking", completed + " of " + total + " items", true);
            }

            @Override
            public void onFinished(ScanReport report, boolean single, String explanation) {
                nav.setStatus("Check complete",
                        report.summary().total() + " items · " + report.durationMs() + " ms", false);
                summaryCard.show(report);
                historyCard.add(report);
                historyPanel.add(report);
                if (single)
                    results.showSingle(report, explanation);
                else
                    results.showBatch(report);
            }

            @Override
            public void onFailed(String message) {
                nav.setStatus(message, "No report was produced", false);
            }

            @Override
            public void onReset() {
                results.clear();
            }
        };
    }

    private void switchMode(Mode mode) {
        nav.setActive(mode);
        dashboard.getChildren().clear();

        switch (mode) {
            // INSPECT and BATCH are Dashboard scan sub-modes (single input vs CSV batch)
            case INSPECT -> showWorkspace(checkPanel.getNode());
            case BATCH -> showWorkspace(bulkPanel.getNode());
            case HISTORY -> showFullWidth(historyPanel.getNode());
            case METHOD -> showFullWidth(methodPanel.getNode());
        }
    }

    private void showWorkspace(Region scanPanel) {
        mainColumn.getChildren().setAll(scanPanel, results.getNode());
        sideColumn.getChildren().setAll(summaryCard, historyCard);
        VBox.setVgrow(scanPanel, Priority.NEVER);
        VBox.setVgrow(results.getNode(), Priority.NEVER);

        dashboard.getChildren().addAll(mainColumn, sideColumn);
        HBox.setHgrow(mainColumn, Priority.ALWAYS);
        HBox.setHgrow(sideColumn, Priority.NEVER);
    }

    private void showFullWidth(Region panel) {
        panel.setMinWidth(0);
        dashboard.getChildren().add(panel);
        HBox.setHgrow(panel, Priority.ALWAYS);
    }

    private void resetAll() {
        checkPanel.reset();
        bulkPanel.reset();
        results.clear();
        summaryCard.clear();
        historyCard.clear();
        historyPanel.clear();
        nav.setStatus("Idle", "No active check", false);
        switchMode(Mode.INSPECT);
    }

    private void toggleTheme() {
        lightTheme = !lightTheme;
        applyTheme();
    }

    private void applyTheme() {
        if (root == null) return;
        root.getStyleClass().remove("light-theme");
        if (lightTheme) root.getStyleClass().add("light-theme");
        nav.setTheme(lightTheme);
        summaryCard.setLightTheme(lightTheme);
        results.setLightTheme(lightTheme);
    }

    private static void loadFont(String resource) {
        try (InputStream stream = ThreatDetectorApp.class.getResourceAsStream(resource)) {
            if (stream != null)
                Font.loadFont(stream, 12);
        } catch (IOException ignored) {
            // Fonts are optional; CSS falls back to the platform sans font.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
