package com.threatdetector.ui;

import com.threatdetector.model.RiskLevel;
import com.threatdetector.model.ScanOutcome;
import javafx.animation.AnimationTimer;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;

import java.util.ArrayList;
import java.util.List;

/**
 * Isometric JavaFX 3D operations floor. It is the Java equivalent of the
 * original Three.js renderer: an ingress module, four worker towers, routed
 * cables and an archive deck that fills in submission order.
 */
public final class ThreadCoreView extends StackPane {
    private static final double FLOOR_TOP = 0.08;
    private static final double CABLE_Y = FLOOR_TOP + 0.052;
    private static final double DECK_X = 4.1;
    private static final double DECK_Z = 1.5;
    private static final double[] WORKER_XS = {-1.75, -0.58, 0.59, 1.76};
    private static final double WORKER_Z = -0.75;
    private static final double[] DECK_COL_XS = {-1.02, -0.34, 0.34, 1.02};
    private static final double[] DECK_ROW_ZS = {-0.72, 0.0, 0.72};

    private static final Color BACKGROUND = Color.web("#0f1216");
    private static final Color PLATE = Color.web("#181c22");
    private static final Color PLATE_EDGE = Color.web("#30363f");
    private static final Color GRAPHITE = Color.web("#3a414a");
    private static final Color GRAPHITE_DARK = Color.web("#22272d");
    private static final Color AMBER = Color.web("#e8a33d");
    private static final Color AMBER_DEEP = Color.web("#b0792c");
    private static final Color PALE_CABLE = Color.web("#5a636e");
    private static final Color CARD_PAPER = Color.web("#d8d2c5");
    private static final Color CARD_INK = Color.web("#1c2025");

    private final Group world = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;
    private final AnimationTimer timer;

    private final List<WorkerVisual> workerVisuals = new ArrayList<>();
    private final List<SlotVisual> slots = new ArrayList<>();
    private final List<List<Point3D>> approachRoutes = new ArrayList<>();
    private final List<List<Point3D>> egressRoutes = new ArrayList<>();
    private final List<PacketVisual> packets = new ArrayList<>();

    private final Box progressFill;
    private final Box scanLine;

    private final StringProperty phase = new SimpleStringProperty("idle");
    private final IntegerProperty workerCount = new SimpleIntegerProperty(4);
    private final IntegerProperty completed = new SimpleIntegerProperty(0);
    private final IntegerProperty total = new SimpleIntegerProperty(0);
    private final LongProperty elapsedMillis = new SimpleLongProperty(0);
    private final IntegerProperty activeWorker = new SimpleIntegerProperty(-1);

    private double time;
    private double runStartedAt;
    private double activeUntil;
    private int nextSlot;
    private long lastFrame;

    public ThreadCoreView() {
        camera.setFieldOfView(30);
        camera.setNearClip(0.1);
        camera.setFarClip(120);

        world.getChildren().addAll(
                new AmbientLight(Color.web("#6f7782")),
                keyLight(),
                fillLight(),
                new PointLight(Color.web("#6b4a1f")));
        world.getChildren().get(3).setTranslateX(1.6);
        world.getChildren().get(3).setTranslateY(1.4);
        world.getChildren().get(3).setTranslateZ(-1.4);

        buildFloor();
        scanLine = buildScanLine();
        progressFill = buildProgressRail();
        buildIngress();
        buildWorkers();
        buildDeck();
        buildCables();

        subScene = new SubScene(world, 400, 430, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(BACKGROUND);
        getChildren().add(subScene);
        setPrefSize(400, 430);
        setMinSize(280, 320);

        widthProperty().addListener((obs, old, value) -> subScene.setWidth(value.doubleValue()));
        heightProperty().addListener((obs, old, value) -> subScene.setHeight(value.doubleValue()));

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                tick(now);
            }
        };
        timer.start();
    }

    private static PointLight keyLight() {
        PointLight light = new PointLight(Color.web("#b6bcc6"));
        light.setTranslateX(7);
        light.setTranslateY(13);
        light.setTranslateZ(8);
        light.setMaxRange(60);
        return light;
    }

    private static PointLight fillLight() {
        PointLight light = new PointLight(Color.web("#4a4f57"));
        light.setTranslateX(-9);
        light.setTranslateY(6);
        light.setTranslateZ(-6);
        light.setMaxRange(60);
        return light;
    }

    // ---------- public bridge API ----------

    public void setWorkers(int count) {
        workerCount.set(Math.max(1, Math.min(4, count)));
        for (int i = 0; i < workerVisuals.size(); i++) {
            workerVisuals.get(i).root.setVisible(i < count);
        }
    }

    public void setPhase(String value) {
        phase.set(value == null ? "idle" : value);
        if ("running".equals(phase.get())) {
            runStartedAt = time;
            elapsedMillis.set(0);
        }
    }

    public void setProgress(double fraction) {
        progressValue = Math.max(0, Math.min(1, fraction));
    }

    public void setCounts(int done, int totalItems) {
        completed.set(Math.max(0, done));
        total.set(Math.max(0, totalItems));
    }

    public void dispatch(int workerId, ScanOutcome outcome) {
        int index = Math.max(0, Math.min(workerCount.get() - 1, workerId));
        Color tone = colorOf(outcome);
        WorkerVisual worker = workerVisuals.get(index);
        worker.flashUntil = time + 0.65;
        worker.ledMaterial.setDiffuseColor(tone);
        worker.stripMaterial.setDiffuseColor(tone);
        activeWorker.set(index);
        activeUntil = time + 0.7;
        completed.set(completed.get() + 1);

        List<Point3D> route = new ArrayList<>();
        route.addAll(approachRoutes.get(index));
        route.add(new Point3D(worker.x, FLOOR_TOP + 0.75, WORKER_Z));
        route.addAll(egressRoutes.get(index));
        route.add(deckSlotPoint(nextSlot % slots.size()));

        Sphere core = sphere(0.10, tone, 1.0);
        Sphere glow = sphere(0.20, tone, 0.24);
        world.getChildren().addAll(glow, core);
        packets.add(new PacketVisual(core, glow, route, tone));
        nextSlot++;
    }

    public void reset() {
        for (PacketVisual packet : packets) {
            world.getChildren().removeAll(packet.core, packet.glow);
        }
        packets.clear();
        for (SlotVisual slot : slots) {
            slot.card.setVisible(false);
            slot.stripe.setVisible(false);
        }
        nextSlot = 0;
        completed.set(0);
        total.set(0);
        elapsedMillis.set(0);
        activeWorker.set(-1);
        progressValue = -1;
        phase.set("idle");
        for (WorkerVisual worker : workerVisuals) {
            worker.flashUntil = 0;
            worker.ledMaterial.setDiffuseColor(AMBER);
            worker.stripMaterial.setDiffuseColor(AMBER);
        }
    }

    public StringProperty phaseProperty() {
        return phase;
    }

    public IntegerProperty workerCountProperty() {
        return workerCount;
    }

    public IntegerProperty completedProperty() {
        return completed;
    }

    public IntegerProperty totalProperty() {
        return total;
    }

    public LongProperty elapsedMillisProperty() {
        return elapsedMillis;
    }

    public IntegerProperty activeWorkerProperty() {
        return activeWorker;
    }

    private double progressValue = -1;

    // ---------- scene construction ----------

    private void buildFloor() {
        box(11.8, 0.36, 6.8, GRAPHITE, 0.15, -0.18, 0.35, 1.0);
        box(11.5, 0.08, 6.5, PLATE, 0.15, 0.04, 0.35, 1.0);
        box(11.5, 0.012, 0.012, PLATE_EDGE, 0.15, 0.086, -2.9, 0.7);
        box(11.5, 0.012, 0.012, PLATE_EDGE, 0.15, 0.086, 3.6, 0.7);

        PhongMaterial minor = material(PLATE_EDGE, Color.web("#ffffff"), 8);
        for (double x = -5.5; x <= 5.81; x += 0.4) {
            addBox(0.008, 0.004, 6.4, minor, x, FLOOR_TOP + 0.004, 0.35, 0.30);
        }
        for (double z = -2.85; z <= 3.56; z += 0.4) {
            addBox(11.4, 0.004, 0.008, minor, 0.15, FLOOR_TOP + 0.005, z, 0.30);
        }
        PhongMaterial major = material(Color.web("#3b424b"), Color.web("#ffffff"), 8);
        for (double x = -5.5; x <= 5.81; x += 2.0) {
            addBox(0.012, 0.004, 6.4, major, x, FLOOR_TOP + 0.007, 0.35, 0.35);
        }
        for (double z = -2.85; z <= 3.56; z += 2.0) {
            addBox(11.4, 0.004, 0.012, major, 0.15, FLOOR_TOP + 0.008, z, 0.35);
        }
    }

    private Box buildScanLine() {
        Box line = box(10.6, 0.01, 0.035, AMBER, 0.15, FLOOR_TOP + 0.012, -2.6, 0.20);
        return line;
    }

    private Box buildProgressRail() {
        box(9.7, 0.02, 0.08, Color.web("#4a525c"), 0.15, FLOOR_TOP + 0.014, 3.28, 1.0);
        Box fill = box(9.7, 0.03, 0.09, AMBER, -4.7, FLOOR_TOP + 0.024, 3.28, 1.0);
        fill.setScaleX(0.001);
        return fill;
    }

    private void buildIngress() {
        double x = -4.3;
        double z = 1.15;
        box(1.5, 0.08, 1.9, GRAPHITE_DARK, x, FLOOR_TOP + 0.04, z, 1.0);
        box(1.38, 1.1, 1.78, Color.web("#59636f"), x, FLOOR_TOP + 0.63, z, 1.0);
        box(1.46, 0.09, 1.86, Color.web("#6b7683"), x, FLOOR_TOP + 1.225, z, 1.0);
        box(1.42, 0.03, 1.82, Color.web("#2b3037"), x, FLOOR_TOP + 1.168, z, 0.8);
        for (int i = 0; i < 4; i++) {
            box(0.03, 0.02, 0.66, AMBER, x - 0.54 + i * 0.36, FLOOR_TOP + 1.28, z, 0.85);
        }
    }

    private void buildWorkers() {
        for (int index = 0; index < WORKER_XS.length; index++) {
            double x = WORKER_XS[index];
            double z = WORKER_Z;
            Group root = new Group();
            world.getChildren().add(root);
            addBox(root, 0.88, 0.08, 0.88,
                    material(GRAPHITE_DARK, Color.WHITE, 24), x, FLOOR_TOP + 0.04, z, 1.0);
            addBox(root, 0.6, 1.3, 0.6,
                    material(Color.web("#6d7783"), Color.WHITE, 24), x, FLOOR_TOP + 0.73, z, 1.0);

            PhongMaterial cap = material(Color.web("#2b3037"), AMBER_DEEP, 42);
            PhongMaterial strip = material(AMBER, Color.web("#ffffff"), 48);
            PhongMaterial led = material(AMBER, Color.web("#ffffff"), 80);
            addBox(root, 0.72, 0.09, 0.72, cap, x, FLOOR_TOP + 1.425, z, 1.0);
            for (int row = 0; row < 3; row++) {
                addBox(root, 0.46, 0.012, 0.024,
                        material(Color.web("#5f6975"), Color.WHITE, 20),
                        x, FLOOR_TOP + 1.474, z - 0.19 + row * 0.12, 0.7);
            }
            addBox(root, 0.035, 0.86, 0.02, strip, x, FLOOR_TOP + 0.82, z + 0.305, 0.55);
            addBox(root, 0.13, 0.05, 0.02, led, x, FLOOR_TOP + 1.30, z + 0.31, 1.0);
            workerVisuals.add(new WorkerVisual(root, x, z, cap, strip, led));
        }
    }

    private void buildDeck() {
        box(3.2, 0.3, 2.6, Color.web("#20252b"), DECK_X, 0.15, DECK_Z, 1.0);
        box(3.1, 0.08, 2.5, Color.web("#5b6572"), DECK_X, 0.34, DECK_Z, 1.0);
        box(3.1, 0.02, 0.02, AMBER, DECK_X, 0.34, DECK_Z + 1.26, 1.0);
        for (double dz : DECK_ROW_ZS) {
            for (double dx : DECK_COL_XS) {
                box(0.56, 0.016, 0.7, Color.web("#87919d"), DECK_X + dx, 0.386, DECK_Z + dz, 1.0);
                Box card = box(0.56, 0.02, 0.7, CARD_PAPER, DECK_X + dx, 0.405, DECK_Z + dz, 1.0);
                card.setVisible(false);
                Box stripe = box(0.56, 0.024, 0.12, Color.web("#78966c"), DECK_X + dx, 0.42, DECK_Z + dz - 0.26, 1.0);
                stripe.setVisible(false);
                slots.add(new SlotVisual(card, stripe));
            }
        }
    }

    private Point3D deckSlotPoint(int index) {
        int row = index / DECK_COL_XS.length;
        int column = index % DECK_COL_XS.length;
        return new Point3D(DECK_X + DECK_COL_XS[column], 0.46, DECK_Z + DECK_ROW_ZS[row]);
    }

    private void buildCables() {
        PhongMaterial amberCable = material(Color.web("#bf8b3c"), AMBER, 52);
        for (int index = 0; index < WORKER_XS.length; index++) {
            double x = WORKER_XS[index];
            double lane = 0.95 + index * 0.07;
            List<Point3D> approach = List.of(
                    new Point3D(-3.58, CABLE_Y, lane),
                    new Point3D(x - 0.55, CABLE_Y, lane),
                    new Point3D(x - 0.55, CABLE_Y, -1.44),
                    new Point3D(x, CABLE_Y, -1.44),
                    new Point3D(x, CABLE_Y, -1.11));
            addCable(approach, amberCable);
            approachRoutes.add(approach);

            double egressLane = 1.35 + index * 0.1;
            List<Point3D> egress = List.of(
                    new Point3D(x + 0.36, CABLE_Y, WORKER_Z),
                    new Point3D(x + 0.58, CABLE_Y, WORKER_Z),
                    new Point3D(x + 0.58, CABLE_Y, egressLane),
                    new Point3D(2.56, CABLE_Y, egressLane));
            addCable(egress, material(PALE_CABLE, Color.web("#ffffff"), 18));
            egressRoutes.add(egress);
        }
    }

    private void addCable(List<Point3D> path, PhongMaterial cableMaterial) {
        for (int i = 1; i < path.size(); i++) {
            Point3D from = path.get(i - 1);
            Point3D to = path.get(i);
            Point3D direction = to.subtract(from);
            double length = direction.magnitude();
            if (length < 0.001) continue;
            Cylinder segment = new Cylinder(0.036, length);
            segment.setMaterial(cableMaterial);
            orientAlongY(segment, direction);
            segment.setTranslateX((from.getX() + to.getX()) / 2);
            segment.setTranslateY((from.getY() + to.getY()) / 2);
            segment.setTranslateZ((from.getZ() + to.getZ()) / 2);
            world.getChildren().add(segment);
            if (i < path.size() - 1) {
                Sphere joint = sphere(0.043, PALE_CABLE, 1.0);
                joint.setTranslateX(to.getX());
                joint.setTranslateY(to.getY());
                joint.setTranslateZ(to.getZ());
                world.getChildren().add(joint);
            }
        }
    }

    // ---------- animation ----------

    private void tick(long now) {
        double dt = lastFrame == 0 ? 0 : Math.min(0.05, (now - lastFrame) / 1_000_000_000.0);
        lastFrame = now;
        time += dt;

        // True top-down view: eye shares the target's x/z, so the plate stays square.
        Point3D target = new Point3D(0.15, 0.62, 0.35);
        Point3D eye = new Point3D(target.getX(), 24, target.getZ() + 0.01);
        camera.getTransforms().setAll(lookAt(eye, target));

        scanLine.setTranslateZ(-2.6 + ((time * 1.7) % 5.8));
        scanLine.setOpacity("running".equals(phase.get()) ? 0.50 : 0.12);

        if (progressValue >= 0) {
            double p = progressValue;
            progressFill.setScaleX(Math.max(0.001, p));
            progressFill.setTranslateX(-4.7 + 9.7 * p / 2);
            progressFill.setVisible(true);
        } else {
            progressFill.setVisible(false);
        }

        if ("running".equals(phase.get())) {
            elapsedMillis.set((long) ((time - runStartedAt) * 1000));
        }

        for (WorkerVisual worker : workerVisuals) {
            boolean active = time < worker.flashUntil;
            worker.capMaterial.setDiffuseColor(active ? AMBER : Color.web("#2b3037"));
            worker.stripMaterial.setSpecularColor(active ? Color.WHITE : Color.web("#ffffff"));
            if (!active) {
                worker.ledMaterial.setDiffuseColor(AMBER);
                worker.stripMaterial.setDiffuseColor(AMBER);
            }
        }
        if (activeWorker.get() >= 0 && time > activeUntil) activeWorker.set(-1);

        for (int i = packets.size() - 1; i >= 0; i--) {
            PacketVisual packet = packets.get(i);
            packet.t += dt * 0.42;
            if (packet.t >= 1) {
                world.getChildren().removeAll(packet.core, packet.glow);
                packets.remove(i);
                deposit(packet.tone);
                continue;
            }
            Point3D position = pointOnRoute(packet.route, packet.t);
            place(packet.core, position);
            place(packet.glow, position);
            packet.core.setRotate(time * 160);
        }
    }

    private void deposit(Color tone) {
        if (slots.isEmpty()) return;
        int index = Math.max(0, (nextSlot - 1) % slots.size());
        SlotVisual slot = slots.get(index);
        slot.card.setVisible(true);
        slot.stripe.setVisible(true);
        slot.stripe.setMaterial(material(tone, Color.WHITE, 60));
    }

    private static Point3D pointOnRoute(List<Point3D> route, double t) {
        double remaining = Math.max(0, Math.min(1, t)) * routeLength(route);
        for (int i = 1; i < route.size(); i++) {
            Point3D from = route.get(i - 1);
            Point3D to = route.get(i);
            double length = from.distance(to);
            if (remaining <= length || i == route.size() - 1) {
                double local = Math.min(1, Math.max(0, remaining / Math.max(length, 0.0001)));
                return from.add(to.subtract(from).multiply(local));
            }
            remaining -= length;
        }
        return route.get(route.size() - 1);
    }

    private static double routeLength(List<Point3D> route) {
        double total = 0;
        for (int i = 1; i < route.size(); i++) total += route.get(i - 1).distance(route.get(i));
        return total;
    }

    // ---------- helpers ----------

    private static Color colorOf(ScanOutcome outcome) {
        if (outcome instanceof ScanOutcome.Scored scored) {
            RiskLevel risk = scored.risk();
            return switch (risk) {
                case HIGH -> Color.web("#d0523c");
                case MEDIUM -> Color.web("#d9982f");
                case LOW -> Color.web("#7c9a70");
            };
        }
        return Color.web("#5f6975");
    }

    private static PhongMaterial material(Color diffuse, Color specular, double power) {
        PhongMaterial result = new PhongMaterial(diffuse);
        result.setSpecularColor(Color.web("#2b3037"));
        result.setSpecularPower(Math.min(power, 12));
        return result;
    }

    private Box box(double w, double h, double d, Color color,
                    double x, double y, double z, double opacity) {
        return addBox(w, h, d, material(color, Color.web("#ffffff"), 24), x, y, z, opacity);
    }

    private Box addBox(double w, double h, double d, PhongMaterial material,
                       double x, double y, double z, double opacity) {
        return addBox(world, w, h, d, material, x, y, z, opacity);
    }

    private Box addBox(Group parent, double w, double h, double d, PhongMaterial material,
                       double x, double y, double z, double opacity) {
        Box result = new Box(w, h, d);
        result.setMaterial(material);
        result.setTranslateX(x);
        result.setTranslateY(y);
        result.setTranslateZ(z);
        result.setOpacity(opacity);
        parent.getChildren().add(result);
        return result;
    }

    private static Sphere sphere(double radius, Color color, double opacity) {
        Sphere sphere = new Sphere(radius, 16);
        sphere.setMaterial(material(color, Color.WHITE, 90));
        sphere.setOpacity(opacity);
        return sphere;
    }

    private static void place(Sphere sphere, Point3D point) {
        sphere.setTranslateX(point.getX());
        sphere.setTranslateY(point.getY());
        sphere.setTranslateZ(point.getZ());
    }

    private static void orientAlongY(Cylinder cylinder, Point3D direction) {
        Point3D axis = direction.normalize();
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D rotationAxis = yAxis.crossProduct(axis);
        if (rotationAxis.magnitude() < 0.0001) {
            cylinder.setRotate(0);
            return;
        }
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, yAxis.dotProduct(axis)))));
        cylinder.setRotationAxis(rotationAxis.normalize());
        cylinder.setRotate(angle);
    }

    private static Affine lookAt(Point3D eye, Point3D target) {
        Point3D zAxis = target.subtract(eye).normalize();
        Point3D xAxis = new Point3D(0, 1, 0).crossProduct(zAxis).normalize();
        Point3D yAxis = zAxis.crossProduct(xAxis).normalize();
        return new Affine(
                xAxis.getX(), yAxis.getX(), zAxis.getX(), eye.getX(),
                xAxis.getY(), yAxis.getY(), zAxis.getY(), eye.getY(),
                xAxis.getZ(), yAxis.getZ(), zAxis.getZ(), eye.getZ());
    }

    private static final class WorkerVisual {
        private final Group root;
        private final double x;
        @SuppressWarnings("unused")
        private final double z;
        private final PhongMaterial capMaterial;
        private final PhongMaterial stripMaterial;
        private final PhongMaterial ledMaterial;
        private double flashUntil;

        private WorkerVisual(Group root, double x, double z, PhongMaterial capMaterial,
                             PhongMaterial stripMaterial, PhongMaterial ledMaterial) {
            this.root = root;
            this.x = x;
            this.z = z;
            this.capMaterial = capMaterial;
            this.stripMaterial = stripMaterial;
            this.ledMaterial = ledMaterial;
        }
    }

    private static final class SlotVisual {
        private final Box card;
        private final Box stripe;

        private SlotVisual(Box card, Box stripe) {
            this.card = card;
            this.stripe = stripe;
        }
    }

    private static final class PacketVisual {
        private final Sphere core;
        private final Sphere glow;
        private final List<Point3D> route;
        private final Color tone;
        private double t;

        private PacketVisual(Sphere core, Sphere glow, List<Point3D> route, Color tone) {
            this.core = core;
            this.glow = glow;
            this.route = route;
            this.tone = tone;
        }
    }
}
