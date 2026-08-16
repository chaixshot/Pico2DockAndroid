<div align="center">
  <img src="Resource/icon.ico" width="128" height="128"/>
  
  # Pico2Dock Android
  [English](README.md) | [中文](README_zh.md) | [Русский](README_ru.md)
  
  ### Конвертируйте файлы (X)APK(M, S) для Pico 4 и Neo 3, чтобы изменить состояние приложения с плавающего (Floating Far) на панель инструментов (Dashboard Near Dock), аналогично файловому менеджеру.<br>Позволяет работать в режиме многозадачности во время использования полноэкранного иммерсивного приложения.
  ### [Версия для ПК](https://github.com/chaixshot/Pico2Dock)
</div>

>### ⚠️ Если ваш Pico 4 имеет [root-права](https://pico4.wiki/guides/root/01-root/), попробуйте вместо этого модуль Lsposed [PICO 2D Resolution](https://github.com/hhhbwc/pico-resfix).
  
## 🖥️ Скриншот приложения
<image src="Resource/Android_Pico2Dock.jpeg" width="400">
  
## 👓 Скриншоты VR-гарнитуры
<image src="Resource/Screenshot_pl.solidexplorer2.jpeg" width="400"> <image src="Resource/Screenshot_org.mozilla.firefox_beta.jpeg" width="400"> <image src="Resource/Screenshot_com.google.android.apps.translate.jpeg" width="400"> <image src="Resource/Screenshot_app.android.apps.youtube.music.jpeg" width="400">
  
## ⛏️ Предварительные условия
Поддерживаемые архитектуры: **arm64-v8a**, **armeabi-v7a** и **armeabi**.\
Поддерживаются расширения **.apk**, **.xapk**, **.apkm** и **.apks**.

## 📐 Как использовать? 
1. Ознакомьтесь с [предварительными условиями](#-предварительные-условия)
2. Загрузите последнюю версию из [релизов](https://github.com/chaixshot/Pico2DockAndroid/releases) на GitHub
3. Скопируйте APK-файлы на гарнитуру и установите их напрямую или через команду ``adb install``
4. Нажмите на поле выбора файлов, чтобы выбрать нужные файлы
5. Нажмите кнопку **Start** и дождитесь завершения процесса
6. Конвертированные APK-файлы находятся в папке **Pico** в той же директории, что и исходный файл, либо удерживайте нажатие на файл в списке выше, чтобы увидеть доступные опции

## ⁉️ Может ли приложение менять состояние «на лету»?
Нет, но вы можете установить **Docked**-версию параллельно с **Floating**-версией, выбрав опцию **Random package name**, а также добавить текст после названия приложения для удобства классификации.

<image src="Resource/Screenshot_both.jpeg" width="400">

## 🔃 Проверка обновлений?
Ознакомьтесь с [ObtainX](https://github.com/bikram-agarwal/ObtainX) на GitHub.

<image src="Resource/Screenshot_dev.bikram.obtainx_2026.04.30-18.15.18.692_612.jpeg" width="400">

## 🙏 Особая благодарность:
- [APKEditor](https://github.com/REAndroid/APKEditor) - Используется для декомпиляции и рекомпиляции Android-пакетов
- [uber-apk-signer](https://github.com/patrickfav/uber-apk-signer) - Используется для подписи
- [FilePicker](https://github.com/TutorialsAndroid/FilePicker) - Android-библиотека для выбора файлов
- [Markwon](https://github.com/noties/Markwon) - Поддержка синтаксиса Markdown в текстовых полях
- [zip4j](https://github.com/srikanth-lingala/zip4j) - Java-библиотека для работы с zip-файлами и потоками
