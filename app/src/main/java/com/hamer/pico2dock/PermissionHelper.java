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
    static CardView WarningCard = (CardView) mainActivity.findViewById(R.id.CardPermissionWarning);
    static Button ButtonStart = (Button) mainActivity.findViewById(R.id.ButtonStart);

    public static void CheckPermission() {
        if (ContextCompat.checkSelfPermission(mainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            WarningCard.setVisibility(CardView.VISIBLE);

            ActivityCompat.requestPermissions(mainActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            PermissionsGranted();
        }
    }

    public static void PermissionsGranted() {
        WarningCard.setVisibility(CardView.GONE);
        ButtonStart.setEnabled(true);
    }
}
