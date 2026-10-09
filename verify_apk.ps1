$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Dante\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"

$apksigner = "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat"
$releaseApk = "C:\Users\Dante\.gemini\antigravity\scratch\Vaultia\app\build\outputs\apk\release\app-release.apk"

Write-Host "Verifying release APK signing with apksigner..."
& $apksigner verify --verbose $releaseApk
