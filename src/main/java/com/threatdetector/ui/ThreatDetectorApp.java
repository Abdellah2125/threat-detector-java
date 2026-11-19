package com.threatdetector.ui;

import com.threatdetector.model.ItemResult;
import com.threatdetector.model.ScanReport;
import com.threatdetector.runtime.ScanCoordinator;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
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

import java.io.IOException;
import java.io.InputStream;

/** JavaFX shell: rail, topbar, hero, workspace, live telemetry and report. */
public final class ThreatDetectorApp extends Application {
    private ScanCoordinator coordinator;
    private Rail rail;
    private Label currentCrumb;
    private HBox workspace;
    private CheckPanel checkPanel;
    private BulkPanel bulkPanel;
    private MethodPanel methodPanel;
    private ThreadCorePanel corePanel;
    private ResultsSection results;

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
        rail = new Rail(this::switchMode);
        results = new ResultsSection();
        corePanel = new ThreadCorePanel();
        checkPanel = new CheckPanel(coordinator, bridge());
        bulkPanel = new BulkPanel(coordinator, bridge());
        methodPanel = new MethodPanel();

        HBox topbar = new HBox(10);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        currentCrumb = Ui.label("Check", "crumb-current");
        HBox crumbs = new HBox(9,
                Ui.label("◈", "crumb-icon"),
                Ui.label("STUDIO", "micro"),
                Ui.label("/", "micro"),
                Ui.label("WORKSPACE", "micro"),
                Ui.label("/", "micro"),
                currentCrumb);
        crumbs.getStyleClass().add("crumbs");
        crumbs.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(crumbs, Priority.ALWAYS);
        javafx.scene.control.Button reset = Ui.button("Reset", "button-ghost");
        HBox actions = new HBox(12,
                Ui.badge("LOCAL RULES", "live"),
                Ui.badge("OFFLINE", "neutral"),
                reset);
        actions.getStyleClass().add("top-actions");
        actions.setAlignment(Pos.CENTER_RIGHT);
        topbar.getChildren().addAll(crumbs, actions);

        Hero hero = new Hero();
        workspace = new HBox(22);
        workspace.getStyleClass().add("workspace");
        workspace.setAlignment(Pos.TOP_LEFT);

        HBox foot = new HBox(14,
                Ui.label("Threat Detector · clear, rule-based risk checks · JavaFX + Java 17+", "foot-text"),
                Ui.spacer(),
                Ui.label("NO NETWORK", "foot-right"),
                Ui.label("NOT VERIFIED ≠ SAFE", "foot-right"));
        foot.getStyleClass().add("foot");
        foot.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(0, topbar, hero.getNode(), guide(), workspace, results.getNode(), foot);
        content.getStyleClass().add("main-content");
        content.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("main-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setLeft(rail);
        root.setCenter(scroll);

        reset.setOnAction(event -> resetAll());

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.max(1024, Math.min(1440, bounds.getWidth() - 32));
        double height = Math.max(640, Math.min(900, bounds.getHeight() - 32));
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setTitle("Threat Detector — Signal Intelligence");
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setScene(scene);
        stage.show();

        switchMode(Mode.INSPECT);
        rail.setActive(Mode.INSPECT);
    }

    private ScanUiBridge bridge() {
        return new ScanUiBridge() {
            @Override
            public void onStarted(int total, int workers) {
                rail.setStatus("Checking", "0 of " + total + " items", true);
                corePanel.core().setWorkers(workers);
                corePanel.core().setCounts(0, total);
                corePanel.core().setPhase("running");
                corePanel.core().setProgress(0);
            }

            @Override
            public void onItem(ItemResult result, int completed, int total) {
                rail.setStatus("Checking", completed + " of " + total + " items", true);
                corePanel.core().dispatch(result.workerId(), result.outcome());
                corePanel.core().setCounts(completed, total);
                corePanel.core().setProgress(total == 0 ? 1 : completed / (double) total);
            }

            @Override
            public void onFinished(ScanReport report, boolean single, String explanation) {
                rail.setStatus("Check complete",
                        report.summary().total() + " items · " + report.durationMs() + " ms", false);
                corePanel.core().setPhase("complete");
                corePanel.core().setProgress(1);
                if (single) results.showSingle(report, explanation);
                else results.showBatch(report);
            }

            @Override
            public void onFailed(String message) {
                rail.setStatus(message, "No report was produced", false);
                corePanel.core().setPhase("failed");
            }

            @Override
            public void onReset() {
                results.clear();
                corePanel.core().reset();
            }
        };
    }

    private void switchMode(Mode mode) {
        rail.setActive(mode);
        currentCrumb.setText(switch (mode) {
            case INSPECT -> "Check";
            case BATCH -> "Bulk scan";
            case METHOD -> "How it works";
        });
        workspace.getChildren().clear();
        switch (mode) {
            case INSPECT -> addWorkspace(checkPanel.getNode(), corePanel.getNode());
            case BATCH -> addWorkspace(bulkPanel.getNode(), corePanel.getNode());
            case METHOD -> {
                workspace.getChildren().add(methodPanel.getNode());
                HBox.setHgrow(methodPanel.getNode(), Priority.ALWAYS);
            }
        }
    }

    private void addWorkspace(Region main, Region side) {
        main.setMinWidth(0);
        side.setMinWidth(280);
        side.prefWidthProperty().bind(Bindings.when(workspace.widthProperty().lessThan(1000))
                .then(320).otherwise(400));
        HBox.setHgrow(main, Priority.ALWAYS);
        HBox.setHgrow(side, Priority.NEVER);
        workspace.getChildren().addAll(main, side);
    }

    /** Compact orientation strip that explains the four zones of the screen. */
    private static Region guide() {
        HBox grid = new HBox(0);
        grid.getStyleClass().add("guide-grid");
        guideItem(grid, "A  NAVIGATION", "Switch workspaces and read live scan status.");
        guideItem(grid, "B  INPUT", "Enter one signal or load a CSV batch for local rules.");
        guideItem(grid, "C  LIVE FLOOR", "Watch items travel from ingress through workers to the archive.");
        guideItem(grid, "D  REPORT", "Read the verdict, score, findings and per-item evidence.");

        VBox guide = new VBox(6, Ui.label("HOW THIS SCREEN IS ORGANIZED", "ui-guide-title"), grid);
        guide.getStyleClass().add("ui-guide");
        return guide;
    }

    private static void guideItem(HBox grid, String key, String copy) {
        VBox item = new VBox(4, Ui.label(key, "guide-key"), Ui.label(copy, "guide-copy"));
        item.getStyleClass().add("guide-item");
        HBox.setHgrow(item, Priority.ALWAYS);
        grid.getChildren().add(item);
    }

    private void resetAll() {
        checkPanel.reset();
        bulkPanel.reset();
        corePanel.core().reset();
        results.clear();
        rail.setStatus("Idle", "No active check", false);
        switchMode(Mode.INSPECT);
    }

    private static void loadFont(String resource) {
        try (InputStream stream = ThreatDetectorApp.class.getResourceAsStream(resource)) {
            if (stream != null) Font.loadFont(stream, 12);
        } catch (IOException ignored) {
            // Fonts are optional; CSS falls back to the platform sans font.
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
