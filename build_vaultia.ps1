$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Dante\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"

Write-Host "Java version:"
& "$env:JAVA_HOME\bin\java.exe" -version

Write-Host "Running Gradle build..."
Set-Location "C:\Users\Dante\.gemini\antigravity\scratch\Vaultia"

cmd.exe /c "gradlew.bat assembleDebug --stacktrace"
