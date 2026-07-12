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

Requires Java 21, the Android SDK, and Python 3.14 on the build machine (Chaquopy uses it to bundle yt-dlp).

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

- Chaquopy is open-source under the MIT license; see the [Chaquopy license](https://chaquo.com/chaquopy/license/).
- The APK is large (~50 MB) because it bundles Python and yt-dlp.
- On Android 8–9 (API 26–28) the app falls back to saving files in its private cache if MediaStore insertion fails. Gallery saving works best on Android 10+.
- Chaquopy 17 with Python 3.14 only provides native libraries for `arm64-v8a` and `x86_64`, so 32-bit `armeabi-v7a` devices are not supported.

## Pre-commit hooks

This project uses [pre-commit](https://pre-commit.com/) for linting, formatting and SAST.

Install pre-commit (if you don't have it) and register the hooks from the repository root:

```bash
pip install pre-commit
cd /path/to/this/repo
pre-commit install
```

Run all checks manually:

```bash
pre-commit run --all-files
```

Configured hooks:

- `trailing-whitespace`, `end-of-file-fixer`, `check-yaml`, `check-added-large-files`, `check-merge-conflict`
- `ktlint` -- lints and auto-formats Kotlin files
- `ruff` / `ruff-format` -- lints and formats the Chaquopy Python downloader
- `semgrep` -- runs SAST rules against Kotlin and Python source

Semgrep also runs in CI on every push and pull request using Python 3.12 (see `.github/workflows/semgrep.yml`).
