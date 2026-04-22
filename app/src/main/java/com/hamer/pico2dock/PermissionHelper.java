package com.hamer.pico2dock;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    static MainActivity mainActivity = MainActivity.getInstance();
    static Runnable callback;

    public static void CheckWritePermission(Runnable cb) {
        callback = cb;

        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 112);
        } else {
            callback.run();
        }
    }

    public static void WritePermissionGranted() {
        callback.run();
    }

    public static void AskInstallPermission() throws Settings.SettingNotFoundException {
        if (Settings.Secure.getInt(mainActivity.getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) != 1) {
            final Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);

            intent.setData(Uri.parse("package:" + mainActivity.getPackageName()));
            mainActivity.startActivity(intent);
        }
    }
}
