<div align="center">

# 🚀 EVA TV Launcher

<b>A lightweight, blazing-fast, remote-friendly Android TV launcher based on CleanYourTV, featuring live Reddit wallpaper synchronization, GitHub OTA auto-updates, and ultra-low RAM overhead.</b>

<p align="center">
  <a href="https://github.com/Gathin23"><img src="https://img.shields.io/badge/Base-CleanYourTV-blue?style=flat-square" alt="Base Project"></a>
  <a href="https://developer.android.com/about/versions/oreo"><img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-green?style=flat-square" alt="Android Min SDK"></a>
  <a href="https://github.com/therajatshahare/eva-launcher/releases/latest"><img src="https://img.shields.io/badge/APK-~530%20KB-success?style=flat-square" alt="APK Size"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="License"></a>
  <a href="https://github.com/gradle/gradle"><img src="https://img.shields.io/badge/Java-17-orange?style=flat-square" alt="Java 17"></a>
</p>

</div>

---

## 🌟 Key Features

### ⚡ Blazing-Fast & Ultra-Lightweight
- **Zero Bloatware:** Base APK is roughly ~530 KB, built with plain Java and minimal dependencies (no heavy libraries like `androidx.core`).
- **R8 Optimized:** Code stripping and resource shrinking enabled for instantaneous startup and minimal RAM usage on low-spec Smart TV processors.

### 🖼️ Live Reddit Wallpaper Synchronization
- **`r/wallpapers` Integration:** Fetches high-definition landscape artwork directly from Reddit's `r/wallpapers` (`WallpaperFetcher.java`).
- **Smart Rotation & Memory Protection:** Downsamples images in memory to protect low-RAM TV boxes and rotates backgrounds automatically.

### 🔄 GitHub OTA Auto-Updates
- **In-App Updater:** Check for new releases directly from settings (`UpdateManager.java`).
- **Seamless Install:** Downloads and prompts for installation via `UpdateFileProvider`, a custom lightweight `ContentProvider`.

### 🎨 Customizable Grid & Dock
- **Adjustable Layout:** Configure app columns (4 to 7 per row) and customize rows displayed below the main dock.
- **Dock Opacity Controls:** Choose between 100%, 70%, 50%, 30%, or full clean transparency.
- **App Library & Shortcuts:** Fast D-pad navigation with shortcut pinning and app reordering.

### 🕒 Daydream Clock, System Stats & Festive Themes
- **Daydream Screensaver:** Integrated clock screensaver supporting 12-hour, 24-hour, and system time formats.
- **Real-Time System Stats:** Monitor displaying CPU load %, RAM usage, free storage, and active network connections (Wi-Fi, Ethernet, VPN).
- **Dynamic Cards & Widgets:** Race weekend schedule countdowns (F1/Motorsport), game day updates, and adaptive time-of-day greetings.
- **Seasonal Visual Accents:** Dynamic visual celebrations for Diwali, Dussehra, Halloween, and Lunar New Year.
- **Multi-Language Support:** Fully translated into over 30 languages.

---

## ⚙️ How It Works

### Reddit Wallpaper Sync (`WallpaperFetcher.java`)
- Queries `https://www.reddit.com/r/wallpapers/hot.json?limit=25`.
- Sends an Android TV User-Agent to prevent HTTP 403 response blocks.
- Filters out portrait images, gallery posts, and invalid files under 10 KB.
- Saves high-res landscape wallpapers to app storage where `WallpaperPlayer` rotates them automatically.

### OTA Auto-Updates (`UpdateManager.java` & `UpdateFileProvider.java`)
- Queries GitHub Releases API (`https://api.github.com/repos/therajatshahare/eva-launcher/releases/latest`).
- Compares installed `versionName` against the latest GitHub tag.
- Downloads `app-release.apk` to `cacheDir/updates/` and prompts for direct installation without requiring third-party app installers.

---

## 📋 Requirements

- Android TV device running **Android 8.0 (API 26) or higher**.
- Permissions: `INTERNET` and `REQUEST_INSTALL_PACKAGES`.

---

## 📥 Installation

1. Download `app-release.apk` from the [Latest Release](https://github.com/therajatshahare/eva-launcher/releases/latest).
2. Sideload the APK onto your Android TV using ADB or a file manager app (e.g., Downloader).
3. Open **EVA TV Launcher** and optionally set it as your default Home launcher.

Using ADB:
```bash
adb connect <tv-ip-address>:5555
adb install -r app-release.apk
```

---

## 📦 Building from Source

### Prerequisites
- JDK 17
- Android SDK (Compile SDK 34, Min SDK 26)

### Build Commands

**Linux / macOS:**
```bash
./gradlew assembleRelease
```

**Windows (PowerShell):**
```powershell
.\gradlew.bat assembleRelease
```

The output APK will be generated at:
```text
app/build/outputs/apk/release/app-release.apk
```

---

## 🚀 Releasing an Update

1. Commit and push your changes:
   ```bash
   git add .
   git commit -m "Your update description"
   git push origin main
   ```

2. Tag and push a release version:
   ```bash
   git tag v3.2
   git push origin v3.2
   ```

3. GitHub Actions (`.github/workflows/release.yml`) builds and publishes the release APK automatically.

---

## 📁 Project Structure

```text
eva-launcher/
├── .github/workflows/release.yml     # CI/CD auto-release workflow
├── app/
│   ├── build.gradle                  # App configuration, SDK 34, R8 shrinking
│   └── src/main/
│       ├── AndroidManifest.xml       # Permissions, UpdateFileProvider
│       ├── java/com/gathin/tvhome/
│       │   ├── LauncherSettingsActivity.java
│       │   ├── WallpaperFetcher.java
│       │   ├── UpdateManager.java
│       │   ├── UpdateFileProvider.java
│       │   └── ...                   # Launcher core components
│       └── res/                      # UI layouts, fonts, drawables, strings
├── README.md
└── LICENSE
```

---

## 🔒 Privacy

EVA makes network requests only when triggered from Launcher Settings:
- `reddit.com` for fetching wallpapers
- `api.github.com` and GitHub release assets for update checks and downloads

No telemetry, no tracking, and no external advertising services.

---

## 👥 Credits & Acknowledgments

- **Original Base:** Built upon the [CleanYourTV](https://github.com/Gathin23/cleanyourtv) project created by **[Gathin23](https://github.com/Gathin23)**.
- **Custom Enhancements & Maintenance:** Developed and maintained by **Rajat Shahare**.
- **Font:** Uses the [Outfit](https://github.com/outfit-project/outfit-font) typeface under the SIL Open Font License 1.1.

---

## 📄 License

This project is licensed under the **[MIT License](LICENSE)**.
