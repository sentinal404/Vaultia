# Vaultia - Private Telegram Cloud Vault for Android

![Vaultia Banner](app/src/main/res/drawable/ic_launcher_foreground.xml)

**Vaultia** is a modern, privacy-first personal cloud and media vault application for Android that uses a private Telegram Channel and the official Telegram Bot API as an infinite, decentralized, and personal remote storage backend.

---

## Key Highlights

- **Private Telegram Channel as Remote Storage**: Files are encrypted, chunked, and stored safely in your personal private Telegram channel. No public channels.
- **Enterprise-Grade Security**: Bot tokens and security PINs are protected with **Android Keystore** and **EncryptedSharedPreferences (AES-256 GCM)**. No credentials are ever hardcoded or uploaded to external servers.
- **Chunking Engine**: Arbitrarily large files are split into streamable, verified chunks (default 20MB, configurable 5MB–45MB) with SHA-256 checksums per chunk and for the whole file. No large files are ever loaded into RAM at once.
- **Resumable Parallel Uploads & Downloads**: If a transfer is interrupted by network failure or app restart, only missing chunks are transferred.
- **Worker Bot System**: Add multiple secondary Telegram bots as admins in the same channel to distribute traffic and handle Telegram Bot API rate limits gracefully.
- **On-Demand Streaming Media**: Play multi-gigabyte videos and music instantly without downloading the whole file first, powered by an internal localhost HTTP Range streaming engine.
- **2026 Soft Claymorphism UI**: Beautiful tactile soft depth, smooth rounded cards, subtle shadows, and light/dark theme support.
- **Pure Command-Line Build**: Built 100% from Windows PowerShell / Terminal without Android Studio.

---

## Architecture Overview

```
Presentation Layer (Jetpack Compose + Soft Claymorphism)
       │
       ▼
ViewModel Layer (VaultViewModel, TransferViewModel, PlayerViewModel, SettingsViewModel)
       │
       ▼
Domain & Engine Layer (ChunkEngine, UploadManager, DownloadManager, WorkerScheduler, MediaStreamingServer)
       │
       ├──► Local Storage: Room Database (Files, Chunks, Transfers, Workers) + Android Keystore
       └──► Remote Backend: Telegram Bot API (getMe, getChat, getChatMember, sendDocument, getFile)
```

---

## Feature Matrix

| Feature | Description |
| :--- | :--- |
| **Startup Validation** | Animated splash, token verification, admin permission check, metadata recovery, and offline fallback. |
| **Onboarding Guide** | Step-by-step tutorial for BotFather, channel creation, and admin promotion with live verification checklist. |
| **Resumable Transfers** | Room-backed chunk status tracking. Skip finished chunks on resume. Exponential backoff retry. |
| **Video Player** | Media3/ExoPlayer video streaming, seek bar, 10s skip/rewind, speed control (0.5x–2.0x), buffering indicator. |
| **Audio Player** | Background audio playback via `MediaSessionService`, animated rotating disc, track progress. |
| **Image Viewer** | Zoom, pan, and double-tap gestures with high-resolution rendering and download to device. |
| **Search & Filters** | Instant search with category filters: Videos, Images, Audio, Documents, Archives. |
| **App Lock** | Optional PIN keypad and Biometric authentication before unlocking the vault. |
| **Multi-Worker Bots** | Concurrency distribution across up to 5 worker bots with rate-limit tracking. |

---

## Windows Terminal Build Instructions (No Android Studio)

### 1. Prerequisites
- **JDK 17** (Temurin 17 installed)
- **Android SDK Platform 34** & **Build-Tools 34.0.0**
- Environment variables:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Dante\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"
```

### 2. Build Debug APK
```powershell
cd C:\Users\Dante\.gemini\antigravity\scratch\Vaultia
.\gradlew.bat assembleDebug
```
The APK is generated at:
```
app\build\outputs\apk\debug\app-debug.apk
```

### 3. Build Release APK
```powershell
.\gradlew.bat assembleRelease
```
The release APK is generated at:
```
app\build\outputs\apk\release\app-release-unsigned.apk
```

---

## Privacy & Security Policy

1. **Tokens Never Hardcoded**: All bot tokens are inputted exclusively by the user.
2. **Device-Only Storage**: Secrets are stored locally using Android Keystore cryptographic keys.
3. **No Central Server**: Vaultia does not operate any analytics server, tracking server, or intermediary proxy. All communication is strictly direct between your Android device and `https://api.telegram.org`.
4. **Wipe Configuration**: Users can tap **Settings → Telegram → Delete Configuration** at any time to purge all stored keys and metadata.
