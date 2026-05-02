package com.hamer.pico2dock;

import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileviewHelper {
    static MainActivity mainActivity = MainActivity.getInstance();

    public static void Apply(String[] files) {
        ListView fileView = (ListView) mainActivity.findViewById(R.id.ListViewFiles);

        mainActivity.registerForContextMenu(fileView);

        ListAdapter myAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_list_item_activated_1, mainActivity.APKFiles);
        fileView.setAdapter(myAdapter);
        fileView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    public static void Select(Integer index) {
        ListView fileView = (ListView) mainActivity.findViewById(R.id.ListViewFiles);

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fileView.setItemChecked(index, true);
            }
        });
    }

    public static void ChangeText(Integer index, String text) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.APKFiles[index] = text;
                Apply(mainActivity.APKFiles);
            }
        });
    }

    public static void ClearAllTag() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Integer index = 0;
                for (String path : mainActivity.APKFiles) {
                    String newPath = path.replaceAll("(" + Utils.FileIndicator.Working + "|" + Utils.FileIndicator.Success + ")\\s", "");

                    mainActivity.APKFiles[index] = newPath;

                    index++;
                }

                Apply(mainActivity.APKFiles);
            }
        });
    }

    public static void ClearTag(Integer index) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.APKFiles[index] = mainActivity.APKFiles[index].replaceAll("(" + Utils.FileIndicator.Working + "|" + Utils.FileIndicator.Success + ")\\s", "");
                Apply(mainActivity.APKFiles);
            }
        });
    }

    public static void RemoveByIndex(int index) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<String> _listAPKFiles = new ArrayList<String>(Arrays.asList(mainActivity.APKFiles));
                _listAPKFiles.remove(index);
                mainActivity.APKFiles = _listAPKFiles.toArray(new String[0]);

                List<String> _APKFilesOut = new ArrayList<String>(Arrays.asList(mainActivity.APKFilesOut));
                _APKFilesOut.remove(index);
                mainActivity.APKFilesOut = _APKFilesOut.toArray(new String[0]);

                Apply(mainActivity.APKFiles);
            }
        });
    }
}
