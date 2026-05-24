# 编译 APK 并复制到指定路径（带版本号）

# 设置路径
$projectPath = "E:\SmsAutoReply"
$apkSource = "$projectPath\app\build\outputs\apk\debug\app-debug.apk"
$version = "v3"

# 进入项目目录
Set-Location $projectPath

# 编译
& "C:\Gradle\gradle-8.5\bin\gradle.bat" assembleDebug --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed"
    exit 1
}

# 复制带版本号
Copy-Item $apkSource "$projectPath\SmsAutoReply-$version.apk" -Force
Write-Host "APK: SmsAutoReply-$version.apk"
