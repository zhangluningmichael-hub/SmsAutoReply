$projectPath = "E:\SmsAutoReply"
$apkSource = "$projectPath\app\build\outputs\apk\debug\app-debug.apk"
$version = "v7"

Set-Location $projectPath

& "C:\Gradle\gradle-8.5\bin\gradle.bat" assembleDebug --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed"
    exit 1
}

Copy-Item $apkSource "$projectPath\SmsAutoReply-$version.apk" -Force
Write-Host "APK: SmsAutoReply-$version.apk"
