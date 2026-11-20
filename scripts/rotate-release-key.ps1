# Rotates the DocScanner release key with an APK Signature Scheme v3 lineage.
#
# The retired key stays in the chain so existing installs update in place:
#   * API 28+ follows the lineage and adopts the new key.
#   * API 24-27 verify the v2 signature made with the retired key.
#
# The retired keystore is recovered from git history when it is missing or was
# written by PowerShell's ">" redirection (which corrupts binary data).
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts/rotate-release-key.ps1 -OldStorePassword '<password>'

param(
    [Parameter(Mandatory = $true)][string]$OldStorePassword,
    [string]$OldKeystore = "old.keystore",
    [string]$OldKeystoreGitRef = "c3dfc9c:app/docscanner-signing.keystore",
    [string]$Alias = "docscanner",
    [string]$NewKeystore = "app/release.keystore",
    [string]$LineagePath = "app/release-lineage.bin",
    [string]$Dname = "CN=DocScanner, OU=Mobile, O=DocScanner, L=Unknown, ST=Unknown, C=IN"
)

$ErrorActionPreference = "Stop"

function Find-JavaHome {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $candidates += @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
        (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr"),
        (Join-Path $env:LOCALAPPDATA "Programs\Android Studio1\jbr")
    )
    $jdks = Join-Path $env:USERPROFILE ".jdks"
    if (Test-Path $jdks) {
        $candidates += Get-ChildItem $jdks -Directory |
            Sort-Object { $_.Name } -Descending |
            ForEach-Object { $_.FullName }
    }
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path (Join-Path $candidate "bin\keytool.exe"))) { return $candidate }
    }
    $command = Get-Command keytool -ErrorAction SilentlyContinue
    if ($command) { return (Split-Path -Parent (Split-Path -Parent $command.Source)) }
    return $null
}

function Find-AndroidSdk {
    $candidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
    if (Test-Path "local.properties") {
        $match = Select-String -Path "local.properties" -Pattern '^sdk\.dir=' | Select-Object -First 1
        if ($match) { $candidates += ($match.Line -replace '^sdk\.dir=', '') }
    }
    $candidates += @(
        (Join-Path $env:LOCALAPPDATA "Android\Sdk"),
        (Join-Path $env:USERPROFILE "AppData\Local\Android\Sdk")
    )
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path (Join-Path $candidate "build-tools"))) { return $candidate }
    }
    return $null
}

# PKCS12/JKS keystores are binary; a valid one starts with an ASN.1 SEQUENCE.
function Test-Keystore([string]$Path) {
    if (-not (Test-Path $Path)) { return $false }
    try {
        $stream = [System.IO.File]::OpenRead($Path)
        try { $first = $stream.ReadByte() } finally { $stream.Close() }
        return ($first -eq 0x30)
    } catch {
        return $false
    }
}

# Writes the exact git blob bytes; PowerShell's ">" would re-encode them as UTF-16.
function Restore-KeystoreFromGit([string]$GitRef, [string]$Destination) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = "git"
    $startInfo.Arguments = "cat-file blob $GitRef"
    $startInfo.RedirectStandardOutput = $true
    $startInfo.UseShellExecute = $false
    $process = [System.Diagnostics.Process]::Start($startInfo)
    try {
        $output = $process.StandardOutput.BaseStream
        $file = [System.IO.File]::Create($Destination)
        try { $output.CopyTo($file) } finally { $file.Close() }
    } finally {
        $process.WaitForExit()
    }
    if ($process.ExitCode -ne 0) { throw "git cat-file failed for $GitRef" }
}

$javaHome = Find-JavaHome
if (-not $javaHome) {
    throw "No JDK found. Set JAVA_HOME or install one under $env:USERPROFILE\.jdks."
}
$env:JAVA_HOME = $javaHome
$keytool = Join-Path $javaHome "bin\keytool.exe"

$sdk = Find-AndroidSdk
if (-not $sdk) {
    throw "Android SDK not found. Set ANDROID_HOME or add sdk.dir to local.properties."
}

$buildTools = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
    Where-Object { $_.Name -match '^\d+(\.\d+){1,3}$' } |
    Sort-Object { [version]$_.Name } -Descending |
    Select-Object -First 1
if (-not $buildTools) { throw "No build-tools found under $sdk" }

$apksigner = Join-Path $buildTools.FullName "apksigner.bat"
if (-not (Test-Path $apksigner)) { $apksigner = Join-Path $buildTools.FullName "apksigner" }

if (-not (Test-Keystore $OldKeystore)) {
    Write-Host "Recovering the retired keystore from $OldKeystoreGitRef"
    Restore-KeystoreFromGit $OldKeystoreGitRef $OldKeystore
    if (-not (Test-Keystore $OldKeystore)) {
        throw "$OldKeystore is not a valid keystore after recovery from $OldKeystoreGitRef"
    }
}

if (Test-Path $NewKeystore) { throw "$NewKeystore already exists. Move it aside first." }
if (Test-Path $LineagePath) { throw "$LineagePath already exists. Move it aside first." }

$bytes = New-Object byte[] 24
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$newPassword = [Convert]::ToBase64String($bytes).Replace("+", "-").Replace("/", "_").TrimEnd("=")

$newKeystoreDir = Split-Path -Parent $NewKeystore
if ($newKeystoreDir) { New-Item -ItemType Directory -Force -Path $newKeystoreDir | Out-Null }

& $keytool -genkeypair `
    -keystore $NewKeystore -alias $Alias `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $newPassword -keypass $newPassword -dname $Dname
if ($LASTEXITCODE -ne 0) { throw "keytool failed with exit code $LASTEXITCODE" }

& $apksigner rotate --out $LineagePath `
    --old-signer --ks $OldKeystore --ks-key-alias $Alias --ks-pass "pass:$OldStorePassword" `
    --new-signer --ks $NewKeystore --ks-key-alias $Alias --ks-pass "pass:$newPassword"
if ($LASTEXITCODE -ne 0) { throw "apksigner rotate failed with exit code $LASTEXITCODE" }

$properties = @(
    "# Generated by scripts/rotate-release-key.ps1. Never commit this file.",
    "storeFile=$NewKeystore",
    "storePassword=$newPassword",
    "keyAlias=$Alias",
    "keyPassword=$newPassword"
) -join "`n"
Set-Content -Path "keystore.properties" -Value $properties -Encoding UTF8

$oldKeystoreFull = (Resolve-Path $OldKeystore).Path
$newKeystoreFull = (Resolve-Path $NewKeystore).Path
$lineageFull = (Resolve-Path $LineagePath).Path

Write-Host ""
Write-Host "New keystore: $NewKeystore"
Write-Host "Lineage:      $LineagePath"
Write-Host "Password:     $newPassword"
Write-Host ""
Write-Host "Back up the keystore, the lineage and the password. Losing them blocks future updates."
Write-Host "Delete $OldKeystore once its secret is uploaded; that key is compromised."
Write-Host ""
Write-Host "Upload the signing secrets:"
Write-Host "  [Convert]::ToBase64String([IO.File]::ReadAllBytes('$newKeystoreFull')) | gh secret set DOCSCANNER_KEYSTORE_BASE64"
Write-Host "  [Convert]::ToBase64String([IO.File]::ReadAllBytes('$oldKeystoreFull')) | gh secret set DOCSCANNER_OLD_KEYSTORE_BASE64"
Write-Host "  [Convert]::ToBase64String([IO.File]::ReadAllBytes('$lineageFull')) | gh secret set DOCSCANNER_LINEAGE_BASE64"
Write-Host "  gh secret set DOCSCANNER_KEY_ALIAS --body '$Alias'"
Write-Host "  gh secret set DOCSCANNER_OLD_STORE_PASSWORD --body '<old keystore password>'"
Write-Host "  gh secret set DOCSCANNER_STORE_PASSWORD --body '$newPassword'"
Write-Host "  gh secret set DOCSCANNER_KEY_PASSWORD --body '$newPassword'"
