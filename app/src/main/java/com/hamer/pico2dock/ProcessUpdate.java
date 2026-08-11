package com.hamer.pico2dock;

public class ProcessUpdate {
    public final String statusText;
    public final int progress;
    public final String errorMessage;
    public final boolean isFinished;
    public final boolean isCancelled;

    public ProcessUpdate(String statusText, int progress, String errorMessage, boolean isFinished, boolean isCancelled) {
        this.statusText = statusText;
        this.progress = progress;
        this.errorMessage = errorMessage;
        this.isFinished = isFinished;
        this.isCancelled = isCancelled;
    }

    public static ProcessUpdate progress(String statusText, int progress) {
        return new ProcessUpdate(statusText, progress, null, false, false);
    }

    public static ProcessUpdate error(String errorMessage) {
        return new ProcessUpdate(null, 100, errorMessage, true, false);
    }

    public static ProcessUpdate success() {
        return new ProcessUpdate(null, 100, null, true, false);
    }

    public static ProcessUpdate cancelled() {
        return new ProcessUpdate(null, 100, null, true, true);
    }
}
