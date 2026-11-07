# Install Cinema Dusk theme to TV via ADB
$targetDir = "/sdcard/Android/data/io.rns.evalauncher/files/"
adb shell "mkdir -p $targetDir/wallpapers"
adb push config.json "$targetDir/config.json"
adb push wallpaper.jpg "$targetDir/wallpaper.jpg"
if (Test-Path "wallpapers") {
    adb push wallpapers/* "$targetDir/wallpapers/"
}
adb shell "am force-stop io.rns.evalauncher"
Write-Host "Cinema Dusk Theme installed successfully!" -ForegroundColor Green
