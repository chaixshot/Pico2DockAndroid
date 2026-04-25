<div align="center">
  <img src="Resource/icon.ico" width="128" height="128"/>
  
  # Pico2Dock Android
  ### Convert the APK file for Pico 4 VR to change the application state from Floating Far app to Dashboard Near Dock, similar to File Manager.<br>Allow multitasking while in a full-screen immersive app.
  ### [Desktop Version](https://github.com/chaixshot/Pico2Dock)
  </div>
  
  ## 🖥️ Application screenshot
  <image src="Resource/Android_Pico2Dock.png" width="400">
    
  ## 👓 VR Headset screenshot
  <image src="Resource/Screenshot_pl.solidexplorer2.jpeg" width="400"> <image src="Resource/Screenshot_org.mozilla.firefox_beta.jpeg" width="400"> <image src="Resource/Screenshot_com.google.android.apps.translate.jpeg" width="400"> <image src="Resource/Screenshot_app.android.apps.youtube.music.jpeg" width="400">
  
  ## ⛏️ Prerequisites
[Pico 4](https://www.picoxr.com/products/pico4) supports APK Architecture **arm64-v8a**, **armeabi-v7a**, and **armeabi**.

## 📐 How to use? 
1. Read and finish the [Prerequisites](#%EF%B8%8F-prerequisites)
2. Download the latest [Release](https://github.com/chaixshot/Pico2DockAndroid/releases) from the GitHub repo
3. Copy APK files to the headset and install or install via ``adb install`` command
4. Click the file selection box to choose files
6. Press the **Start** button and wait for the finish
7. Docked APK files are in **Pico** folder by the same folder as the original file, or long click the file in the box above to see the options

## ⁉️ Can an app change state on the fly?
No, but you can install **Docked** alongside **Floating** by checking the **Random package name** box option, and you can also add text after the app name for classification.

<image src="Resource/Screenshot_both.jpeg" width="400">

## 🙏 Special thanks to:
- [APKEditor](https://github.com/REAndroid/APKEditor) - Used for decompiling and recompiling Android Package
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) - Used for signing
- [FilePicker](https://github.com/TutorialsAndroid/FilePicker) - Android Library to select files
- [Markwon](https://github.com/noties/Markwon) - Textbox Markdown Syntax
- [zt-zip](https://github.com/zeroturnaround/zt-zip) - ZIP Library
