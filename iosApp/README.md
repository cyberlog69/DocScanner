# DocScanner iOS App

This directory contains the **SwiftUI iOS app** that uses the shared KMP framework.

## ⚠️ Requirements: macOS + Xcode 16+

The iOS app cannot be built on Windows. You need:
1. A Mac with **Xcode 16+** installed.
2. Open iosApp.xcodeproj in Xcode.
3. Run ./gradlew :shared:assembleXCFramework on the Mac to build the shared framework.

## Project Structure

`
iosApp/
├── iosApp.xcodeproj/     ← Xcode project (open this on Mac)
├── iosApp/
│   ├── ContentView.swift          ← Root navigation (Document List)
│   ├── DocumentListView.swift     ← SwiftUI document list screen
│   ├── DocumentDetailView.swift   ← SwiftUI document detail + OCR text
│   ├── ScannerView.swift          ← VNDocumentCameraViewController wrapper
│   ├── SettingsView.swift         ← Settings: OCR language, biometric lock
│   └── SharedBridge.swift         ← Kotlin shared framework import + wiring
`

## Wiring the KMP shared framework

After building with Gradle on Mac:
1. Drag shared.xcframework into the Xcode project.
2. In SharedBridge.swift:
   `swift
   import shared  // the compiled Kotlin shared framework
   `
3. All shared domain models, CategoryClassifier, and OcrResult types are available directly in Swift.
