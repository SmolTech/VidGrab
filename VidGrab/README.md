# VidGrab

A self-contained Android app that downloads videos using [yt-dlp](https://github.com/yt-dlp/yt-dlp).

## Features

- Paste a video URL and tap **Download**.
- Runs yt-dlp inside a foreground service with a progress notification.
- Saves finished videos to `Movies/VidGrab` via the MediaStore (Android 10+).
- Shows download progress, conversion state, and errors in the Compose UI.

## Tech stack

- Kotlin 2.1.20 + Jetpack Compose
- AGP 8.13.2 / Gradle 8.14
- Min SDK 26 / Target SDK 36
- [Chaquopy 17.0.0](https://chaquo.com/chaquopy/) embeds Python 3.14 and yt-dlp
- Foreground service (`dataSync`) for background downloads

## Build

Requires Java 21 and the Android SDK.

```bash
cd VidGrab
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Install

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Open the app.
2. Paste a supported video URL (use the paste icon or type it in).
3. Tap **Download**.
4. Watch the notification / UI for progress.
5. The finished video appears in your device's `Movies/VidGrab` folder.

## Notes

- Chaquopy's free edition is used. Distribution may require attribution; see the [Chaquopy license](https://chaquo.com/chaquopy/license/).
- The APK is large (~50 MB) because it bundles Python and yt-dlp.
- On Android 8–9 (API 26–28) the app falls back to saving files in its private cache if MediaStore insertion fails. Gallery saving works best on Android 10+.
- Chaquopy 17 with Python 3.14 only provides native libraries for `arm64-v8a` and `x86_64`, so 32-bit `armeabi-v7a` devices are not supported.
