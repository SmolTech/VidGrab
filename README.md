# VidGrab

This repository contains **VidGrab**, an Android app that downloads videos using [yt-dlp](https://github.com/yt-dlp/yt-dlp) via [Chaquopy](https://chaquo.com/chaquopy/).

This ia an AI developed app, built with Kimi.

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
- **Build release APK** — builds the Android release APK after semgrep passes (`.github/workflows/build.yml`)
- **Publish GitHub Release** — on `v*` tags, signs the release APK and attaches it to a GitHub Release

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

## Releasing

Tags drive releases. Create a release locally or on GitHub with a tag like `v1.2.0`:

```bash
git tag -a v1.2.0 -m "Release 1.2.0"
git push origin v1.2.0
```

Before pushing the tag, make sure:

- `versionName` and `versionCode` in `VidGrab/app/build.gradle.kts` are bumped.
- `CurrentVersion` and `CurrentVersionCode` in `metadata/us.smoltech.vidgrab.yml` match.

The CI workflow will verify these values match the tag, build a release APK, sign it, and attach it to a GitHub Release.

### Required GitHub secrets

To sign the release APK, create these repository secrets:

| Secret | Value |
|--------|-------|
| `SIGNING_KEY_BASE64` | Base64-encoded release keystore file |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

Generate a keystore if you don’t have one:

```bash
keytool -genkey -v \
  -keystore vidgrab-release.keystore \
  -alias vidgrab \
  -keyalg RSA -keysize 4096 -validity 10000
```

Then base64-encode it for the secret (works on Linux and macOS):

```bash
base64 vidgrab-release.keystore | tr -d '\n'
```

### F-Droid

F-Droid builds the app from source using the recipe in `metadata/us.smoltech.vidgrab.yml`. The GitHub Release APK is the upstream-signed reference that F-Droid can use for reproducible-build verification.

## Pre-commit hooks

Install and run from the repository root:

```bash
pip install pre-commit
pre-commit install
pre-commit run --all-files
```

## License

VidGrab is licensed under the GNU General Public License v3.0 or later
(GPL-3.0-or-later). See [LICENSE](LICENSE) for the full text.
