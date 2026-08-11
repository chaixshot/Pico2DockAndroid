package com.hamer.pico2dock;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileviewHelper {

    public static void Apply(Activity activity, String[] apkFiles) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListView fileView = (ListView) activity.findViewById(R.id.ListViewFiles);
                activity.registerForContextMenu(fileView);

                ListAdapter myAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_activated_1, apkFiles);
                fileView.setAdapter(myAdapter);
                fileView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            }
        });
    }

    public static void Select(Activity activity, Integer index) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ListView fileView = (ListView) activity.findViewById(R.id.ListViewFiles);
                fileView.setItemChecked(index, true);
                fileView.smoothScrollToPosition(index + 1);
            }
        });
    }

    public static void ChangeText(Integer index, String text) {
        ProgressManager.getInstance().updateFileStatus(index, text);
    }

    public static void ClearAllTag() {
        String[] apkFiles = ProgressManager.getInstance().getApkFiles();
        if (apkFiles == null) return;

        for (int i = 0; i < apkFiles.length; i++) {
            apkFiles[i] = apkFiles[i].replaceAll("(" + Utils.FileIndicator.Working + "|" + Utils.FileIndicator.Success + ")\\s", "");
        }

        ProgressManager.getInstance().setApkFiles(apkFiles);
    }

    public static void ClearTag(Integer index) {
        String[] apkFiles = ProgressManager.getInstance().getApkFiles();
        if (apkFiles == null || index < 0 || index >= apkFiles.length) return;

        apkFiles[index] = apkFiles[index].replaceAll("(" + Utils.FileIndicator.Working + "|" + Utils.FileIndicator.Success + ")\\s", "");

        ProgressManager.getInstance().setApkFiles(apkFiles);
    }

    public static void RemoveByIndex(int index) {
        String[] apkFiles = ProgressManager.getInstance().getApkFiles();
        if (apkFiles == null || index < 0 || index >= apkFiles.length) return;

        List<String> _listAPKFiles = new ArrayList<String>(Arrays.asList(apkFiles));
        _listAPKFiles.remove(index);
        ProgressManager.getInstance().setApkFiles(_listAPKFiles.toArray(new String[0]));
        
        // Note: MainActivity.APKFilesOut also needs to be synced if we were using it there, 
        // but for now we focus on the UI/ProgressManager integration.
    }
}
