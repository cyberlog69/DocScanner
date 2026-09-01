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

- **Linux / macOS**:
  ```bash
  ./gradlew assembleRelease
  ```

- **Windows (PowerShell)**:
  ```powershell
  .\gradlew.bat assembleRelease
  ```

**Output Artifact**:
```
app/build/outputs/apk/release/app-release-unsigned.apk
```

---

### 3. Run Unit Tests

Execute the complete multiplatform and Android test suites:

- **Linux / macOS**:
  ```bash
  ./gradlew :shared:allTests :app:testDebugUnitTest
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
