package com.hamer.pico2dock;

import android.content.Context;
import android.content.res.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static File GetKeystoreFile(Context context) {
        File keystore;

        Resources resources = context.getResources();
        try {
            // Open the audio file from the raw folder
            InputStream inputStream = resources.openRawResource(R.raw.keystore);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            // Create a new File Object
            keystore = new File(context.getExternalFilesDir(null), "keystore.jks");
            FileOutputStream outputStream = new FileOutputStream(keystore);
            outputStream.write(bytes);
            outputStream.close();

            return keystore;
        } catch (IOException e) {
//            e.printStackTrace();
            return null;
        }
    }

    public static void CleanupDir(String path) {
        File file = new File(path);

        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                CleanupDir(path + "/" + child);
            }
        }
        file.delete();
    }

    public static void CleanupTempDir(){
        CleanupDir("storage/emulated/0/Pico2Dock/Worker");
        CleanupDir("storage/emulated/0/Pico2Dock/Unsign");
    }
}
