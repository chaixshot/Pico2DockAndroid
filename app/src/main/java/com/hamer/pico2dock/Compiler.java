package com.hamer.pico2dock;

import com.reandroid.apkeditor.compile.BuildOptions;

public class Compiler extends com.reandroid.apkeditor.compile.Builder {
    String apkName;
    static MainActivity mainActivity = MainActivity.getInstance();

    public Compiler(BuildOptions options, String name) {
        super(options);

        apkName = name;
    }

    @Override
    public void logMessage(String msg) {
        super.logMessage(msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nCompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logMessage(String tag, String msg) {
        super.logMessage(tag, msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nCompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logVerbose(String msg) {
        super.logVerbose(msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nCompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logVerbose(String tag, String msg) {
        super.logVerbose(tag, msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nCompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logError(String msg, Throwable tr) {
        super.logError(msg, tr);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Status\nCompiling **" + apkName + "**...\n\n``" + msg + "``");
    }

    @Override
    public void logWarn(String msg) {
        super.logWarn(msg);

        if (mainActivity.findViewById(R.id.ButtonCancel).isEnabled())
            mainActivity.ChangeStateText("## Current Statu\nCompiling **" + apkName + "**...\n\n``" + msg + "``");
    }
}
