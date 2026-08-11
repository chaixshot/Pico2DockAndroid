package com.hamer.pico2dock;

import com.reandroid.apkeditor.decompile.DecompileOptions;
import java.util.function.Supplier;

public class Decompiler extends com.reandroid.apkeditor.decompile.Decompiler {
    String apkName;
    Supplier<Boolean> cancellationCheck;

    public Decompiler(DecompileOptions options, String name, Supplier<Boolean> cancellationCheck) {
        super(options);
        this.apkName = name;
        this.cancellationCheck = cancellationCheck;
    }

    private void updateStatus(String msg) {
        if (!cancellationCheck.get()) {
            ProgressManager.getInstance().postUpdate(new ProcessUpdate(
                "## Decoder\nDecompiling **" + apkName + "**...\n\n``" + msg + "``",
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
