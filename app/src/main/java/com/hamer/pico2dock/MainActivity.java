package com.hamer.pico2dock;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
    File keystore;

    AsyncTask MainTask;

    Button ButtonStart;
    Button ButtonCancel;
    Button ButtonClear;
    ListView ListViewFile;
    TextView TextViewSelectHint;
    Switch SwtichHideDock;
    CheckBox CheckboxRePackage;
    CheckBox CheckboxRePackageAdv;

    boolean IsHideDock = false;
    boolean IsRePackage = false;
    boolean IsRePackageAdv = false;

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

        PermissionHelper.CheckPermission(this);
        keystore = Utils.GetKeystoreFile(this);

        ButtonStart = (Button) findViewById(R.id.ButtonStart);
        ButtonCancel = (Button) findViewById(R.id.ButtonCancel);
        ButtonClear = (Button) findViewById(R.id.ButtonClear);
        ListViewFile = (ListView) findViewById(R.id.ListViewFiles);
        TextViewSelectHint = (TextView) findViewById(R.id.TextFileSelectHint);
        SwtichHideDock = (Switch) findViewById(R.id.SwitchHideDock);

        CheckboxRePackage = (CheckBox) findViewById(R.id.CheckboxRePackage);
        CheckboxRePackageAdv = (CheckBox) findViewById(R.id.CheckboxRePackageAdv);
    }

    public void SelectFile(View view) {
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

        var _this = this;
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (files.length > 0) {
                    // File list view
                    ListAdapter myAdapter = new ArrayAdapter<String>(_this, android.R.layout.simple_list_item_1, files);
                    ListViewFile.setAdapter(myAdapter);
                    TextViewSelectHint.setVisibility(View.GONE);

                    APKFiles = files;
                }
            }
        });

        dialog.show();
    }

    public void StartMainTask(View view) {
        if (APKFiles != null && APKFiles.length > 0) {
            ButtonStart.setEnabled(false);
            ButtonCancel.setEnabled(true);
            ButtonClear.setEnabled(false);

            IsHideDock = SwtichHideDock.isChecked();
            IsRePackage = CheckboxRePackage.isChecked();
            IsRePackageAdv = CheckboxRePackageAdv.isChecked();

            MainTask = new Worker().execute(APKFiles);
        } else {
            ChangeStateText("### ERROR\nThere is no file in process.");
        }
    }

    public void ClearFileList(View view) {
        String[] files = new String[]{};
        // File list view
        ListAdapter myAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, files);

        ListViewFile.setAdapter(myAdapter);
        TextViewSelectHint.setVisibility(VISIBLE);

        APKFiles = files;
    }

    public void CancelMainTask(View view) {
        ChangeStateText("### Current Status\nCanceling process please wait...");

        MainTask.cancel(true);

        view.setEnabled(false);
    }

    private class Worker extends AsyncTask<String, String, String> {
        boolean isError = false;

        protected String doInBackground(String... APKFiles) {
            ChangeStateText("### Current Status\nCleaning...");
            Utils.CleanupTempDir();

            for (String file : APKFiles) {
                File apkFile = new File(file);
                String apkName = apkFile.getName();
                String filePath = apkFile.getAbsolutePath().replace(apkName, "");

                Boolean isExist = apkFile.exists();
                Boolean isFile = apkFile.isFile();
                Boolean isReadable = apkFile.canRead();

                if (isExist && isFile && isReadable) {
                    File dirPico2Dock = new File("storage/emulated/0/Pico2Dock");
                    File dirWorker = new File(dirPico2Dock + "/Worker");
                    File dirUnsign = new File(dirPico2Dock + "/Unsign");
                    File dirOut = new File(filePath + "/Pico");
                    File dirApkOut = new File(dirOut + "/Pico_" + apkName);
                    File dirApkUnsing = new File(dirUnsign + "/" + apkName);

                    if (!dirOut.exists())
                        dirOut.mkdir();

                    // Check if output apk file exist
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
                        ChangeStateText("### Current Status\nDecompiling **" + apkName + "**...");

                        DecompileOptions decompiler = new DecompileOptions();
                        decompiler.inputFile = apkFile;
                        decompiler.outputFile = dirWorker;
                        decompiler.loadDex = 1; // 1.4.2++
                        decompiler.runCommand();
                    } catch (Exception e) {
                        ChangeStateText(e.toString());
                        continue;
                    }

                    //?? -------------------- [[ Edit AndroidManifest.xml ]] --------------------
                    if (isCancelled()) break;
                    try {
                        ChangeStateText("### Current Status\nModifing **AndroidManifest.xml** of **" + apkName + "**...");

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
                                } catch (XPathExpressionException e) {
                                    ChangeStateText(e.toString());
                                    isError = true;
                                    continue;
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
                    } catch (Exception e) {
                        ChangeStateText(e.toString());
                        isError = true;
                        continue;
                    }

                    //?? -------------------- [[ Start compiler apk ]] --------------------
                    if (isCancelled()) break;
                    try {
                        ChangeStateText("### Current Status\nCompiling **" + apkName + "**...");

                        BuildOptions compiler = new BuildOptions();

                        compiler.inputFile = dirWorker;
                        compiler.outputFile = dirApkUnsing;
                        compiler.type = BuildOptions.TYPE_XML;
                        compiler.runCommand();
                    } catch (Exception e) {
                        ChangeStateText(e.toString());
                        isError = true;
                        continue;
                    }

                    //?? -------------------- [[ Start signing apk ]] --------------------
                    if (isCancelled()) break;
                    try {
                        ChangeStateText("### Current Status\nSigning **" + apkName + "**...");

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
                    } catch (Exception e) {
                        ChangeStateText(e.toString());
                        isError = true;
                        continue;
                    }

                    //?? -------------------- [[ Cleaning temp ]] --------------------
                    ChangeStateText("### Current Status\nCleaning...");
                    Utils.CleanupTempDir();
                } else {
                    ChangeStateText("Can't access file \"" + file + "\"");
                }
            }

            return null;
        }

        protected void onProgressUpdate(String... values) {
//            setProgressPercent(progress[0]);
        }

        protected void onPostExecute(String result) {
            ButtonStart.setEnabled(true);
            ButtonCancel.setEnabled(false);
            ButtonClear.setEnabled(true);

            if (!isError)
                ChangeStateText("### Current Status\nAll APK files have been modified.\nYou can install them using the APK files in Pico folder by the same folder as the original file.");
        }

        protected void onCancelled() {
            ButtonStart.setEnabled(true);
            ButtonCancel.setEnabled(false);
            ButtonClear.setEnabled(true);

            ChangeStateText("### Current Status\nCleaning...");
            Utils.CleanupTempDir();

            ChangeStateText("### Process has been terminated.");
        }
    }

    private void ChangeStateText(String text) {
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

    public void ClickRequestPermission(View view) {
        PermissionHelper.CheckPermission(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    PermissionHelper.PermissionsGranted();
                } else {

                }
                return;
            }
        }
    }
}