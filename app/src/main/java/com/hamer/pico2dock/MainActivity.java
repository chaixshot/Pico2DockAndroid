package com.hamer.pico2dock;

import static android.view.View.VISIBLE;

import static androidx.core.content.FileProvider.getUriForFile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.util.List;
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
    String[] APKFiles;
    String[] APKFilesOut;
    File keystore;

    AsyncTask MainTask;

    Button ButtonStart;
    Button ButtonCancel;
    Button ButtonClear;
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
    String NamePrefix;
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

        keystore = Utils.GetKeystoreFile();

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

        ResetAppearance();
        ChangeButtonState();
    }

    @Override
    public void onBackPressed() {
        if (DoubleBack + 2000 > System.currentTimeMillis()) {
            if (IsProcessRunning) {
                ButtonCancel.performClick();
                Toast.makeText(this, "Canceling process please wait...", Toast.LENGTH_SHORT).show();
            } else {
                super.onBackPressed();
            }
        } else {
            DoubleBack = System.currentTimeMillis();

            if (IsProcessRunning)
                Toast.makeText(this, "Press once again to Cancel", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Press once again to Exit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
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

                        FileviewHelper.FileviewApply(files);
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

        FileviewHelper.FileviewClearTag();
        ResetAppearance();
        ChangeButtonState();

        MainTask = new Worker().execute(APKFiles);
    }

    public void ButtonClearPressed(View view) {
        String[] empty = new String[]{};

        APKFiles = empty;
        APKFilesOut = empty;

        FileviewHelper.FileviewApply(empty);

        ChangeButtonState();
    }

    public void ButtonCancelPressed(View view) {
        if (!MainTask.isCancelled()) {
            ChangeStateText("## Current Status\nCanceling process please wait...");

            MainTask.cancel(true);

            view.setEnabled(false);
        }
    }

    private class Worker extends AsyncTask<String, String, String> {
        String errorMessage;

        @Override
        protected String doInBackground(String... apkFiles) {
            for (String file : apkFiles) {
                // skip is file error from previous task
                if (file.contains("❌"))
                    continue;

                ChangeStateText("## Current Status\nCleaning directory...");
                Utils.CleanupTempDir();

                errorMessage = "";

                File dirPico2Dock = new File("storage/emulated/0/Pico2Dock");
                File dirWorker = new File(dirPico2Dock, "Worker");
                File dirUnsign = new File(dirPico2Dock, "Unsign");
                File apkFile = new File(file);
                String apkName = apkFile.getName(); // Including extension
                String filePath = apkFile.getAbsolutePath().replaceAll(apkName + "$", "");
                File dirOut = new File(filePath, "Pico");
                File dirApkOut = new File(dirOut, "Pico_" + apkName);
                File dirApkUnsing = new File(dirUnsign, apkName);

                if (!dirPico2Dock.exists())
                    dirPico2Dock.mkdir();

                Utils.ProgressBar progressBar = new Utils.ProgressBar(apkFiles.length, 5);
                Integer index = Arrays.asList(apkFiles).indexOf(file);

                //?? -------------------- [[ File indicator ]] --------------------
                FileviewHelper.FileviewChangeText(index, "🛠️ " + file);
                FileviewHelper.FileviewSelect(index);

                //?? -------------------- [[ Check file access ]] --------------------
                if (!apkFile.exists() || !apkFile.isFile() || !apkFile.canRead()) {
                    errorMessage = "Can't access file \"" + apkFile.getPath() + "\"";
                    FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + errorMessage);

                    continue;
                }

                //?? -------------------- [[ Convert APKM to APK ]] --------------------
                if (Pattern.matches(".*\\.(xapk|apkm|apks)", file)) {
                    File dirMerger = new File(dirPico2Dock, "Merger");
                    File dirZipper = new File(dirPico2Dock, "Zipper");
                    File dirZipApk = new File(dirZipper, apkName);

                    progressBar.Step += 1;
                    progressBar.Increase(null);

                    try {
                        if (true) { // Clean unnecessary architecture
                            ChangeStateText("## Merger\n**" + apkName + "**\nRemoving unnecessary architecture...");

                            if (!dirZipper.exists())
                                dirZipper.mkdir();
                            if (!dirZipApk.exists())
                                dirZipApk.createNewFile();

                            Files.copy(apkFile.toPath(), dirZipApk.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            final Boolean[] pickArm64v8a = {false};
                            File finalApkFile = apkFile;

                            ZipFile zipFile = new ZipFile(dirZipApk);
                            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
                            List<String> filesToRemove = new ArrayList<String>();

                            // Find arm64_v8a
                            fileHeaders.forEach(fileHeader -> {
                                if (Pattern.matches(".*arm64_v8a.*", fileHeader.getFileName()))
                                    pickArm64v8a[0] = true;
                            });

                            // Removing unnecessary architecture
                            fileHeaders.forEach(fileHeader -> {
                                String fileName = fileHeader.getFileName();

                                if (Pattern.matches(".*config\\..{3,}(?<!dpi)\\.apk$", fileName)) { // is architecture file
                                    if (!Pattern.matches(".*arm64_v8a.*", fileName)) { // is not arm64_v8a
                                        if (Pattern.matches(".*armeabi_v7a.*", fileName)) { // is armeabi_v7a
                                            if (pickArm64v8a[0]) // is no arm64_v8a
                                                filesToRemove.add(fileName);
                                        } else
                                            filesToRemove.add(fileName);
                                    }
                                }
                            });
                            zipFile.removeFiles(filesToRemove);

                            // Change APK target to clean file
                            apkFile = dirZipApk;
                        }

                        ChangeStateText("## Merger\nMerging multiple split **" + apkName + "**...");

                        // Start merge split APK
                        String newName = apkName.replaceAll("\\.x?apk[ms]?", ".apk");
                        MergerOptions options = new MergerOptions();
                        options.inputFile = apkFile;
                        options.outputFile = new File(dirMerger, newName);

                        Merger executor = new Merger(options, apkName);
                        executor.runCommand();

                        // Change APK target to merged file
                        apkFile.delete();
                        apkName = newName;
                        apkFile = new File(dirMerger, newName);
                        dirApkOut = new File(dirOut, "Pico_" + apkName);
                        dirApkUnsing = new File(dirUnsign, apkName);
                    } catch (Exception error) {
                        if (!isCancelled()) {
                            errorMessage = error.toString();
                            FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + error.toString());
                            progressBar.Increase(5);
                        }

                        continue;
                    } catch (OutOfMemoryError error) {
                        if (!isCancelled()) {
                            errorMessage = "Out of memory";
                            FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + errorMessage);
                            progressBar.Increase(5);
                        }
                        cancel(true);

                        continue;
                    }
                }

                //?? -------------------- [[ Rename ]] --------------------
                if (dirApkOut.exists()) {
                    int count = 1;
                    while (dirApkOut.exists()) {
                        String newPath = String.format(dirOut + "/Pico_%s (%d).apk", apkName.substring(0, apkName.length() - 4), count);
                        dirApkOut = new File(newPath);
                        count++;
                    }
                    ;
                }

                //?? -------------------- [[ Start decompiler apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Decoder\nDecompiling resources of **" + apkName + "**...");
                    progressBar.Increase(null);

                    DecompileOptions options = new DecompileOptions();
                    options.inputFile = apkFile;
                    options.outputFile = dirWorker;
                    options.loadDex = 10; // 1.4.2++

                    Decompiler executor = new Decompiler(options, apkName);
                    executor.runCommand();
                } catch (Exception error) {
                    if (!isCancelled()) {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + error.toString());
                        progressBar.Increase(4);
                    }

                    continue;
                } catch (OutOfMemoryError error) {
                    if (!isCancelled()) {
                        errorMessage = "Out of memory";
                        FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + errorMessage);
                        progressBar.Increase(4);
                    }
                    cancel(true);

                    continue;
                }

                //?? -------------------- [[ Edit AndroidManifest.xml ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Current Status\nModifing **AndroidManifest.xml** of **" + apkName + "**...");
                    progressBar.Increase(null);

                    String androidSpace = "http://schemas.android.com/apk/res/android";

                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    File xmlFile = new File(dirWorker, "/AndroidManifest.xml");
                    Document xmlDoc = builder.parse(xmlFile);

                    Element xmlRoot = xmlDoc.getDocumentElement();

                    NodeList applications = xmlRoot.getElementsByTagName("application");
                    Element application = (Element) applications.item(0);

                    // Add docked attribute
                    if (true) {
                        Element metaDataVrPosition = xmlDoc.createElement("meta-data");
                        metaDataVrPosition.setAttributeNS(androidSpace, "android:name", "pico.vr.position");
                        metaDataVrPosition.setAttributeNS(androidSpace, "android:value", IsHideDock ? "near_dialog" : "near");

                        Element metaDataVrMode = xmlDoc.createElement("meta-data");
                        metaDataVrMode.setAttributeNS(androidSpace, "android:name", "pvr.2dtovr.mode");
                        metaDataVrMode.setAttributeNS(androidSpace, "android:value", "6");

                        // Add metaData to all activities and activity-alias elements under application
                        NodeList activities = application.getElementsByTagName("activity");
                        for (int i = 0; i < activities.getLength(); i++) {
                            activities.item(i).appendChild(metaDataVrPosition.cloneNode(true));
                            activities.item(i).appendChild(metaDataVrMode.cloneNode(true));
                        }

                        NodeList aliases = application.getElementsByTagName("activity-alias");
                        for (int i = 0; i < aliases.getLength(); i++) {
                            aliases.item(i).appendChild(metaDataVrPosition.cloneNode(true));
                            aliases.item(i).appendChild(metaDataVrMode.cloneNode(true));
                        }
                    }

                    // Pico tag
                    if (true) {
                        Element metaDataIsPUI = xmlDoc.createElement("meta-data");
                        metaDataIsPUI.setAttributeNS(androidSpace, "android:name", "isPUI");
                        metaDataIsPUI.setAttributeNS(androidSpace, "android:value", "1");
                        application.appendChild(metaDataIsPUI);

                        Element metaDataVRShell = xmlDoc.createElement("meta-data");
                        metaDataVRShell.setAttributeNS(androidSpace, "android:name", "pvr.vrshell.mode");
                        metaDataVRShell.setAttributeNS(androidSpace, "android:value", "1");
                        application.appendChild(metaDataVRShell);

                        Element metaDataTrackingMode = xmlDoc.createElement("meta-data");
                        metaDataTrackingMode.setAttributeNS(androidSpace, "android:name", "com.pvr.hmd.trackingmode");
                        metaDataTrackingMode.setAttributeNS(androidSpace, "android:value", "3dof");
                        application.appendChild(metaDataTrackingMode);

                        Element metaDataPermissionDimShow = xmlDoc.createElement("meta-data");
                        metaDataPermissionDimShow.setAttributeNS(androidSpace, "android:name", "pico_permission_dim_show");
                        metaDataPermissionDimShow.setAttributeNS(androidSpace, "android:value", "false");
                        application.appendChild(metaDataPermissionDimShow);

                        application.setAttributeNS(androidSpace, "android:screenOrientation", "landscape");
                    }

                    // Get package name and generate new package name
                    String packageName = xmlRoot.getAttribute("package");
                    String ranPrefix = Utils.generateString(6);
                    String newPackageName = packageName + ranPrefix;

                    // Random package name
                    if (IsRePackage) {
                        xmlRoot.setAttribute("package", newPackageName);

                        if (IsRePackageAdv) {
                            String sharedUserId = xmlRoot.getAttributeNS(androidSpace, "sharedUserId");
                            if (sharedUserId != null && !sharedUserId.isEmpty()) {
                                String newSharedUserId = sharedUserId.replace(packageName, newPackageName);
                                xmlRoot.setAttributeNS(androidSpace, "android:sharedUserId", newSharedUserId);
                            }
                        }

                        // Update providers authorities attribute
                        NodeList providers = application.getElementsByTagName("provider");
                        for (int i = 0; i < providers.getLength(); i++) {
                            Element provider = (Element) providers.item(i);
                            String authorities = provider.getAttributeNS(androidSpace, "authorities");
                            if (authorities.contains(packageName)) {
                                provider.setAttributeNS(androidSpace, "android:authorities", authorities.replace(packageName, newPackageName));
                            } else {
                                provider.setAttributeNS(androidSpace, "android:authorities", authorities + ranPrefix);
                            }
                        }

                        // Change permissions
                        NodeList permissionsList = xmlRoot.getElementsByTagName("permission");
                        for (int i = 0; i < permissionsList.getLength(); i++) {
                            Element permission = (Element) permissionsList.item(i);
                            String name = permission.getAttributeNS(androidSpace, "name");
                            if (IsRePackageAdv) {
                                permission.setAttributeNS(androidSpace, "android:name", name.replace(packageName, newPackageName));
                            } else {
                                permission.setAttributeNS(androidSpace, "android:name", name + ranPrefix);
                            }
                        }

                        NodeList usesPermissionsList = xmlRoot.getElementsByTagName("uses-permission");
                        for (int i = 0; i < usesPermissionsList.getLength(); i++) {
                            Element usesPermission = (Element) usesPermissionsList.item(i);
                            String name = usesPermission.getAttributeNS(androidSpace, "name");
                            if (IsRePackageAdv) {
                                usesPermission.setAttributeNS(androidSpace, "android:name", name.replace(packageName, newPackageName));
                            } else {
                                usesPermission.setAttributeNS(androidSpace, "android:name", name + ranPrefix);
                            }
                        }

                        if (IsRePackageAdv) {
                            NodeList activityAliases = application.getElementsByTagName("activity-alias");
                            for (int i = 0; i < activityAliases.getLength(); i++) {
                                Element alias = (Element) activityAliases.item(i);
                                String name = alias.getAttributeNS(androidSpace, "name");
                                alias.setAttributeNS(androidSpace, "android:name", name.replace(packageName, newPackageName));
                            }
                        }
                    }

                    // Change app name
                    if (!NamePrefix.isEmpty()) {
                        String app_name = application.getAttributeNS(androidSpace, "label");

                        if (IsRename || !Pattern.matches("@string\\/.*", app_name)) {
                            application.setAttributeNS(androidSpace, "android:label", NamePrefix);
                        } else {
                            String stringID = app_name.replace("@string/", "");

                            // Iterate over res/values* directories and update strings.xml
                            Path resPath = Paths.get(dirWorker + "/resources/package_1/res");
                            try (Stream<Path> dirs = Files.list(resPath)) {
                                dirs.filter(Files::isDirectory)
                                        .filter(dir -> dir.getFileName().toString().contains("values"))
                                        .forEach(dir -> {
                                            Path stringsFile = dir.resolve("strings.xml");
                                            if (Files.exists(stringsFile)) {
                                                try {
                                                    Document stringDoc = builder.parse(stringsFile.toFile());
                                                    Element stringRoot = stringDoc.getDocumentElement();
                                                    NodeList strings = stringRoot.getElementsByTagName("string");
                                                    for (int i = 0; i < strings.getLength(); i++) {
                                                        Element srt = (Element) strings.item(i);
                                                        String nameAttr = srt.getAttribute("name");
                                                        if (nameAttr.contains(stringID)) {
                                                            String currentValue = srt.getTextContent();
                                                            srt.setTextContent(currentValue + NamePrefix);
                                                        }
                                                    }
                                                    // Save updated strings.xml
                                                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                                                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                                    transformer.transform(new DOMSource(stringDoc), new StreamResult(stringsFile.toFile()));
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        });
                            }
                        }
                    }

                    // Save the modified AndroidManifest.xml
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.transform(new DOMSource(xmlDoc), new StreamResult(xmlFile));
                } catch (Exception error) {
                    if (!isCancelled()) {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + error.toString());
                        progressBar.Increase(3);
                    }

                    continue;
                }

                //?? -------------------- [[ Start compiler apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Encoder\nBuilding **" + apkName + "**...");
                    progressBar.Increase(null);

                    BuildOptions options = new BuildOptions();

                    options.inputFile = dirWorker;
                    options.outputFile = dirApkUnsing;
                    options.type = BuildOptions.TYPE_XML;

                    Compiler executor = new Compiler(options, apkName);
                    executor.runCommand();
                } catch (Exception error) {
                    if (!isCancelled()) {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + error.toString());
                        progressBar.Increase(2);
                    }

                    continue;
                } catch (OutOfMemoryError error) {
                    if (!isCancelled()) {
                        errorMessage = "Out of memory";
                        FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + errorMessage);
                        progressBar.Increase(2);
                    }
                    cancel(true);

                    continue;
                }

                //?? -------------------- [[ Start signing apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("## Signer\nSigning **" + apkName + "**");
                    progressBar.Increase(null);

                    if (!dirOut.exists())
                        dirOut.mkdir();

                    // zipalign
                    File align = new File(dirApkUnsing.getAbsolutePath().replaceAll(dirApkUnsing.getName() + "$", "") + "align_" + dirApkUnsing.getName());
                    ZipAlign.alignApk(dirApkUnsing, align);
                    dirApkUnsing.delete();
                    align.renameTo(dirApkUnsing);

                    // Uber signer
                    String[] arg = new String[]{
                            "sign",
                            "--ks", keystore.getPath(),
                            "--key-pass", "pass:forpico2dock",
                            "--ks-pass", "pass:forpico2dock",
                            "--min-sdk-version", "29",
                            "--max-sdk-version", "29",
                            "--v4-signing-enabled", "false",
                            "--in", dirApkUnsing.getPath(),
                            "--out", dirApkOut.getPath(),
                    };
                    ApkSignerTool.main(arg);
                    File idsig = new File(dirApkOut, ".idsig");
                    idsig.delete();
                } catch (Exception error) {
                    if (!isCancelled()) {
                        errorMessage = "```\n" + error.toString() + "\n```";
                        FileviewHelper.FileviewChangeText(index, "❌ " + apkFile.getPath() + " ⭕ " + error.toString());
                        progressBar.Increase(1);
                    }

                    continue;
                }

                progressBar.Increase(null);
                FileviewHelper.FileviewChangeText(index, "✅ " + file);
                APKFilesOut[index] = dirApkOut.getPath();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Utils.CleanupTempDir();

            if (errorMessage != null && !errorMessage.isEmpty()) {
                PercentText.setText("Error");

                ChangeStateText("## ERROR\n\n" + errorMessage);
            } else {
                PercentText.setText("Successful");

                ChangeStateText("## Current Status\nAll files have been modified.\n* The APK files are in the Pico folder by the same directory as the original file.\n* Long click file in the box above to see the options.");
            }

            StatusProgressBar.setProgress(100);
            IsProcessRunning = false;
            ChangeButtonState();
        }

        @Override
        protected void onCancelled() {
            PercentText.setText("Terminated");

            Utils.CleanupTempDir();

            ChangeStateText("## Current Status\nProcess has been terminated.");

            StatusProgressBar.setProgress(100);
            IsProcessRunning = false;
            ChangeButtonState();
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
        File apkFile = new File(apkPath.replace("🛠️ ", "").replace("✅ ", ""));

        String apkOutPath = APKFilesOut[info.position];
        File apkOutFile = new File(apkOutPath);

        Boolean isConverted = apkPath.contains("✅");

        String apkTargetPath = isConverted ? apkOutPath : apkPath;
        File apkTargetFile = isConverted ? apkOutFile : apkFile;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (IsProcessRunning) {
            builder.setTitle("");
            builder.setMessage("Can't do this action while processing");

            builder.setPositiveButton("Close", (dialog, which) -> {
                dialog.dismiss();
            });
        } else {
            if (item.getTitle() == "Install") {

                builder.setTitle("Do you want to install?");
                builder.setMessage(apkTargetPath);

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

            if (item.getTitle() == "Remove") {
                builder.setTitle("Do you want to remove?");
                builder.setMessage(apkTargetPath);

                builder.setPositiveButton("YES", (dialog, which) -> {
                    List<String> list = new ArrayList<String>(Arrays.asList(APKFiles));
                    list.remove(info.position);
                    APKFiles = list.toArray(new String[0]);

                    FileviewHelper.FileviewApply(APKFiles);
                    ChangeButtonState();

                    dialog.dismiss();
                }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
            }

            if (item.getTitle() == "Delete") {
                builder.setTitle("Do you want to delete?");
                builder.setMessage(apkTargetPath);

                builder.setPositiveButton("YES", (dialog, which) -> {
                    if (!isConverted) {
                        List<String> list = new ArrayList<String>(Arrays.asList(APKFiles));
                        list.remove(info.position);
                        APKFiles = list.toArray(new String[0]);
                        FileviewHelper.FileviewApply(APKFiles);
                    }

                    ChangeButtonState();
                    apkTargetFile.delete();

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
        builder.setMessage("⬤ Hole point any element to see its tooltip including files in the box.");

        builder.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}