package com.hamer.pico2dock;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

import brut.androlib.ApkDecoder;
import brut.androlib.Config;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void SelectFile(View view) {
        Intent intent = new Intent()
                .setType("application/vnd.android.package-archive")
                .setAction(Intent.ACTION_GET_CONTENT)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        startActivityForResult(Intent.createChooser(intent, "Select a file"), 111);
    }

    //? After file selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 111 && resultCode == RESULT_OK) {
            if (data != null) {
                // Checking for selection multiple files or single.
                if (data.getClipData() != null) {
//                    int itemCount = data.getClipData().getItemCount();
//                    for (int index = 0; index < itemCount; index++) {
//                        Uri uri = data.getClipData().getItemAt(index).getUri();
//                        Log.d("filesUri [" + uri + "] : ", uri.toString());
//                    }
                } else {
                    String filePath = "";
                    Uri dataUri = data.getData();
                    if (dataUri != null)
                        filePath = dataUri.getPath().replace("/document/raw:/", "");
                    MainTask(filePath);
                }
            }
        }
    }

    public void MainTask(String filePath){
        File apkFile = new File(filePath);
        File outFolder = new File("storage/emulated/0/Pico2Dock");

        Boolean isExist = apkFile.exists();
        Boolean isFile = apkFile.isFile();
        Boolean isReadable = apkFile.canRead();

        Config config = new Config("3.0.1");
        config.mForced = true;

        if (isExist && isFile && isReadable) {
            new ApkDecoder(apkFile, config).decode(outFolder);
        }
    }
}