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

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
            properties.extensions = new String[]{"apk"};
            properties.show_hidden_files = false;

            FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
            dialog.setTitle("Select apk files");

            dialog.setDialogSelectionListener(new DialogSelectionListener() {
                @Override
                public void onSelectedFilePaths(String[] files) {
                    if (files.length > 0) {
                        APKFiles = files;
                        APKFilesOut = files;

                        FileviewHelper.FileviewApply(files);

                        TextViewSelectHint.setVisibility(View.GONE);
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

        TextViewSelectHint.setVisibility(VISIBLE);
        ChangeButtonState();
    }

    public void ButtonCancelPressed(View view) {
        if (!MainTask.isCancelled()) {
            ChangeStateText("### Current Status\n---\nCanceling process please wait...");

            MainTask.cancel(true);

            view.setEnabled(false);
        }
    }

    private class Worker extends AsyncTask<String, String, String> {
        String errorMessage;
        int index = 0;

        protected String doInBackground(String... apkFiles) {
            ChangeStateText("### Current Status\n---\nCleaning directory...");
            Utils.CleanupTempDir();

            for (String file : apkFiles) {
                File apkFile = new File(file);
                String apkName = apkFile.getName();
                String filePath = apkFile.getAbsolutePath().replace(apkName, "");

                Boolean isExist = apkFile.exists();
                Boolean isFile = apkFile.isFile();
                Boolean isReadable = apkFile.canRead();

                errorMessage = "";

                // skip is file error from previous task
                if (file.contains("❌"))
                    continue;

                //?? -------------------- [[ File indicator ]] --------------------
                FileviewHelper.FileviewChangeText(index, "🛠️ " + file);
                FileviewHelper.FileviewSelect(index);

                File dirPico2Dock = new File("storage/emulated/0/Pico2Dock");
                File dirWorker = new File(dirPico2Dock + "/Worker");
                File dirUnsign = new File(dirPico2Dock + "/Unsign");
                File dirOut = new File(filePath + "/Pico");
                File dirApkOut = new File(dirOut + "/Pico_" + apkName);
                File dirApkUnsing = new File(dirUnsign + "/" + apkName);

                if (!dirOut.exists())
                    dirOut.mkdir();

                if (!isExist || !isFile || !isReadable) {
                    errorMessage = "Can't access file \"" + file + "\"";
                    FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + errorMessage);

                    continue;
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
                    ChangeStateText("### Current Status\n---\nDecompiling **" + apkName + "**...");
                    IncreaseProgressBar(apkFiles.length, 1);

                    DecompileOptions options = new DecompileOptions();
                    options.inputFile = apkFile;
                    options.outputFile = dirWorker;
                    options.loadDex = 1; // 1.4.2++

                    Decompiler executor = new Decompiler(options, apkName);
                    executor.logMessage(this.toString());
                    executor.runCommand();
                } catch (Exception error) {
                    errorMessage = "```\n" + error.toString() + "\n```";
                    FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + error.toString());
                    IncreaseProgressBar(apkFiles.length, 4);

                    continue;
                } catch (OutOfMemoryError error) {
                    errorMessage = "Out of memory";
                    FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + errorMessage);
                    IncreaseProgressBar(apkFiles.length, 4);

                    continue;
                }

                //?? -------------------- [[ Edit AndroidManifest.xml ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("### Current Status\n---\nModifing **AndroidManifest.xml** of **" + apkName + "**...");
                    IncreaseProgressBar(apkFiles.length, 1);

                    String androidSpace = "http://schemas.android.com/apk/res/android";

                    File xmlFile = new File(dirWorker + "/AndroidManifest.xml");
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    dbFactory.setNamespaceAware(true);
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(xmlFile);
                    doc.getDocumentElement().normalize();

                    Element xmlRoot = doc.getDocumentElement();

                    // Add docked attribute
                    if (true) {
                        Element metaData = doc.createElement("meta-data");
                        metaData.setAttributeNS(androidSpace, "android:name", "pico.vr.position");
                        metaData.setAttributeNS(androidSpace, "android:value", IsHideDock ? "near_dialog" : "near");

                        NodeList applications = xmlRoot.getElementsByTagName("application");
                        for (int i = 0; i < applications.getLength(); i++) {
                            Element application = (Element) applications.item(i);

                            NodeList activities = application.getElementsByTagName("activity");
                            for (int j = 0; j < activities.getLength(); j++) {
                                Element activity = (Element) activities.item(j);
                                activity.appendChild(metaData.cloneNode(true));
                            }

                            NodeList aliases = application.getElementsByTagName("activity-alias");
                            for (int j = 0; j < aliases.getLength(); j++) {
                                Element alias = (Element) aliases.item(j);
                                alias.appendChild(metaData.cloneNode(true));
                            }
                        }
                    }

                    // Pico tag
                    if (true) {
                        Element application = (Element) xmlRoot.getElementsByTagName("application").item(0);

                        Element metaData1 = doc.createElement("meta-data");
                        metaData1.setAttributeNS(androidSpace, "android:name", "isPUI");
                        metaData1.setAttributeNS(androidSpace, "android:value", "1");
                        application.appendChild(metaData1);

                        Element metaData2 = doc.createElement("meta-data");
                        metaData2.setAttributeNS(androidSpace, "android:name", "pvr.vrshell.mode");
                        metaData2.setAttributeNS(androidSpace, "android:value", "1");
                        application.appendChild(metaData2);

                        Element metaData3 = doc.createElement("meta-data");
                        metaData3.setAttributeNS(androidSpace, "android:name", "pico_permission_dim_show");
                        metaData3.setAttributeNS(androidSpace, "android:value", "false");
                        application.appendChild(metaData3);
                    }

                    // Random package name
                    if (IsRePackage) {
                        String packageName = xmlRoot.getAttribute("package");
                        String ranPrefix = Utils.generateString(6);
                        String newPackageName = packageName + ranPrefix;

                        // Change package name
                        xmlRoot.setAttribute("package", newPackageName);

                        if (IsRePackageAdv) {
                            String sharedUserId = xmlRoot.getAttributeNS(androidSpace, "sharedUserId");
                            if (sharedUserId != null && !sharedUserId.isEmpty()) {
                                xmlRoot.setAttributeNS(androidSpace, "sharedUserId", sharedUserId.replace(packageName, newPackageName));
                            }
                        }

                        NodeList applicationNodes = xmlRoot.getElementsByTagName("application");
                        for (int i = 0; i < applicationNodes.getLength(); i++) {
                            Element application = (Element) applicationNodes.item(i);
                            NodeList providers = application.getElementsByTagName("provider");
                            for (int j = 0; j < providers.getLength(); j++) {
                                Element provider = (Element) providers.item(j);
                                String value = provider.getAttributeNS(androidSpace, "authorities");
                                if (value.contains(packageName)) {
                                    provider.setAttributeNS(androidSpace, "authorities", value.replace(packageName, newPackageName));
                                } else {
                                    provider.setAttributeNS(androidSpace, "authorities", value + ranPrefix);
                                }
                            }
                        }

                        // Change permission
                        {
                            XPathFactory xpathFactory = XPathFactory.newInstance();
                            XPath xpath = xpathFactory.newXPath();
                            try {
                                XPathExpression permissionExpr = xpath.compile("//permission");
                                NodeList permissionsList = (NodeList) permissionExpr.evaluate(xmlRoot, XPathConstants.NODESET);
                                for (int i = 0; i < permissionsList.getLength(); i++) {
                                    Element permission = (Element) permissionsList.item(i);
                                    String value = permission.getAttributeNS(androidSpace, "name");
                                    if (IsRePackageAdv) {
                                        permission.setAttributeNS(androidSpace, "name", value.replace(packageName, newPackageName));
                                    } else {
                                        permission.setAttributeNS(androidSpace, "name", value + ranPrefix);
                                    }
                                }

                                XPathExpression usesPermissionExpr = xpath.compile("//uses-permission");
                                NodeList usesPermissionsList = (NodeList) usesPermissionExpr.evaluate(xmlRoot, XPathConstants.NODESET);
                                for (int i = 0; i < usesPermissionsList.getLength(); i++) {
                                    Element permission = (Element) usesPermissionsList.item(i);
                                    String value = permission.getAttributeNS(androidSpace, "name");
                                    if (IsRePackageAdv) {
                                        permission.setAttributeNS(androidSpace, "name", value.replace(packageName, newPackageName));
                                    } else {
                                        permission.setAttributeNS(androidSpace, "name", value + ranPrefix);
                                    }
                                }

                                if (IsRePackageAdv) {
                                    XPathExpression activityAliasExpr = xpath.compile("//activity-alias");
                                    NodeList activityAliasList = (NodeList) activityAliasExpr.evaluate(xmlRoot, XPathConstants.NODESET);
                                    for (int i = 0; i < activityAliasList.getLength(); i++) {
                                        Element activityAlias = (Element) activityAliasList.item(i);
                                        String value = activityAlias.getAttributeNS(androidSpace, "name");
                                        activityAlias.setAttributeNS(androidSpace, "name", value.replace(packageName, newPackageName));
                                    }
                                }
                            } catch (XPathExpressionException error) {
                                errorMessage = "```\n" + error.toString() + "\n```";
                                FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + error.toString());

                                continue;
                            }
                        }
                    }

                    if (!NamePrefix.isEmpty()) {
                        Element application = (Element) xmlRoot.getElementsByTagName("application").item(0);
                        Attr labelAttr = application.getAttributeNodeNS(androidSpace, "label");

                        if (IsRename) {
                            if (labelAttr != null) {
                                labelAttr.setValue(NamePrefix);
                            } else {
                                application.setAttributeNS(androidSpace, "android:label", NamePrefix);
                            }
                        } else {
                            String stringID = "app_name";
                            if (labelAttr != null) {
                                String val = labelAttr.getValue();
                                if (val != null) {
                                    stringID = val.replace("@string/", "");
                                }
                            }

                            File resDir = new File(dirWorker+"/resources/package_1/res");
                            File[] dirs = resDir.listFiles(File::isDirectory);
                            if (dirs != null) {
                                for (File dir : dirs) {
                                    if (dir.getName().contains("values")) {
                                        File stringsFile = new File(dir, "strings.xml");
                                        if (stringsFile.exists()) {
                                            DocumentBuilderFactory sFactory = DocumentBuilderFactory.newInstance();
                                            DocumentBuilder sBuilder = sFactory.newDocumentBuilder();
                                            Document stringDoc = sBuilder.parse(stringsFile);
                                            stringDoc.getDocumentElement().normalize();

                                            NodeList stringNodes = stringDoc.getElementsByTagName("string");
                                            for (int i = 0; i < stringNodes.getLength(); i++) {
                                                Element srt = (Element) stringNodes.item(i);
                                                if (srt.hasAttribute("name") && srt.getAttribute("name").contains(stringID)) {
                                                    String currentValue = srt.getTextContent();
                                                    srt.setTextContent(currentValue + NamePrefix);
                                                }
                                            }

                                            // Save changes back to file
                                            TransformerFactory transformerFactory = TransformerFactory.newInstance();
                                            Transformer transformer = transformerFactory.newTransformer();
                                            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                                            DOMSource source = new DOMSource(stringDoc);
                                            StreamResult result = new StreamResult(stringsFile);
                                            transformer.transform(source, result);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Save changes to file
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    DOMSource source = new DOMSource(doc);
                    StreamResult result = new StreamResult(xmlFile);
                    transformer.transform(source, result);
                } catch (Exception error) {
                    errorMessage = "```\n" + error.toString() + "\n```";
                    FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + error.toString());
                    IncreaseProgressBar(apkFiles.length, 3);

                    continue;
                }

                //?? -------------------- [[ Start compiler apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("### Current Status\n---\nCompiling **" + apkName + "**...");
                    IncreaseProgressBar(apkFiles.length, 1);

                    BuildOptions options = new BuildOptions();

                    options.inputFile = dirWorker;
                    options.outputFile = dirApkUnsing;
                    options.type = BuildOptions.TYPE_XML;

                    Compiler executor = new Compiler(options, apkName);
                    executor.logMessage(this.toString());
                    executor.runCommand();
                } catch (Exception error) {
                    errorMessage = "```\n" + error.toString() + "\n```";
                    FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + error.toString());
                    IncreaseProgressBar(apkFiles.length, 2);

                    continue;
                } catch (OutOfMemoryError error) {
                    errorMessage = "Out of memory";
                    FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + errorMessage);
                    IncreaseProgressBar(apkFiles.length, 2);

                    continue;
                }

                //?? -------------------- [[ Start signing apk ]] --------------------
                if (isCancelled()) break;
                try {
                    ChangeStateText("### Current Status\n---\nSigning **" + apkName + "**...");
                    IncreaseProgressBar(apkFiles.length, 1);

                    String[] arg = new String[]{
                            "sign",
                            "--ks", keystore.getPath(),
                            "--key-pass", "pass:forpico2dock",
                            "--ks-pass", "pass:forpico2dock",
                            "--in", dirApkUnsing.getPath(),
                            "--out", dirApkOut.getPath(),
                    };
                    ApkSignerTool.main(arg);

                    File idsig = new File(dirApkOut + ".idsig");
                    idsig.delete();
                } catch (Exception error) {
                    errorMessage = "```\n" + error.toString() + "\n```";
                    FileviewHelper.FileviewChangeText(index, "❌ " + file + " ⭕ " + error.toString());
                    IncreaseProgressBar(apkFiles.length, 1);

                    continue;
                }

                //?? -------------------- [[ Cleaning temp ]] --------------------
                ChangeStateText("### Current Status\n---\nCleaning directory...");
                IncreaseProgressBar(apkFiles.length, 1);

                Utils.CleanupTempDir();
                FileviewHelper.FileviewChangeText(index, "✅ " + file);

                APKFilesOut[index] = dirApkOut.getPath();

                index++;
            }

            return null;
        }

        protected void onProgressUpdate(String... values) {
//            setProgressPercent(progress[0]);
        }

        protected void onPostExecute(String result) {

            if (errorMessage != null && !errorMessage.isEmpty()) {
                PercentText.setText("Error");

                ChangeStateText("### ERROR\n---\n\n" + errorMessage);
            } else {
                PercentText.setText("Successful");

                ChangeStateText("### Current Status\n---\nAll APK files have been modified.\nYou can install them using the APK files in Pico folder by the same folder as the original file.");
            }

            StatusProgressBar.setProgress(100);
            IsProcessRunning = false;
            ChangeButtonState();
        }

        protected void onCancelled() {
            PercentText.setText("Terminated");

            ChangeStateText("### Current Status\n---\nCleaning directory...");
            Utils.CleanupTempDir();

            ChangeStateText("### Current Status\n---\nProcess has been terminated.");

            IsProcessRunning = false;
            ChangeButtonState();
        }
    }

    //** UI
    private void ChangeButtonState() {
        if ((APKFiles != null && APKFiles.length > 0) &&
                (!IsProcessRunning))
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

    private void IncreaseProgressBar(int count, int time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StatusProgressBar.setVisibility(VISIBLE);
                StatusProgressBar.incrementProgressBy(((95 / 5) * time) / count);
                PercentText.setText(StatusProgressBar.getProgress() + "%");
            }
        });
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

        if (item.getTitle() == "Install") {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

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
                    ChangeStateText("### ERROR\n---\n\n" + e);
                }

                dialog.dismiss();
            }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());

            AlertDialog alert = builder.create();
            alert.show();
        }

        if (item.getTitle() == "Delete") {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("Do you want to delete?");
            builder.setMessage(apkTargetPath);

            builder.setPositiveButton("YES", (dialog, which) -> {
                apkTargetFile.delete();
                dialog.dismiss();
            }).setNegativeButton("NO", (dialog, which) -> dialog.dismiss());

            AlertDialog alert = builder.create();
            alert.show();
        }

        return super.onContextItemSelected(item);
    }
}