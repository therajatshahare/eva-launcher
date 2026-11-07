#!/bin/sh
TARGET_DIR="/sdcard/Android/data/io.rns.evalauncher/files"
adb shell "mkdir -p $TARGET_DIR/wallpapers"
adb push config.json "$TARGET_DIR/config.json"
adb push wallpaper.jpg "$TARGET_DIR/wallpaper.jpg"
if [ -d "wallpapers" ]; then
    adb push wallpapers/* "$TARGET_DIR/wallpapers/"
fi
adb shell "am force-stop io.rns.evalauncher"
echo "Cinema Dusk Theme installed successfully!"
