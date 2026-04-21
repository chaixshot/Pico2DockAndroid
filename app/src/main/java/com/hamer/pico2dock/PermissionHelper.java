package com.hamer.pico2dock;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Button;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    static MainActivity mainActivity = MainActivity.getInstance();
    static Runnable callback;

    public static void CheckPermission(Runnable cb) {
        callback = cb;
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mainActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 112);
        } else {
            callback.run();
        }
    }

    public static void PermissionsGranted() {
        callback.run();
    }
}
