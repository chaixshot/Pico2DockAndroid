package com.hamer.pico2dock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.reandroid.apkeditor.compile.BuildOptions;
import com.reandroid.apkeditor.decompile.DecompileOptions;
import com.reandroid.apkeditor.merge.MergerOptions;

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

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import com.reandroid.archive.ZipAlign;
import com.android.apksigner.ApkSignerTool;

public class MainWorker extends Worker {

    private static final String CHANNEL_ID = "Pico2Dock_Process";
    private static final int NOTIFICATION_ID = 1;

    private String errorMessage;
    private final ProgressManager progressManager = ProgressManager.getInstance();

    public MainWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        createNotificationChannel();
        setForegroundAsync(getForegroundInfo());

        String[] apkFiles = getInputData().getStringArray("APK_FILES");
        boolean isHideDock = getInputData().getBoolean("IS_HIDE_DOCK", false);
        boolean isRePackage = getInputData().getBoolean("IS_REPACKAGE", false);
        boolean isRePackageAdv = getInputData().getBoolean("IS_REPACKAGE_ADV", false);
        String namePrefix = getInputData().getString("NAME_PREFIX");
        boolean isRename = getInputData().getBoolean("IS_RENAME", false);

        if (apkFiles == null) return Result.failure();

        progressManager.reset();
        progressManager.setApkFiles(apkFiles.clone());

        File keystore = Utils.GetKeystoreFile(getApplicationContext());

        for (int i = 0; i < apkFiles.length; i++) {
            String file = apkFiles[i];
            
            if (isStopped()) return Result.success();

            if (file.contains(Utils.FileIndicator.Error))
                continue;

            progressManager.postUpdate(ProcessUpdate.progress("## Current Status\nCleaning directory...", -1));
            Utils.CleanupTempDir();

            errorMessage = "";

            File dirPico2Dock = new File("storage/emulated/0/Pico2Dock");
            File dirWorker = new File(dirPico2Dock, "Worker");
            File dirUnsign = new File(dirPico2Dock, "Unsign");
            File apkFile = new File(file);
            String apkName = apkFile.getName();
            String filePath = apkFile.getAbsolutePath().replace(apkName, "");
            File dirOut = new File(filePath, "Pico");
            File dirApkOut = new File(dirOut, "Pico_" + apkName);
            File dirApkUnsing = new File(dirUnsign, apkName);

            if (!dirPico2Dock.exists())
                dirPico2Dock.mkdir();

            Utils.ProgressBar progressBar = new Utils.ProgressBar(apkFiles.length, 5);

            FileviewHelper.ChangeText(i, Utils.FileIndicator.Working + " " + file);

            if (!apkFile.exists() || !apkFile.isFile() || !apkFile.canRead()) {
                errorMessage = "Can't access file \"" + apkFile.getPath() + "\"";
                FileviewHelper.ChangeText(i, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + errorMessage);
                continue;
            }

            // Convert APKM to APK
            if (Pattern.matches(".*\\.(xapk|apkm|apks)", file)) {
                if (isStopped()) return Result.success();
                File dirMerger = new File(dirPico2Dock, "Merger");
                File dirZipper = new File(dirPico2Dock, "Zipper");
                File dirZipApk = new File(dirZipper, apkName);

                progressBar.Increase(null);

                try {
                    if (true) {
                        progressManager.postUpdate(ProcessUpdate.progress("## Merger\n**" + apkName + "**\nRemoving unnecessary architecture...", -1));

                        if (!dirZipper.exists())
                            dirZipper.mkdir();
                        if (!dirZipApk.exists())
                            dirZipApk.createNewFile();

                        Files.copy(apkFile.toPath(), dirZipApk.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        final Boolean[] pickArm64v8a = {false};

                        ZipFile zipFile = new ZipFile(dirZipApk);
                        List<FileHeader> fileHeaders = zipFile.getFileHeaders();
                        List<String> filesToRemove = new ArrayList<String>();

                        fileHeaders.forEach(fileHeader -> {
                            if (Pattern.matches(".*arm64_v8a.*", fileHeader.getFileName()))
                                pickArm64v8a[0] = true;
                        });

                        fileHeaders.forEach(fileHeader -> {
                            String fileName = fileHeader.getFileName();
                            if (Pattern.matches(".*config\\..{3,}(?<!dpi)\\.apk$", fileName)) {
                                if (!Pattern.matches(".*arm64_v8a.*", fileName)) {
                                    if (Pattern.matches(".*armeabi_v7a.*", fileName)) {
                                        if (pickArm64v8a[0])
                                            filesToRemove.add(fileName);
                                    } else
                                        filesToRemove.add(fileName);
                                }
                            }
                        });
                        zipFile.removeFiles(filesToRemove);
                        apkFile = dirZipApk;
                    }

                    progressManager.postUpdate(ProcessUpdate.progress("## Merger\nMerging multiple split **" + apkName + "**...", -1));

                    String newName = apkName.replaceAll("\\.x?apk[ms]?", ".apk");
                    MergerOptions options = new MergerOptions();
                    options.inputFile = apkFile;
                    options.outputFile = new File(dirMerger, newName);

                    Merger executor = new Merger(options, apkName, this::isStopped);
                    executor.runCommand();

                    apkFile.delete();
                    apkName = newName;
                    apkFile = new File(dirMerger, newName);
                    dirApkOut = new File(dirOut, "Pico_" + apkName);
                    dirApkUnsing = new File(dirUnsign, apkName);
                } catch (Exception error) {
                    if (isStopped()) return Result.success();
                    errorMessage = error.toString();
                    FileviewHelper.ChangeText(i, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                    progressBar.Increase(5);
                    continue;
                }
            }

            if (dirApkOut.exists()) {
                int count = 1;
                while (dirApkOut.exists()) {
                    String newPath = String.format(dirOut + "/Pico_%s (%d).apk", apkName.substring(0, apkName.length() - 4), count);
                    dirApkOut = new File(newPath);
                    count++;
                }
            }

            if (isStopped()) return Result.success();
            try {
                progressManager.postUpdate(ProcessUpdate.progress("## Decoder\nDecompiling resources of **" + apkName + "**...", -1));
                progressBar.Increase(null);

                DecompileOptions options = new DecompileOptions();
                options.inputFile = apkFile;
                options.outputFile = dirWorker;
                options.loadDex = 10;
                options.noCache = true;
                options.dex = true;

                Decompiler executor = new Decompiler(options, apkName, this::isStopped);
                executor.runCommand();
            } catch (Exception error) {
                if (isStopped()) return Result.success();
                errorMessage = "```\n" + error.toString() + "\n```";
                FileviewHelper.ChangeText(i, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                progressBar.Increase(4);
                continue;
            }

            if (isStopped()) return Result.success();
            try {
                progressManager.postUpdate(ProcessUpdate.progress("## Current Status\nModifing **AndroidManifest.xml** of **" + apkName + "**...", -1));
                progressBar.Increase(null);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                DocumentBuilder builder = factory.newDocumentBuilder();

                String android = "http://schemas.android.com/apk/res/android";
                File xmlFile = new File(dirWorker, "/AndroidManifest.xml");
                Document xmlDoc = builder.parse(xmlFile);
                Element xmlRoot = xmlDoc.getDocumentElement();
                Element application = (Element) xmlRoot.getElementsByTagName("application").item(0);

                // Modify Activity elements
                boolean isPortrait = false;
                Element vrPosition = xmlDoc.createElement("meta-data");
                vrPosition.setAttributeNS(android, "android:name", "pico.vr.position");
                vrPosition.setAttributeNS(android, "android:value", isHideDock ? "near_dialog" : "near");

                Element vrPositionOverlay = xmlDoc.createElement("meta-data");
                vrPositionOverlay.setAttributeNS(android, "android:name", "pico.vr.position.overlay");
                vrPositionOverlay.setAttributeNS(android, "android:value", "far");

                Element layout = xmlDoc.createElement("layout");
                layout.setAttributeNS(android, "android:defaultWidth", "900.0dp");
                layout.setAttributeNS(android, "android:defaultHeight", isPortrait ? "480.0dp" : "600.0dp");

                for (String tagName : new String[]{"activity", "activity-alias"}) {
                    NodeList activities = application.getElementsByTagName(tagName);
                    for (int itemIndex = 0; itemIndex < activities.getLength(); itemIndex++) {
                        boolean isMainActivity = false;
                        Element activity = (Element) activities.item(itemIndex);
                        NodeList actions = activity.getElementsByTagName("action");
                        for (int k = 0; k < actions.getLength(); k++) {
                            if ("android.intent.action.MAIN".equals(((Element) actions.item(k)).getAttributeNS(android, "name"))) {
                                isMainActivity = true;
                                break;
                            }
                        }

                        Element vrMode = xmlDoc.createElement("meta-data");
                        vrMode.setAttributeNS(android, "android:name", "pvr.2dtovr.mode");
                        vrMode.setAttributeNS(android, "android:value", isMainActivity ? "6" : "2");

                        activity.appendChild(vrPosition.cloneNode(true));
                        activity.appendChild(vrPositionOverlay.cloneNode(true));
                        activity.appendChild(vrMode.cloneNode(true));
                        activity.appendChild(layout.cloneNode(true));

                        activity.setAttributeNS(android, "android:resizeableActivity", "true");
                        if (isMainActivity)
                            activity.setAttributeNS(android, "android:screenOrientation", isPortrait ? "portrait" : "landscape");
                    }
                }

                // Root meta-data
                Map<String, String> rootMetaData = new LinkedHashMap<>();
                rootMetaData.put("pvr.2dtovr.mode", "6");
                rootMetaData.put("pvr.display.orientation", "180");
                for (Map.Entry<String, String> entry : rootMetaData.entrySet()) {
                    Element metaData = xmlDoc.createElement("meta-data");
                    metaData.setAttributeNS(android, "android:name", entry.getKey());
                    metaData.setAttributeNS(android, "android:value", entry.getValue());
                    xmlRoot.appendChild(metaData);
                }

                // Application meta-data
                Map<String, String> appMetaData = new LinkedHashMap<>();
                appMetaData.put("isPUI", "1");
                appMetaData.put("pvr.vrshell.mode", "1");
                appMetaData.put("com.pvr.hmd.trackingmode", "6dof");
                appMetaData.put("pico_permission_dim_show", "false");
                appMetaData.put("pvr.2dtovr.mode", "6");
                appMetaData.put("pvr.display.orientation", "180");
                appMetaData.put("feature", "2");
                appMetaData.put("feature_version", "2");
                appMetaData.put("feature.support_custom_panel", "1");
                appMetaData.put("channel_id", "PUI");
                for (Map.Entry<String, String> entry : appMetaData.entrySet()) {
                    Element metaData = xmlDoc.createElement("meta-data");
                    metaData.setAttributeNS(android, "android:name", entry.getKey());
                    metaData.setAttributeNS(android, "android:value", entry.getValue());
                    application.appendChild(metaData);
                }

                // Repackage
                if (isRePackage) {
                    String packageName = xmlRoot.getAttribute("package");
                    String newPackageName = packageName + "DOCK";
                    xmlRoot.setAttribute("package", newPackageName);

                    if (isRePackageAdv) {
                        String sharedId = xmlRoot.getAttributeNS(android, "sharedUserId");
                        if (sharedId != null && !sharedId.isEmpty()) {
                            xmlRoot.setAttributeNS(android, "android:sharedUserId", sharedId.replace(packageName, newPackageName));
                        }
                    }

                    NodeList providers = application.getElementsByTagName("provider");
                    for (int k = 0; k < providers.getLength(); k++) {
                        Element provider = (Element) providers.item(k);
                        String auth = provider.getAttributeNS(android, "authorities");
                        String newAuth = auth.contains(packageName) ? auth.replace(packageName, newPackageName) : auth + "DOCK";
                        provider.setAttributeNS(android, "android:authorities", newAuth);
                    }

                    NodeList permissionsList = xmlRoot.getElementsByTagName("permission");
                    for (int k = 0; k < permissionsList.getLength(); k++) {
                        Element permission = (Element) permissionsList.item(k);
                        String name = permission.getAttributeNS(android, "name");
                        permission.setAttributeNS(android, "android:name", isRePackageAdv ? name.replace(packageName, newPackageName) : name + "DOCK");
                    }

                    NodeList usesPermissionsList = xmlRoot.getElementsByTagName("uses-permission");
                    for (int k = 0; k < usesPermissionsList.getLength(); k++) {
                        Element usesPermission = (Element) usesPermissionsList.item(k);
                        String name = usesPermission.getAttributeNS(android, "name");
                        usesPermission.setAttributeNS(android, "android:name", isRePackageAdv ? name.replace(packageName, newPackageName) : name + "DOCK");
                    }

                    if (isRePackageAdv) {
                        NodeList activityAliases = application.getElementsByTagName("activity-alias");
                        for (int k = 0; k < activityAliases.getLength(); k++) {
                            Element alias = (Element) activityAliases.item(k);
                            String name = alias.getAttributeNS(android, "name");
                            alias.setAttributeNS(android, "android:name", name.replace(packageName, newPackageName));
                        }
                    }
                }

                // Rename
                if (namePrefix != null && !namePrefix.isEmpty()) {
                    String app_label = application.getAttributeNS(android, "label");
                    if (isRename) {
                        application.setAttributeNS(android, "android:label", namePrefix);
                    } else if (!Pattern.matches("@string/.+", app_label)) {
                        application.setAttributeNS(android, "android:label", app_label + namePrefix);
                    } else {
                        String stringID = app_label.replace("@string/", "");
                        try (Stream<Path> paths = Files.walk(Paths.get(dirWorker.getPath(), "resources"))) {
                            paths.filter(p -> p.getFileName().toString().equals("strings.xml")).forEach(path -> {
                                try {
                                    File stringXml = path.toFile();
                                    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringXml);
                                    NodeList tags = doc.getElementsByTagName("string");
                                    boolean modified = false;
                                    for (int k = 0; k < tags.getLength(); k++) {
                                        Element el = (Element) tags.item(k);
                                        if (el.getAttribute("name").equals(stringID)) {
                                            el.setTextContent(el.getTextContent() + namePrefix);
                                            modified = true;
                                            break;
                                        }
                                    }
                                    if (modified) {
                                        Transformer tf = TransformerFactory.newInstance().newTransformer();
                                        tf.transform(new DOMSource(doc), new StreamResult(stringXml));
                                    }
                                } catch (Exception ignored) {}
                            });
                        } catch (Exception ignored) {}
                    }
                }

                Transformer transManifest = TransformerFactory.newInstance().newTransformer();
                transManifest.setOutputProperty(OutputKeys.INDENT, "yes");
                transManifest.transform(new DOMSource(xmlDoc), new StreamResult(xmlFile));
            } catch (Exception error) {
                if (isStopped()) return Result.success();
                errorMessage = "```\n" + error.toString() + "\n```";
                FileviewHelper.ChangeText(i, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                progressBar.Increase(3);
                continue;
            }

            if (isStopped()) return Result.success();
            try {
                progressManager.postUpdate(ProcessUpdate.progress("## Encoder\nBuilding **" + apkName + "**...", -1));
                progressBar.Increase(null);

                BuildOptions options = new BuildOptions();
                options.inputFile = dirWorker;
                options.outputFile = dirApkUnsing;
                options.type = BuildOptions.TYPE_XML;
                options.noCache = true;

                Compiler executor = new Compiler(options, apkName, this::isStopped);
                executor.runCommand();
            } catch (Exception error) {
                if (isStopped()) return Result.success();
                errorMessage = "```\n" + error.toString() + "\n```";
                FileviewHelper.ChangeText(i, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                progressBar.Increase(2);
                continue;
            }

            if (isStopped()) return Result.success();
            try {
                progressManager.postUpdate(ProcessUpdate.progress("## Signer\nSigning **" + apkName + "**", -1));
                progressBar.Increase(null);

                if (!dirOut.exists()) dirOut.mkdir();

                File align = new File(dirApkUnsing.getAbsolutePath().replace(dirApkUnsing.getName(), "") + "align_" + dirApkUnsing.getName());
                ZipAlign.alignApk(dirApkUnsing, align);
                dirApkUnsing.delete();
                align.renameTo(dirApkUnsing);

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
                File idsig = new File(dirApkOut.getAbsolutePath() + ".idsig");
                if (idsig.exists()) idsig.delete();
            } catch (Exception error) {
                if (isStopped()) return Result.success();
                errorMessage = "```\n" + error.toString() + "\n```";
                FileviewHelper.ChangeText(i, Utils.FileIndicator.Error + " " + apkFile.getPath() + " " + Utils.FileIndicator.ErrorInfo + " " + error.toString());
                progressBar.Increase(1);
                continue;
            }

            progressBar.Increase(null);
            FileviewHelper.ChangeText(i, Utils.FileIndicator.Success + " " + file);
        }

        Utils.CleanupTempDir();
        if (errorMessage != null && !errorMessage.isEmpty()) {
            progressManager.postUpdate(ProcessUpdate.error(errorMessage));
        } else {
            progressManager.postUpdate(ProcessUpdate.success());
        }

        Utils.PlayAlertSound(getApplicationContext());
        try {
            Thread.sleep(2000); // Wait longer for the 1.5s sound
        } catch (InterruptedException ignored) {}

        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Utils.CleanupTempDir();
        progressManager.postUpdate(ProcessUpdate.cancelled());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pico2Dock Processing",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @NonNull
    @Override
    public ForegroundInfo getForegroundInfo() {
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Pico2Dock is processing files")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default icon
                .setOngoing(true)
                .build();
        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }
}
