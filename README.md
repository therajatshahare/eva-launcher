# EVA TV Launcher

A lightweight, remote-friendly home launcher for Android TV boxes, with fresh wallpapers from Reddit and over-the-air updates straight from GitHub.

![Platform](https://img.shields.io/badge/platform-Android%20TV-3DDC84?logo=android&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26%20(Android%208.0)-blue)
![APK size](https://img.shields.io/badge/APK-~530%20KB-success)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

EVA is a fork of [CleanYourTV Launcher](https://github.com/Gathin23/cleanyourtv) by Gathin23. The upstream launcher is deliberately isolated: no network permissions, and wallpapers are pushed manually over ADB. EVA keeps the same tiny footprint and clean design, and adds just enough networking to make the launcher self-maintaining.

---

## Features

| Feature | What it does |
|---|---|
| **Reddit wallpaper sync** | Pulls landscape, high-resolution images from `r/wallpapers` and rotates them as your home background. |
| **In-app OTA updates** | Checks GitHub Releases for a newer version, downloads it in the background, and prompts you to install. |
| **Tiny footprint** | Base APK is roughly 530 KB, with R8 shrinking enabled and no heavy dependencies such as `androidx.core`. |
| **Remote-friendly** | Built for D-pad navigation on TV boxes. |
| **Automated releases** | Push a version tag and GitHub Actions builds, signs and publishes the APK. |

---

## How it works

### Reddit wallpaper sync (`WallpaperFetcher.java`)

- Queries `https://www.reddit.com/r/wallpapers/hot.json?limit=25`.
- Sends an Android TV User-Agent to avoid Reddit's HTTP 403 blocks.
- Skips portrait and gallery posts, and ignores files under 10 KB.
- Downsamples images in memory so low-RAM TV boxes don't run out of memory.
- Saves results to `getExternalFilesDir(null)/wallpapers/`, where the upstream `WallpaperPlayer` and rotation engine pick them up automatically.

### OTA updates (`UpdateManager.java` and `UpdateFileProvider.java`)

- Calls `https://api.github.com/repos/therajatshahare/eva-launcher/releases/latest`.
- Compares the installed `versionName` with the release `tag_name`.
- If a newer version exists, downloads `app-release.apk` to `cacheDir/updates/launcher-update.apk`.
- Hands the file to Android's Package Installer through `UpdateFileProvider`, a small custom `ContentProvider` that grants temporary read access without pulling in `androidx.core`.

---

## Requirements

- Android TV device running **Android 8.0 (API 26) or higher**
- Permissions used: `INTERNET` and `REQUEST_INSTALL_PACKAGES`
- Allow installs from this app when Android prompts you (needed for OTA updates)

---

## Installation

1. Download `app-release.apk` from the [latest release](https://github.com/therajatshahare/eva-launcher/releases/latest).
2. Copy it to your TV box (USB drive, a file manager app, or ADB).
3. Install it and set **EVA** as your default home app when prompted.

Using ADB:

```bash
adb connect <tv-ip-address>:5555
adb install -r app-release.apk
```

After the first install, future updates can be done from inside the launcher.

---

## Usage

Open **Launcher Settings** to find:

- **Sync Reddit Wallpapers**: fetches a fresh batch of wallpapers.
- **Check for Launcher Updates**: looks for a newer release and offers to install it.

---

## Building from source

**Prerequisites:** Java 17 and the Android SDK (compile SDK 34).

```bash
git clone https://github.com/therajatshahare/eva-launcher.git
cd eva-launcher
./gradlew assembleRelease
```

On Windows PowerShell, use `./gradlew assembleRelease` as well, or `gradlew.bat assembleRelease` from cmd.

The output is at `app/build/outputs/apk/release/app-release.apk`.

---

## Releasing an update

1. Commit and push your changes:

   ```bash
   git add .
   git commit -m "Describe your changes"
   git push origin main
   ```

2. Tag and push a version:

   ```bash
   git tag v3.3
   git push origin v3.3
   ```

3. GitHub Actions (`.github/workflows/release.yml`) builds, signs and publishes the release APK. Your TV will see it the next time you choose **Check for Launcher Updates**.

> **Note:** the app's `versionName` must match the tag you push (for example `3.3` for `v3.3`), otherwise the updater can't tell a release is newer.

---

## Project structure

```text
eva-launcher/
├── .github/workflows/release.yml     # CI/CD auto-release
├── app/
│   ├── build.gradle                  # namespace, SDK 34, R8 shrinking
│   └── src/main/
│       ├── AndroidManifest.xml       # INTERNET, REQUEST_INSTALL_PACKAGES, UpdateFileProvider
│       ├── java/com/gathin/tvhome/
│       │   ├── LauncherSettingsActivity.java
│       │   ├── WallpaperFetcher.java
│       │   ├── UpdateManager.java
│       │   ├── UpdateFileProvider.java
│       │   └── ...                   # upstream launcher files
│       └── res/
│           ├── layout/activity_launcher_settings.xml
│           └── values/config.xml     # github_update_repo
├── README.md
└── LICENSE
```

---

## Privacy

EVA makes network requests only when you trigger them from settings:

- `reddit.com` for wallpaper sync
- `api.github.com` and GitHub release assets for update checks and downloads

No analytics, no accounts, no tracking.

---

## Contributing

Issues and pull requests are welcome. For larger changes, please open an issue first so we can discuss the approach. Keep the APK small: avoid adding heavy dependencies unless there is a strong reason.

---

## Credits

- **Original project:** [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by **Gathin23**
- **EVA enhancements and maintenance:** **Rajat Shahare**

## License

Released under the [MIT License](LICENSE).
