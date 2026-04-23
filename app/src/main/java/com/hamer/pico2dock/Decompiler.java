package com.hamer.pico2dock;

import android.app.Activity;
import android.content.Context;

import com.reandroid.apkeditor.Main;
import com.reandroid.apkeditor.decompile.DecompileOptions;

public class Decompiler extends com.reandroid.apkeditor.decompile.Decompiler {
    String apkName;
    static MainActivity mainActivity = MainActivity.getInstance();

    public Decompiler(DecompileOptions options, String name) {
        super(options);

        apkName = name;
    }

    @Override
    public void logMessage(String msg) {
        super.logMessage(msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nDecompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logMessage(String tag, String msg) {
        super.logMessage(tag, msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nDecompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logVerbose(String msg) {
        super.logVerbose(msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nDecompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logVerbose(String tag, String msg) {
        super.logVerbose(tag, msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nDecompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logError(String msg, Throwable tr) {
        super.logError(msg, tr);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nDecompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logWarn(String msg) {
        super.logWarn(msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nDecompiling **" + apkName + "**...\n\n``" + msg + "``");
    }
}
