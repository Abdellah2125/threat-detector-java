package com.threatdetector.ui;

import com.threatdetector.model.ItemResult;
import com.threatdetector.model.ScanReport;

/** Lets a scan panel notify the shell without owning the shell's widgets. */
public interface ScanUiBridge {
    void onStarted(int total, int workers);

    void onItem(ItemResult result, int completed, int total);

    void onFinished(ScanReport report, boolean single, String explanation);

    void onFailed(String message);

    void onReset();
}
