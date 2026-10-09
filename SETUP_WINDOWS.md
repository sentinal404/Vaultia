# Vaultia - Windows Terminal Setup & Build Guide (No Android Studio)

This guide documents the complete command-line workflow to configure the build environment and compile Vaultia on Windows without Android Studio.

---

## 1. Prerequisites Overview

| Component | Minimum Version | Verified Version |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11 (64-bit) | Windows 11 |
| **Java Development Kit (JDK)** | JDK 17 | Eclipse Adoptium Temurin 17 (17.0.20.1) |
| **Android SDK Command-line Tools** | 12.0+ | `commandlinetools-win-11076708_latest` |
| **Android SDK Platform** | API 34 | `platforms;android-34` |
| **Android SDK Build Tools** | 34.0.0 | `build-tools;34.0.0` |
| **Build System** | Gradle 8.7+ | Gradle 8.7 (via `gradlew.bat`) |

---

## 2. Step-by-Step Environment Setup

### Step 2.1: Install JDK 17
Open PowerShell and install Eclipse Adoptium Temurin 17 via Windows Package Manager:
```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK -e --silent --accept-package-agreements --accept-source-agreements
```
Or download manually from [Adoptium](https://adoptium.net/).

Verify installation:
```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" -version
```

### Step 2.2: Setup Android SDK Command-line Tools
1. Create SDK directory:
```powershell
New-Item -ItemType Directory -Path "C:\Users\Dante\android-sdk" -Force
```
2. Download official command-line tools:
```powershell
curl.exe -L -o "C:\Users\Dante\android-sdk\cmdline-tools.zip" "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
```
3. Extract into `cmdline-tools\latest`:
```powershell
Expand-Archive -Path "C:\Users\Dante\android-sdk\cmdline-tools.zip" -DestinationPath "C:\Users\Dante\android-sdk\temp_extract"
Move-Item -Path "C:\Users\Dante\android-sdk\temp_extract\cmdline-tools" -Destination "C:\Users\Dante\android-sdk\cmdline-tools\latest"
Remove-Item -Recurse -Force "C:\Users\Dante\android-sdk\temp_extract"
```

### Step 2.3: Configure Environment Variables
Set the following session or user environment variables:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Dante\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"
```

To make them persistent across PowerShell sessions:
```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot", "User")
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\Dante\android-sdk", "User")
```

### Step 2.4: Accept Licenses and Install SDK Components
Run `sdkmanager.bat`:
```powershell
# Accept all SDK licenses automatically
cmd.exe /c "(echo y & echo y & echo y & echo y & echo y & echo y & echo y & echo y) | `"$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat`" --licenses"

# Install Android 14 Platform (API 34) and Build-Tools
& "$env:ANDROID_HOME\cmdline-tools\latest\bin\sdkmanager.bat" "platforms;android-34" "build-tools;34.0.0"
```

### Step 2.5: Configure `local.properties`
In the project directory, ensure `local.properties` points to the Android SDK:
```properties
sdk.dir=C\:\\Users\\Dante\\android-sdk
```

---

## 3. Building the Application

Navigate to the project root:
```powershell
cd C:\Users\Dante\.gemini\antigravity\scratch\Vaultia
```

### 3.1: Clean Build
```powershell
.\gradlew.bat clean
```

### 3.2: Assemble Debug APK
```powershell
.\gradlew.bat assembleDebug
```
The output APK is generated at:
```
app\build\outputs\apk\debug\app-debug.apk
```

### 3.3: Assemble Release APK
```powershell
.\gradlew.bat assembleRelease
```
The release APK is generated at:
```
app\build\outputs\apk\release\app-release-unsigned.apk
```

To sign the release APK with Android `apksigner`:
```powershell
# Generate a release keystore (one-time)
keytool -genkey -v -keystore vaultia.keystore -alias vaultia -keyalg RSA -keysize 2048 -validity 10000

# Sign APK
& "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat" sign --ks vaultia.keystore --out app\build\outputs\apk\release\app-release.apk app\build\outputs\apk\release\app-release-unsigned.apk
```

---

## 4. Installing APK on an Android Device

Ensure USB debugging is enabled on your Android phone, connect via USB, then run:
```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```
