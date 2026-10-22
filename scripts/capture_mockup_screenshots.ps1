# capture_mockup_screenshots.ps1
# Automates capturing crisp 1080x2400 screenshots from your connected Android phone

$ErrorActionPreference = "Stop"
$outDir = Join-Path $PSScriptRoot "..\mockup_screenshots"
if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
}

$device = (adb devices | Select-String "device$" | Select-Object -First 1) -replace "\s+device", ""
if (!$device) {
    Write-Host "No Android device detected. Please connect your phone with USB debugging enabled." -ForegroundColor Red
    exit 1
}

Write-Host "Connected Device: $device" -ForegroundColor Green
Write-Host "Saving screenshots to: $outDir`n" -ForegroundColor Cyan

function Capture-Screen($fileName, $promptText) {
    Write-Host ">>> $promptText" -ForegroundColor Yellow
    Read-Host "Press [Enter] when ready on your phone screen..."
    $targetPath = Join-Path $outDir $fileName
    cmd /c "adb -s $device exec-out screencap -p > `"$targetPath`""
    Write-Host "Saved: $fileName`n" -ForegroundColor Green
}

Capture-Screen "1_record_screen.png" "Open Black Screen Recorder to the main Record tab"
Capture-Screen "2_oled_stealth_clock.png" "Start recording with the Clock / Pure Black cover active"
Capture-Screen "3_covers_tab.png" "Switch to the Covers tab (showing the covers grid)"
Capture-Screen "4_vault_pin_lock.png" "Open the Vault / Recordings PIN lock screen"
Capture-Screen "5_recordings_gallery.png" "Unlock and view the Recordings list / Player"

Write-Host "All 5 mockup screenshots captured successfully in $outDir!" -ForegroundColor Green
