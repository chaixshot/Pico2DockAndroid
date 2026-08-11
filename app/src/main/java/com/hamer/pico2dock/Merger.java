package com.hamer.pico2dock;

import com.reandroid.apkeditor.merge.MergerOptions;
import java.util.function.Supplier;

public class Merger extends com.reandroid.apkeditor.merge.Merger {
    String apkName;
    Supplier<Boolean> cancellationCheck;

    public Merger(MergerOptions options, String name, Supplier<Boolean> cancellationCheck) {
        super(options);
        this.apkName = name;
        this.cancellationCheck = cancellationCheck;
    }

    private void updateStatus(String msg) {
        if (cancellationCheck.get()) {
            throw new RuntimeException("Operation cancelled by user");
        }
        ProgressManager.getInstance().postUpdate(new ProcessUpdate(
            "## Merger\nMerging multiple split **" + apkName + "**...\n\n``" + msg + "``",
            -1, null, false, false
        ));
    }

    @Override
    public void logMessage(String msg) {
        super.logMessage(msg);
        updateStatus(msg);
    }

    @Override
    public void logMessage(String tag, String msg) {
        super.logMessage(tag, msg);
        updateStatus(msg);
    }

    @Override
    public void logVerbose(String msg) {
        super.logVerbose(msg);
        updateStatus(msg);
    }

    @Override
    public void logVerbose(String tag, String msg) {
        super.logVerbose(tag, msg);
        updateStatus(msg);
    }

    @Override
    public void logError(String msg, Throwable tr) {
        super.logError(msg, tr);
        updateStatus(msg);
    }

    @Override
    public void logWarn(String msg) {
        super.logWarn(msg);
        updateStatus(msg);
    }
}
