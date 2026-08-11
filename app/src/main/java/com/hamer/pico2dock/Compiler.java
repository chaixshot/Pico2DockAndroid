package com.hamer.pico2dock;

import com.reandroid.apkeditor.compile.BuildOptions;
import java.util.function.Supplier;

public class Compiler extends com.reandroid.apkeditor.compile.Builder {
    String apkName;
    Supplier<Boolean> cancellationCheck;

    public Compiler(BuildOptions options, String name, Supplier<Boolean> cancellationCheck) {
        super(options);
        this.apkName = name;
        this.cancellationCheck = cancellationCheck;
    }

    private void updateStatus(String msg) {
        if (!cancellationCheck.get()) {
            ProgressManager.getInstance().postUpdate(new ProcessUpdate(
                "## Encoder\nBuilding **" + apkName + "**...\n\n``" + msg + "``",
                -1, null, false, false
            ));
        }
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
