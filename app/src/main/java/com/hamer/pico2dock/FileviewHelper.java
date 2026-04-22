package com.hamer.pico2dock;

import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class FileviewHelper {
    static MainActivity mainActivity = MainActivity.getInstance();

    public static void FileviewApply(String[] files) {
        ListView fileView = (ListView) mainActivity.findViewById(R.id.ListViewFiles);

        mainActivity.registerForContextMenu(fileView);

        ListAdapter myAdapter = new ArrayAdapter<String>(mainActivity, android.R.layout.simple_list_item_activated_1, mainActivity.APKFiles);
        fileView.setAdapter(myAdapter);
        fileView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    public static void FileviewSelect(Integer index) {
        ListView fileView = (ListView) mainActivity.findViewById(R.id.ListViewFiles);

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fileView.setItemChecked(index, true);
            }
        });
    }

    public static void FileviewChangeText(Integer index, String text) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.APKFiles[index] = text;
                FileviewApply(mainActivity.APKFiles);
            }
        });
    }

    public static void FileviewClearTag() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Integer index = 0;
                for (String path : mainActivity.APKFiles) {
                    String newPath = path.replace("🛠️ ", "").replace("✅ ", "");

                    mainActivity.APKFiles[index] = newPath;

                    index++;
                }

                FileviewApply(mainActivity.APKFiles);
            }
        });
    }
}
