package com.threatdetector.ui;

/**
 * Workspace modes of the dashboard shell.
 *
 * INSPECT and BATCH are dashboard "scan" sub-modes (single input vs CSV batch) and both
 * keep the Dashboard navigation item active. HISTORY and METHOD (Settings) are separate
 * full-page views.
 */
public enum Mode {
    INSPECT,
    BATCH,
    HISTORY,
    METHOD
}
