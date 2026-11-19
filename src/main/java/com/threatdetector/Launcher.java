package com.threatdetector;

import com.threatdetector.ui.ThreatDetectorApp;
import javafx.application.Application;

/**
 * Plain main class: launching JavaFX from an Application subclass with
 * "java -jar" fails because the runtime modules are not on the module path.
 * Delegating through a normal class makes the shaded jar work directly.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(ThreatDetectorApp.class, args);
    }
}
