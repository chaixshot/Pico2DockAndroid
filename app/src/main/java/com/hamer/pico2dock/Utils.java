package com.hamer.pico2dock;

import static android.view.View.VISIBLE;

import android.content.res.Resources;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class Utils {
    static MainActivity mainActivity = MainActivity.getInstance();

    public static File GetKeystoreFile() {
        File keystore;

        Resources resources = mainActivity.getResources();
        try {
            // Open the audio file from the raw folder
            InputStream inputStream = resources.openRawResource(R.raw.keystore);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            // Create a new File Object
            keystore = new File(mainActivity.getExternalFilesDir(null), "keystore.jks");
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

    public static void CleanupTempDir() {
        CleanupDir("storage/emulated/0/Pico2Dock/Worker");
        CleanupDir("storage/emulated/0/Pico2Dock/Unsign");
        CleanupDir("storage/emulated/0/Pico2Dock/Apkm");
        CleanupDir("storage/emulated/0/Pico2Dock/Zipper");
        CleanupDir("storage/emulated/0/Pico2Dock/Merger");
    }

    public static String generateString(int length) {
        final String valid = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder res = new StringBuilder();
        Random rnd = new Random();
        while (length-- > 0) {
            res.append(valid.charAt(rnd.nextInt(valid.length())));
        }
        return res.toString();
    }

    public static class ProgressBar {
        public Double Files;
        public Double Step;

        public ProgressBar(double files, double step) {
            this.Files = files;
            this.Step = step;
        }

        public void Increase(@Nullable Integer mul) {
            if (mul == null)
                mul = 1;

            Integer finalMul = mul;
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.StatusProgressBar.setVisibility(VISIBLE);
                    mainActivity.StatusProgressBar.incrementProgressBy((int) Math.round(((100 / Step) * finalMul) / Files));
                    mainActivity.PercentText.setText(mainActivity.StatusProgressBar.getProgress() + "%");
                }
            });
        }
    }
}
