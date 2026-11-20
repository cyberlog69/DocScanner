# Building DocScanner from Source

This guide provides step-by-step instructions for compiling and building **DocScanner** for both **Android** and **iOS** directly from the command line on **Windows**, **macOS**, and **Linux**.

---

## 📋 Prerequisites

### 1. Java Development Kit (JDK 17+)
DocScanner requires **JDK 17** or higher.
- **Verify installation**:
  ```bash
  java -version
  ```
- **Set `JAVA_HOME` environment variable**:
  - **macOS / Linux**:
    ```bash
    export JAVA_HOME=$(/usr/libexec/java_home -v 17) # macOS
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 # Ubuntu/Debian
    ```
  - **Windows (PowerShell)**:
    ```powershell
    $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
    ```

### 2. Android SDK
Ensure you have the Android SDK installed with:
- **Compile SDK**: `36`
- **Min SDK**: `24` (Android 7.0+)
- **Build Tools**: `36.0.0` or higher

Create a `local.properties` file in the project root if it does not already exist:
```properties
# macOS / Linux
sdk.dir=/Users/<username>/Library/Android/sdk

# Windows
sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
```

---

## 🤖 Building the Android App (CLI)

The Android project is built using Gradle.

### 1. Build Debug APK

- **Linux / macOS**:
  ```bash
  chmod +x gradlew
  ./gradlew assembleDebug --stacktrace
  ```

- **Windows (PowerShell / Command Prompt)**:
  ```powershell
  .\gradlew.bat assembleDebug --stacktrace
  ```

**Output Artifact**:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

### 2. Build Release APK

Release builds need a signing key. Provide it either through a
`keystore.properties` file at the repository root (git-ignored, see
`keystore.properties.example`) or through environment variables:

| Property | Environment variable |
|---|---|
| `storeFile` | `DOCSCANNER_KEYSTORE` |
| `storePassword` | `DOCSCANNER_STORE_PASSWORD` |
| `keyAlias` | `DOCSCANNER_KEY_ALIAS` |
| `keyPassword` | `DOCSCANNER_KEY_PASSWORD` |

Without those values the release build still runs but produces an **unsigned**
APK. Pull-request and `main`-branch builds stay unsigned; only `v*` tag pushes
are signed and published.

### Automatic signing and publishing on tag pushes

Pushing a `v*` tag runs the `Publish Signed Release` job in
`.github/workflows/build-ios.yml`. It builds an **unsigned** APK, page-aligns it,
then signs with both the retired and the current key plus a v3
`SigningCertificateLineage`, verifies the result, and publishes the GitHub
release with `releases/<tag>/release_notes.md` as the body. Gradle cannot attach
a lineage, which is why signing happens with `apksigner` after assembly.

Dual signing is what lets existing users update in place after a key change:

- **Android 9+ (API 28+)** follows the v3 lineage and adopts the new key.
- **Android 7.0-8.1 (API 24-27)** ignore the v3 block and verify the v2 signature
  made with the retired key, which still matches apps installed before the
  rotation.

Add these secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `DOCSCANNER_KEYSTORE_BASE64` | Base64 of the current keystore |
| `DOCSCANNER_STORE_PASSWORD` | Current keystore password |
| `DOCSCANNER_KEY_PASSWORD` | Current key password |
| `DOCSCANNER_KEY_ALIAS` | Key alias (`docscanner`) |
| `DOCSCANNER_OLD_KEYSTORE_BASE64` | Base64 of the retired keystore |
| `DOCSCANNER_OLD_STORE_PASSWORD` | Retired keystore password |
| `DOCSCANNER_LINEAGE_BASE64` | Base64 of the generated `lineage.bin` |

`scripts/rotate-release-key.ps1` prints all of these commands after it runs:

```powershell
# Windows
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/release.keystore")) | Set-Clipboard
gh secret set DOCSCANNER_KEYSTORE_BASE64 --body "$(Get-Clipboard)"
```

```bash
# macOS / Linux
base64 -w0 app/release.keystore | gh secret set DOCSCANNER_KEYSTORE_BASE64
```

The keystores and the lineage must never be committed; only their Base64 forms
belong in repository secrets. If any secret is missing the job fails instead of
publishing an unsigned APK.

### Rotating the release key

An earlier release keystore was committed to this repository and must be treated
as compromised. `scripts/rotate-release-key.ps1` (Windows) and
`scripts/rotate-release-key.sh` (macOS/Linux) generate a new 4096-bit key and the
lineage that links it to the retired key, so existing installs keep updating.

The retired keystore is recovered from git history automatically when it is
missing or was written by PowerShell's `>` redirection, which corrupts binary
data. The script also locates the JDK and Android SDK on its own.

```powershell
# Windows
powershell -ExecutionPolicy Bypass -File scripts/rotate-release-key.ps1 `
  -OldStorePassword '<old password>'
```

```bash
# macOS / Linux
./scripts/rotate-release-key.sh old.keystore '<old password>'
```

The script writes `app/release.keystore`, `app/release-lineage.bin` and a
git-ignored `keystore.properties`, then prints the `gh secret set` commands. Back
up the keystore, the lineage and the password: losing them blocks future updates.
Delete `old.keystore` once its secret is uploaded; that key is compromised.

To rotate again later, extend the existing lineage rather than starting over:

```bash
apksigner rotate --out lineage-next.bin --in lineage.bin \
  --old-signer --ks current.keystore --ks-key-alias docscanner \
  --new-signer --ks next.keystore --ks-key-alias docscanner
```

Keep signing releases with the **oldest** key first (for API 24-27) and the
**newest** key second (for API 28+), passing the extended lineage.

> **Residual risk on old devices**: because the retired private key is public,
> anyone can still sign an APK that API 24-27 devices would accept as an update.
> Modern devices are protected once they install a lineage-bearing release.
> Raise `minSdk` to 28 to drop the retired key entirely.

### Building a release locally

Local builds sign with the current key only (no lineage), so the APK installs
cleanly but is not update-compatible with official builds.

- **Linux / macOS**:
  ```bash
  ./gradlew assembleGithubRelease
  ```

- **Windows (PowerShell)**:
  ```powershell
  .\gradlew.bat assembleGithubRelease
  ```

**Output Artifact**:
```
app/build/outputs/apk/github/release/app-github-release.apk        # signed
app/build/outputs/apk/github/release/app-github-release-unsigned.apk  # unsigned
```

---

### 3. Run Unit Tests

Execute the complete multiplatform and Android test suites:

- **Linux / macOS**:
  ```bash
  ./gradlew :shared:testAndroidHostTest :app:testGithubDebugUnitTest
  ```

- **Windows (PowerShell)**:
  ```powershell
  .\gradlew.bat :shared:allTests :app:testDebugUnitTest
  ```

---

### 4. Install onto Connected Android Device (ADB)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🍏 Building the iOS App (CLI on macOS)

The iOS app combines a Kotlin Multiplatform (KMP) shared framework (`DocScannerKit`) with a native SwiftUI host application.

### Prerequisites for iOS:
- **macOS** with **Xcode 15.0+** or **Xcode 16.0+** and Command Line Tools installed.
- Run `xcode-select --install` to verify Xcode CLI tools.

---

### 1. Compile the Kotlin Native iOS Framework

#### For iOS Simulator (Apple Silicon `arm64`):
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 --stacktrace
```
*Output*: `shared/build/bin/iosSimulatorArm64/debugFramework/DocScannerKit.framework`

#### For Physical iOS Device (`arm64`):
```bash
./gradlew :shared:linkReleaseFrameworkIosArm64 --stacktrace
```
*Output*: `shared/build/bin/iosArm64/releaseFramework/DocScannerKit.framework`

---

### 2. Compile and Package the Sideloadable `.ipa`

Run the following command sequence in your terminal to build the Mach-O binary, compile asset catalogs, and package the `.ipa`:

```bash
# 1. Create clean build structure
mkdir -p build/ios/Payload/DocScanner.app

# 2. Copy and configure Info.plist
cp iosApp/iosApp/Info.plist build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundleIdentifier -string "com.example.docscanner" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundleExecutable -string "DocScanner" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundleName -string "DocScanner" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundleDisplayName -string "DocScanner" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundlePackageType -string "APPL" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundleDevelopmentRegion -string "en" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundleShortVersionString -string "1.6.0" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace CFBundleVersion -string "6" build/ios/Payload/DocScanner.app/Info.plist
plutil -replace MinimumOSVersion -string "16.0" build/ios/Payload/DocScanner.app/Info.plist

# 3. Compile Swift sources + statically link DocScannerKit into the single executable Mach-O binary
SDK_PATH=$(xcrun --sdk iphoneos --show-sdk-path)

swiftc -sdk $SDK_PATH \
  -target arm64-apple-ios16.0 \
  -F shared/build/bin/iosArm64/releaseFramework \
  -framework UIKit \
  -framework SwiftUI \
  -framework VisionKit \
  -framework Vision \
  -framework LocalAuthentication \
  -framework PDFKit \
  -framework DocScannerKit \
  -o build/ios/Payload/DocScanner.app/DocScanner \
  iosApp/iosApp/iOSApp.swift \
  iosApp/iosApp/ContentView.swift \
  iosApp/iosApp/ScannerView.swift \
  iosApp/iosApp/DocumentListView.swift \
  iosApp/iosApp/DocumentDetailView.swift \
  iosApp/iosApp/SettingsView.swift \
  iosApp/iosApp/OcrService.swift

# 4. Compile AppIcon Assets into Assets.car
cp iosApp/iosApp/*.png build/ios/Payload/DocScanner.app/ 2>/dev/null || true
xcrun actool iosApp/iosApp/Assets.xcassets \
  --compile build/ios/Payload/DocScanner.app \
  --platform iphoneos \
  --minimum-deployment-target 16.0 \
  --app-icon AppIcon \
  --output-partial-info-plist build/ios/partial-info.plist 2>/dev/null || true

# 5. Clean any detached signatures and package .ipa archive
rm -rf build/ios/Payload/DocScanner.app/_CodeSignature
cd build/ios
zip -r DocScanner.ipa Payload
```

**Output Artifact**:
```
build/ios/DocScanner.ipa
```

---

### 3. Sideloading the iOS App

The generated `DocScanner.ipa` can be installed on non-jailbroken iOS devices using:
- **Sideloadly**: Drag and drop `DocScanner.ipa`, enter your Apple ID, and click **Start**.
- **AltStore**: Open `DocScanner.ipa` via the AltStore app on your device.
- **Xcode Devices & Simulators**: Connect device $\rightarrow$ `Window` $\rightarrow$ `Devices and Simulators` $\rightarrow$ click `+` under **Installed Apps**.

---

## 🛠️ Common Troubleshooting

### 1. `JAVA_HOME is not set`
Set `JAVA_HOME` to your JDK 17 installation directory before running Gradle.

### 2. Gradle Permission Denied on Linux/macOS
If `./gradlew` fails with permission denied, grant execute permission:
```bash
chmod +x gradlew
```

### 3. Missing Android SDK Location
Ensure `local.properties` exists in the repository root with `sdk.dir` pointing to your Android SDK directory.
