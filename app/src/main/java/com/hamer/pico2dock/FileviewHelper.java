package com.hamer.pico2dock;

import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileviewHelper {
    static MainActivity mainActivity = MainActivity.getInstance();
    static ListView fileView = (ListView) mainActivity.findViewById(R.id.ListViewFiles);

    public static void Apply() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.registerForContextMenu(fileView);

                ListAdapter myAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_list_item_activated_1, mainActivity.APKFiles);
                fileView.setAdapter(myAdapter);
                fileView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            }
        });
    }

    public static void Select(Integer index) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fileView.setItemChecked(index, true);
                fileView.smoothScrollToPosition(index + 1);
            }
        });
    }

    public static void ChangeText(Integer index, String text) {
        mainActivity.APKFiles[index] = text;

        Apply();
    }

    public static void ClearAllTag() {
        Integer index = 0;

        for (String path : mainActivity.APKFiles) {
            mainActivity.APKFiles[index] = path.replaceAll("(" + Utils.FileIndicator.Working + "|" + Utils.FileIndicator.Success + ")\\s", "");

            index++;
        }

        Apply();
    }

    public static void ClearTag(Integer index) {
        mainActivity.APKFiles[index] = mainActivity.APKFiles[index].replaceAll("(" + Utils.FileIndicator.Working + "|" + Utils.FileIndicator.Success + ")\\s", "");

        Apply();
    }

    public static void RemoveByIndex(int index) {
        List<String> _listAPKFiles = new ArrayList<String>(Arrays.asList(mainActivity.APKFiles));
        _listAPKFiles.remove(index);
        mainActivity.APKFiles = _listAPKFiles.toArray(new String[0]);

        List<String> _APKFilesOut = new ArrayList<String>(Arrays.asList(mainActivity.APKFilesOut));
        _APKFilesOut.remove(index);
        mainActivity.APKFilesOut = _APKFilesOut.toArray(new String[0]);

        Apply();
    }
}
