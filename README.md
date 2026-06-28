# downloader

This repository contains **VidGrab**, an Android app that downloads videos using [yt-dlp](https://github.com/yt-dlp/yt-dlp) via [Chaquopy](https://chaquo.com/chaquopy/).

## Project layout

```
.
├── .github/workflows/   # GitHub Actions CI workflows
├── VidGrab/             # Android application
│   ├── app/             # App module source
│   ├── build.gradle.kts # Module build script
│   └── README.md        # App-specific build/run instructions
├── .pre-commit-config.yaml
├── .editorconfig
└── README.md            # This file
```

## CI

- **Semgrep SAST** — runs on every push/PR using Python 3.12
- **Build debug APK** — builds the Android debug APK after semgrep passes (`.github/workflows/build.yml`)

## Local development

See [`VidGrab/README.md`](./VidGrab/README.md) for:

- Tech stack and features
- Build prerequisites (Java 21, Android SDK, Python 3.14 for Chaquopy)
- How to install and run the app
- Pre-commit hook setup

## Quick start

```bash
cd VidGrab
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

The debug APK is written to `VidGrab/app/build/outputs/apk/debug/app-debug.apk`.

## Pre-commit hooks

Install and run from the repository root:

```bash
pip install pre-commit
pre-commit install
pre-commit run --all-files
```
