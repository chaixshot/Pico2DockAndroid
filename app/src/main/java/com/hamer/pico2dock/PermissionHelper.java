package com.hamer.pico2dock;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHelper {
    static CardView WarningCard;

    public static void CheckPermission(Activity context) {
        WarningCard = (CardView) context.findViewById(R.id.CardPermissionWarning);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            PermissionsGranted();
        }
    }

    public static void PermissionsGranted() {
        WarningCard.setVisibility(CardView.GONE);
    }
}
