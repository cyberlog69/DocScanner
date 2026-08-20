<#
.SYNOPSIS
  Builds and installs the DocScanner debug APK to a device over wireless debugging.

.DESCRIPTION
  Helps with the whole flow:
    1. (Optional) Pair a device using the pairing code shown on the phone.
    2. (Optional) Connect to a device over the network.
    3. Build the debug APK with Gradle.
    4. Install the APK onto the connected device.

.EXAMPLE
  .\install.ps1 -Connect 192.168.1.20:37123
  .\install.ps1 -PairWith 192.168.1.20:34567 -PairCode 123456 -Connect 192.168.1.20:37123
  .\install.ps1 -SkipBuild
#>
[CmdletBinding()]
param(
    # Pair with a device first: "ip:port" shown under "Pair device with pairing code".
    [string]$PairWith,

    # The 6-digit pairing code displayed on the phone.
    [string]$PairCode,

    # Connect to an already-paired device: "ip:port" shown on the Wireless debugging screen.
    [string]$Connect,

    # Skip the Gradle build (only install).
    [switch]$SkipBuild,

    # Skip the install step (only build / connect).
    [switch]$SkipInstall,

    # Override adb path. Auto-detected from local.properties / common SDK locations otherwise.
    [string]$AdbPath,

    # Override JDK home. Auto-detected from Android Studio's bundled JBR otherwise.
    [string]$JavaHome
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

# ── 1. Locate adb ───────────────────────────────────────────────────────────
if (-not $AdbPath) {
    $localProps = Join-Path $projectRoot 'local.properties'
    if (Test-Path $localProps) {
        $sdkDir = (Get-Content $localProps | Where-Object { $_ -match '^sdk\.dir=' }) -replace '^sdk\.dir=', '' -replace '\\\\', '\' -replace '\\:', ':'
        $AdbPath = Join-Path $sdkDir 'platform-tools\adb.exe'
    }
}
if (-not $AdbPath -or -not (Test-Path $AdbPath)) {
    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe",
        "C:\Android\Sdk\platform-tools\adb.exe"
    )
    $AdbPath = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $AdbPath) { throw 'adb not found. Pass -AdbPath <path> or set sdk.dir in local.properties.' }
Write-Host "[adb] $AdbPath" -ForegroundColor DarkGray

# ── 2. Locate a JDK for Gradle ──────────────────────────────────────────────
if (-not $SkipBuild) {
    if (-not $JavaHome) {
        $jbr = 'C:\Program Files\Android\Android Studio\jbr'
        $jbrLocal = "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
        $JavaHome = if (Test-Path "$jbr\bin\java.exe") { $jbr }
                    elseif (Test-Path "$jbrLocal\bin\java.exe") { $jbrLocal }
                    else { $env:JAVA_HOME }
    }
    if (-not $JavaHome -or -not (Test-Path "$JavaHome\bin\java.exe")) {
        throw 'JDK not found. Pass -JavaHome <path> or install Android Studio.'
    }
    Write-Host "[java] $JavaHome" -ForegroundColor DarkGray
}

# ── 3. Pair & connect (wireless debugging) ──────────────────────────────────
if ($PairWith) {
    if (-not $PairCode) { throw 'Pairing requires -PairCode <6-digit code>.' }
    & $AdbPath pair $PairWith $PairCode
    if ($LASTEXITCODE -ne 0) { throw 'Pairing failed. Check the code / port and retry.' }
}
if ($Connect) {
    & $AdbPath connect $Connect
    if ($LASTEXITCODE -ne 0) { throw 'adb connect failed.' }
}

# ── 4. Verify a device is online ────────────────────────────────────────────
$deviceLines = (& $AdbPath devices) | Select-Object -Skip 1 | Where-Object { $_ -match '\S' }
$online = $deviceLines | Where-Object { $_ -match '\tdevice\s*$' }
$offline = $deviceLines | Where-Object { $_ -match '\tunauthorized' }
if ($offline) {
    Write-Host 'A device is showing as "unauthorized" — accept the RSA prompt on the phone.' -ForegroundColor Yellow
    exit 1
}
if (-not $online) {
    Write-Host 'No device connected. On the phone:' -ForegroundColor Yellow
    Write-Host '  Settings > Developer options > Wireless debugging' -ForegroundColor Yellow
    Write-Host "  Then run: .\install.ps1 -PairWith <ip:pairing-port> -PairCode <code> -Connect <ip:port>" -ForegroundColor Yellow
    exit 1
}
$device = ($online | Select-Object -First 1) -split "`t" | Select-Object -First 1
Write-Host "[device] $device" -ForegroundColor Green

# ── 5. Build the debug APK ──────────────────────────────────────────────────
$apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not $SkipBuild) {
    Write-Host '[build] Compiling debug APK...' -ForegroundColor Cyan
    $oldJavaHome = $env:JAVA_HOME
    $env:JAVA_HOME = $JavaHome
    try {
        & (Join-Path $projectRoot 'gradlew.bat') assembleDebug --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
    } finally {
        $env:JAVA_HOME = $oldJavaHome
    }
}
if (-not (Test-Path $apk)) { throw "APK not found at $apk. Run the build first." }
Write-Host "[apk]  $apk" -ForegroundColor Green

# ── 6. Install ──────────────────────────────────────────────────────────────
if (-not $SkipInstall) {
    Write-Host '[install] Installing to device...' -ForegroundColor Cyan
    & $AdbPath -s $device install -r $apk
    if ($LASTEXITCODE -ne 0) { throw 'Install failed.' }
    Write-Host '[done] App installed. Launch it from the launcher.' -ForegroundColor Green
} else {
    Write-Host '[skip] Install skipped. APK ready at:'
    Write-Host "       $apk" -ForegroundColor Green
}