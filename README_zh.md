<div align="center">
  <img src="Resource/icon.ico" width="128" height="128"/>
  
  # Pico2Dock Android
  [English](README.md) | [中文](README_zh.md) | [Русский](README_ru.md)
  
  ### 将 Pico 4 和 Neo 3 的 (X)APK(M, S) 文件从悬浮远景应用 (Floating Far) 转换为仪表盘近景停靠应用 (Dashboard Near Dock)，类似于文件管理器。<br>允许在全屏沉浸式应用中进行多任务处理。
  ### [桌面版本](https://github.com/chaixshot/Pico2Dock)
</div>

>### ⚠️ 如果您的 Pico 4 已[获取 Root 权限](https://pico4.wiki/guides/root/01-root/)，请尝试使用 [PICO 2D Resolution](https://github.com/hhhbwc/pico-resfix) Lsposed 模块。
  
## 🖥️ 应用截图
<image src="Resource/Android_Pico2Dock.jpeg" width="400">
  
## 👓 VR 头显截图
<image src="Resource/Screenshot_pl.solidexplorer2.jpeg" width="400"> <image src="Resource/Screenshot_org.mozilla.firefox_beta.jpeg" width="400"> <image src="Resource/Screenshot_com.google.android.apps.translate.jpeg" width="400"> <image src="Resource/Screenshot_app.android.apps.youtube.music.jpeg" width="400">
  
## ⛏️ 前提条件
支持的架构有 **arm64-v8a**, **armeabi-v7a** 和 **armeabi**。\
文件可以是 **.apk**, **.xapk**, **.apkm** 和 **.apks**。

## 📐 如何使用？ 
1. 阅读并完成[前提条件](#-前提条件)
2. 从 GitHub 仓库下载最新的[发布版本](https://github.com/chaixshot/Pico2DockAndroid/releases)
3. 将 APK 文件复制到头显并安装，或通过 ``adb install`` 命令安装
4. 点击文件选择框选择文件
5. 按下 **开始 (Start)** 按钮并等待完成
6. 转换后的 APK 文件位于原文件所在目录下的 **Pico** 文件夹中，或者长按上方框中的文件以查看选项

## ⁉️ 应用可以在运行中改变状态吗？
不可以，但您可以通过勾选 **随机包名 (Random package name)** 选项来同时安装 **停靠 (Docked)** 版本和 **悬浮 (Floating)** 版本，还可以为应用名称添加后缀以便区分。

<image src="Resource/Screenshot_both.jpeg" width="400">

## 🔃 检查应用更新？
查看 GitHub 上的 [ObtainX](https://github.com/bikram-agarwal/ObtainX)。

<image src="Resource/Screenshot_dev.bikram.obtainx_2026.04.30-18.15.18.692_612.jpeg" width="400">

## 🙏 特别感谢：
- [APKEditor](https://github.com/REAndroid/APKEditor) - 用于反编译和重新编译 Android 安装包
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) - 用于签名
- [FilePicker](https://github.com/TutorialsAndroid/FilePicker) - 用于选择文件的 Android 库
- [Markwon](https://github.com/noties/Markwon) - 文本框 Markdown 语法支持
- [zip4j](https://github.com/srikanth-lingala/zip4j) - 用于 zip 文件和流的 Java 库
