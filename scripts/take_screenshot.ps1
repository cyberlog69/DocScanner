param(
    [string]$Name = ""
)

$destDir = "fastlane\metadata\android\en-US\images\phoneScreenshots"
$buildsDir = "..\builds\screenshots"

if (!(Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
if (!(Test-Path $buildsDir)) { New-Item -ItemType Directory -Path $buildsDir -Force | Out-Null }

if ([string]::IsNullOrWhiteSpace($Name)) {
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $filename = "screenshot_$timestamp.png"
} else {
    $cleanName = $Name -replace '[^a-zA-Z0-9_\-]', '_'
    $filename = "$cleanName.png"
}

$destPath1 = Join-Path $destDir $filename
$destPath2 = Join-Path $buildsDir $filename

Write-Host "Capturing screenshot from connected ADB device..." -ForegroundColor Cyan
adb shell screencap -p /sdcard/docscanner_temp_screen.png
adb pull /sdcard/docscanner_temp_screen.png "$destPath1" | Out-Null
adb shell rm /sdcard/docscanner_temp_screen.png

Copy-Item "$destPath1" "$destPath2" -Force

Write-Host " Screenshot saved to:" -ForegroundColor Green
Write-Host "  1. $destPath1" -ForegroundColor Yellow
Write-Host "  2. $destPath2" -ForegroundColor Yellow
