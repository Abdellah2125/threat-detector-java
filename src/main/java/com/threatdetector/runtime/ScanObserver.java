package com.threatdetector.runtime;

import com.threatdetector.model.ItemResult;
import com.threatdetector.model.ScanEvents;
import com.threatdetector.model.Summary;

/** Telemetry sink implemented by the UI. All callbacks may run on worker threads. */
public interface ScanObserver {
    ScanObserver NO_OP = new ScanObserver() {
    };

    default void onStarted(ScanEvents.ScanStartedEvent event) {
    }

    default void onItem(ScanEvents.ScanItemEvent event) {
    }

    default void onFinished(ScanEvents.ScanFinishedEvent event) {
    }

    default void onError(ItemResult result) {
    }
}
