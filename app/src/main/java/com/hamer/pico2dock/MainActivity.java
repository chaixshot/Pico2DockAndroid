package com.hamer.pico2dock;

import static android.view.View.VISIBLE;

import static androidx.core.content.FileProvider.getUriForFile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.android.apksigner.ApkSignerTool;
import com.developer.filepicker.controller.DialogSelectionListener;
import com.developer.filepicker.model.DialogConfigs;
import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.reandroid.apkeditor.compile.BuildOptions;
import com.reandroid.apkeditor.decompile.DecompileOptions;
import com.reandroid.apkeditor.merge.MergerOptions;
import com.reandroid.archive.ZipAlign;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import io.noties.markwon.Markwon;

public class MainActivity extends AppCompatActivity {
    String[] APKFiles = new String[]{};
    String[] APKFilesOut = new String[]{};
    File keystore;

    Button ButtonStart;
    Button ButtonCancel;
    Button ButtonClear;
    Button ButtonBattery;
    TextView TextViewSelectHint;
    Switch SwtichHideDock;
    CheckBox CheckboxRePackage;
    CheckBox CheckboxRePackageAdv;
    ProgressBar StatusProgressBar;
    TextView PercentText;
    EditText TextRename;
    CheckBox CheckboxRename;

    boolean IsHideDock = false;
    boolean IsRePackage = false;
    boolean IsRePackageAdv = false;
    String NamePrefix = "";
    boolean IsRename = false;

    boolean IsProcessRunning = false;
    Long DoubleBack = System.currentTimeMillis() - 2000;
    private static MainActivity instance;

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

        instance = this;

        keystore = Utils.GetKeystoreFile(this);

        ButtonStart = (Button) findViewById(R.id.ButtonStart);
        ButtonCancel = (Button) findViewById(R.id.ButtonCancel);
        ButtonClear = (Button) findViewById(R.id.ButtonClear);
        TextViewSelectHint = (TextView) findViewById(R.id.TextFileSelectHint);
        SwtichHideDock = (Switch) findViewById(R.id.SwitchHideDock);
        CheckboxRePackage = (CheckBox) findViewById(R.id.CheckboxRePackage);
        CheckboxRePackageAdv = (CheckBox) findViewById(R.id.CheckboxRePackageAdv);
        StatusProgressBar = (ProgressBar) findViewById(R.id.StatusProgressBar);
        PercentText = (TextView) findViewById(R.id.PercentText);
        TextRename = (EditText) findViewById(R.id.TextRename);
        CheckboxRename = (CheckBox) findViewById(R.id.CheckboxRename);
        ButtonBattery = (Button) findViewById(R.id.ButtonBattery);

        ResetAppearance();
        ChangeButtonState();

        setupObservers();
    }

    private void setupObservers() {
        ProgressManager.getInstance().getUpdateLiveData().observe(this, new Observer<ProcessUpdate>() {
            @Override
            public void onChanged(ProcessUpdate update) {
                if (update.statusText != null) {
                    ChangeStateText(update.statusText);
                }
                if (update.progress != -1) {
                    StatusProgressBar.setVisibility(VISIBLE);
                    StatusProgressBar.setProgress(update.progress);
                    PercentText.setText(update.progress + "%");
                }
                if (update.errorMessage != null) {
                    StatusProgressBar.setVisibility(VISIBLE);
                    PercentText.setText("Error");
                    ChangeStateText("## ERROR\n\n" + update.errorMessage);
                }
                if (update.isFinished) {
                    IsProcessRunning = false;
                    ChangeButtonState();
                    if (update.isCancelled) {
                        StatusProgressBar.setVisibility(VISIBLE);
                        PercentText.setText("Terminated");
                        ChangeStateText("## Current Status\nProcess has been terminated.");
                    } else if (update.errorMessage == null) {
                        StatusProgressBar.setVisibility(VISIBLE);
                        PercentText.setText("Successful");
                        ChangeStateText("## Current Status\nAll files have been modified.\n* The APK files are in the Pico folder by the same directory as the original file.\n* Long click file in the box above to see the options.");
                    }
                }
            }
        });

        ProgressManager.getInstance().getFileListLiveData().observe(this, new Observer<String[]>() {
            @Override
            public void onChanged(String[] files) {
                APKFiles = files;
                FileviewHelper.Apply(MainActivity.this, files);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ChangeButtonState();
    }

    @Override
    public void onBackPressed() {
        if (DoubleBack + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            DoubleBack = System.currentTimeMillis();
            Toast.makeText(this, "Press once again to Exit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static MainActivity getInstance() {
        return instance;
    }

    public void SelectFile(View view) {
        PermissionHelper.CheckWritePermission(() -> {
            DialogProperties properties = new DialogProperties();

            properties.selection_mode = DialogConfigs.MULTI_MODE;
            properties.selection_type = DialogConfigs.FILE_SELECT;
            properties.root = new File(DialogConfigs.DEFAULT_DIR);
            properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
            properties.offset = new File(DialogConfigs.DEFAULT_DIR);
            properties.extensions = new String[]{"apk", "xapk", "apkm", "apks"};
            properties.show_hidden_files = false;

            FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
            dialog.setTitle("Select apk files");

            dialog.setDialogSelectionListener(new DialogSelectionListener() {
                @Override
                public void onSelectedFilePaths(String[] files) {
                    if (files.length > 0) {
                        APKFiles = files.clone();
                        APKFilesOut = files.clone();

                        ProgressManager.getInstance().setApkFiles(APKFiles);
                        ChangeButtonState();
                    }
                }
            });

            dialog.show();
        });
    }

    public void ButtonStartPressed(View view) {
        IsHideDock = SwtichHideDock.isChecked();
        IsRePackage = CheckboxRePackage.isChecked();
        IsRePackageAdv = CheckboxRePackageAdv.isChecked();
        NamePrefix = TextRename.getText().toString();
        IsRename = CheckboxRename.isChecked();
        IsProcessRunning = true;

        FileviewHelper.ClearAllTag();
        ResetAppearance();
        StatusProgressBar.setVisibility(VISIBLE);
        PercentText.setText("0%");
        ChangeButtonState();

        Data inputData = new Data.Builder()
                .putStringArray("APK_FILES", APKFiles)
                .putBoolean("IS_HIDE_DOCK", IsHideDock)
                .putBoolean("IS_REPACKAGE", IsRePackage)
                .putBoolean("IS_REPACKAGE_ADV", IsRePackageAdv)
                .putString("NAME_PREFIX", NamePrefix)
                .putBoolean("IS_RENAME", IsRename)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MainWorker.class)
                .addTag("MainWorkerTask")
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);
    }

    public void ButtonClearPressed(View view) {
        APKFiles = new String[]{};
        APKFilesOut = new String[]{};

        ProgressManager.getInstance().setApkFiles(APKFiles);

        ChangeButtonState();
    }

    public void ButtonCancelPressed(View view) {
        ChangeStateText("## Current Status\nCanceling process please wait...");
        WorkManager.getInstance(this).cancelAllWorkByTag("MainWorkerTask");
        view.setEnabled(false);
    }

    public void RequestBatteryOptimization(View view) {
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Battery optimization is already disabled.", Toast.LENGTH_SHORT).show();
        }
    }



    //** UI
    private void ChangeButtonState() {
        if ((APKFiles != null && APKFiles.length > 0) && !IsProcessRunning)
            ButtonStart.setEnabled(true);
        else
            ButtonStart.setEnabled(false);

        if (IsProcessRunning && !ButtonStart.isEnabled())
            ButtonCancel.setEnabled(true);
        else
            ButtonCancel.setEnabled(false);

        if ((APKFiles != null && APKFiles.length > 0) && !ButtonCancel.isEnabled())
            ButtonClear.setEnabled(true);
        else
            ButtonClear.setEnabled(false);

        if (APKFiles != null && APKFiles.length > 0)
            TextViewSelectHint.setVisibility(View.GONE);
        else
            TextViewSelectHint.setVisibility(VISIBLE);

        SwtichHideDock.setEnabled(!IsProcessRunning);
        CheckboxRePackage.setEnabled(!IsProcessRunning);
        CheckboxRePackageAdv.setEnabled(!IsProcessRunning);
        TextRename.setEnabled(!IsProcessRunning);
        CheckboxRename.setEnabled(!IsProcessRunning);

        if (ButtonBattery != null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
                ButtonBattery.setVisibility(View.GONE);
            } else {
                ButtonBattery.setVisibility(View.VISIBLE);
            }
        }
    }

    public void ChangeStateText(String text) {
        TextView statusText = (TextView) findViewById(R.id.StatusText);
        final Markwon markwon = Markwon.create(this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                markwon.setMarkdown(statusText, text);
            }
        });
    }

    public void OpenGithubPage(View view) {
        Uri uri = Uri.parse("https://github.com/chaixshot/Pico2DockAndroid");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    private void ResetAppearance() {
        StatusProgressBar.setProgress(0);
        StatusProgressBar.setVisibility(View.INVISIBLE);
        PercentText.setText("");
    }

    //** Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 112) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                PermissionHelper.WritePermissionGranted();
        }
    }

    //** Context menu
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add("Install");
        menu.add("Remove");
        menu.add("Delete");
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        assert info != null;
        Context _this = this;

        String apkPath = APKFiles[info.position];
        String apkOutPath = APKFilesOut[info.position];

        File apkFile = new File(Utils.FileIndicator.ClearTag(apkPath));
        File apkOutFile = new File(apkOutPath);

        Boolean isConverted = apkPath.contains(Utils.FileIndicator.Success) && apkOutFile.exists();
        File apkTargetFile = isConverted ? apkOutFile : apkFile;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (IsProcessRunning) {
            builder.setTitle("");
            builder.setMessage("Can't do this action while processing");

            builder.setPositiveButton("Close", (dialog, which) -> {
                dialog.dismiss();
            });
        } else {
            //?? Install
            if (item.getTitle() == "Install") {

                builder.setTitle("Do you want to install?");
                builder.setMessage(apkTargetFile.getPath());

                builder.setPositiveButton("YES", (dialog, which) -> {
                    try {
                        PermissionHelper.AskInstallPermission();

                        // Create Uri
                        Uri apkUri = getUriForFile(_this, getPackageName(), apkTargetFile);

                        // Intent to open apk
                        Intent intent = new Intent(Intent.ACTION_VIEW, apkUri);
                        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Settings.SettingNotFoundException e) {
                        ChangeStateText("## ERROR\n\n" + e);
                    }

                    dialog.dismiss();
                }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            }

            //?? Remove
            if (item.getTitle() == "Remove") {
                builder.setTitle("Do you want to remove?");
                builder.setMessage(apkTargetFile.getPath());

                builder.setPositiveButton("YES", (dialog, which) -> {
                    FileviewHelper.RemoveByIndex(info.position);

                    ChangeButtonState();
                    dialog.dismiss();
                }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            }

            //?? Delete
            if (item.getTitle() == "Delete") {
                builder.setTitle("Do you want to delete?");
                builder.setMessage(apkTargetFile.getPath());

                builder.setPositiveButton("YES", (dialog, which) -> {
                    if (isConverted)
                        FileviewHelper.ClearTag(info.position);
                    else
                        FileviewHelper.RemoveByIndex(info.position);

                    ChangeButtonState();
                    apkTargetFile.delete();

                    if (isConverted) {
                        File dirPico = new File(apkTargetFile.getPath().replace(apkTargetFile.getName(), ""));
                        if (dirPico.listFiles().length == 0)
                            dirPico.delete();
                    }

                    dialog.dismiss();
                }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            }
        }

        AlertDialog alert = builder.create();
        alert.show();

        return super.onContextItemSelected(item);
    }

    public void ButtonHelpOpen(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Help");
        builder.setMessage("⬤ Hold point any element to see its tooltip including files in the box.");

        builder.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
