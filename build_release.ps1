$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "C:\Users\Dante\android-sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:PATH"

Set-Location "C:\Users\Dante\.gemini\antigravity\scratch\Vaultia"

cmd.exe /c "gradlew.bat assembleRelease --stacktrace"
