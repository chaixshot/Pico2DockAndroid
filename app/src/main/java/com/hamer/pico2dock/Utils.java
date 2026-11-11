package com.hamer.pico2dock;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

public class Utils {

    /**
     * Optimizes file copying using NIO Channels (Zero-Copy)
     */
    public static void FastCopy(File source, File dest) throws IOException {
        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

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
            if (children != null) {
                for (String child : children) {
                    CleanupDir(path + "/" + child);
                }
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
        CleanupDir("storage/emulated/0/Pico2Dock");
    }

    public static void CleanupWorkerDir() {
        CleanupDir("storage/emulated/0/Pico2Dock/Worker");
        CleanupDir("storage/emulated/0/Pico2Dock/Zipper");
        CleanupDir("storage/emulated/0/Pico2Dock/Merger");
    }

    public static void PlayAlertSound(Context context) {
        try {
            // Using STREAM_MUSIC at max volume (100)
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            // TONE_DTMF_D is a high-frequency loud tone, playing for 1.5 seconds
            tg.startTone(ToneGenerator.TONE_DTMF_D, 1500);
        } catch (Exception e) {
            Log.e("Pico2Dock", "Error playing ToneGenerator sound", e);
        }
    }

    public static class ProgressBar {
        public double Files;
        public double Step;

        public ProgressBar(double files, double step) {
            this.Files = files;
            this.Step = step;
        }

        public void Increase(@Nullable Integer mul) {
            if (mul == null)
                mul = 1;

            int increment = (int) Math.round(((100 / Step) * mul) / Files);
            ProgressManager.getInstance().incrementProgress(increment);
        }
    } 

    public static class FileIndicator {
        public static  String Working = "🛠️";
        public static String Success = "✅";
        public static String Error = "❌";
        public static String ErrorInfo = "⭕";

        public static String ClearTag(String text) {
            return text.replaceAll("(" + Utils.FileIndicator.Working + "|" + Utils.FileIndicator.Success + ")\\s", "");
        }
    }

    public static void triggerHaptic(Context context, View view, boolean isHover, int deviceId) {
        float amplitude = isHover ? 120f : 255f;
        float frequency = isHover ? 177f : 100f;
        float duration = isHover ? 10f : 35f;

        int nativeDeviceId;
        if (deviceId == 100001 || deviceId == 1) {
            nativeDeviceId = 1;
        } else if (deviceId == 100002 || deviceId == 2) {
            nativeDeviceId = 2;
        } else {
            nativeDeviceId = 2;
        }

        try {
            if (com.bytedance.osui.utils.OSUIHelper.isLoaded) {
                int res = com.bytedance.osui.utils.OSUIHelper.a(nativeDeviceId, amplitude, frequency, duration);
                Log.d("PicoHaptic", "OSUIHelper.a result for nativeDeviceId " + nativeDeviceId + " (devId " + deviceId + "): " + res);
            } else {
                Log.w("PicoHaptic", "OSUIHelper is not loaded");
            }
        } catch (Throwable t) {
            Log.e("PicoHaptic", "Error calling OSUIHelper.a", t);
        }
    }

    public static void applyHaptic(View view) {
        if (view == null) return;
        view.setOnHoverListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_HOVER_ENTER) {
                triggerHaptic(v.getContext(), v, true, event.getDeviceId());
            }
            return false;
        });
    }

    public static void applyHapticRecursive(View view) {
        if (view == null) return;
        applyHaptic(view);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyHapticRecursive(vg.getChildAt(i));
            }
        }
    }
}
