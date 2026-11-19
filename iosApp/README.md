# DocScanner iOS App

SwiftUI sources for the iOS app. They currently implement scanning, OCR text
recognition, and PDF export natively in Swift; they do **not** import the KMP
shared framework yet (`SettingsView.swift` only contains a commented-out
`import`). Closing that gap is tracked in [ROADMAP.md](../ROADMAP.md).

## ⚠️ Requirements: macOS + Xcode 16+

The iOS app cannot be built on Windows. You need a Mac with Xcode 16+ and the
iOS SDK installed.

## Project Layout

```
iosApp/
└── iosApp/
    ├── iOSApp.swift               ← @main entry point
    ├── ContentView.swift          ← Root navigation
    ├── DocumentListView.swift     ← Document list screen
    ├── DocumentDetailView.swift   ← Document detail + OCR text
    ├── ScannerView.swift          ← VNDocumentCameraViewController wrapper
    ├── SettingsView.swift         ← Settings: OCR language, biometric lock
    └── OcrService.swift           ← Apple Vision OCR
```

There is no committed `.xcodeproj`. CI compiles the Swift sources directly with
`swiftc` and links the Kotlin framework; see
`.github/workflows/build-ios.yml` (`build-ios` job).

## Building

Build the Kotlin framework first (the framework name is `DocScannerKit`, not
`shared`):

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64   # simulator
./gradlew :shared:linkReleaseFrameworkIosArm64          # device
```

Then compile the app, linking the framework from
`shared/build/bin/iosArm64/releaseFramework`:

```bash
SDK_PATH=$(xcrun --sdk iphoneos --show-sdk-path)
swiftc -sdk "$SDK_PATH" -target arm64-apple-ios16.0 \
  -F shared/build/bin/iosArm64/releaseFramework \
  -framework DocScannerKit \
  -o DocScanner \
  iosApp/iosApp/*.swift
```

The result is an unsigned binary; AltStore, Sideloadly, or Xcode re-sign it
during installation.

## Wiring the shared framework (not done yet)

When the shared core is adopted:

1. Add `import DocScannerKit` to the Swift files that need shared types.
2. Regenerate the framework after changing `shared/`.
3. Verify with `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64`.
