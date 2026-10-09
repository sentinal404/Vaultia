Write-Host "Waiting for Android mobile device to connect via ADB..." -ForegroundColor Cyan
& "C:\Users\Dante\android-sdk\platform-tools\adb.exe" wait-for-device
Write-Host "Device detected! Installing Vaultia Release APK..." -ForegroundColor Green
& "C:\Users\Dante\android-sdk\platform-tools\adb.exe" install -r "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk"
Write-Host "Launching Vaultia on device..." -ForegroundColor Yellow
& "C:\Users\Dante\android-sdk\platform-tools\adb.exe" shell am start -n com.vaultia.app/.MainActivity
Write-Host "Done! Vaultia is running." -ForegroundColor Green
