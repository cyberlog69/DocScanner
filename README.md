# 🚀 DocScanner - Offline AI Document Scanner

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![iOS](https://img.shields.io/badge/Platform-iOS-blue.svg)](https://developer.apple.com/ios)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform%202.3-7F52FF.svg)](https://www.jetbrains.com/kotlin-multiplatform/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Compose%20%26%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Koin DI](https://img.shields.io/badge/DI-Koin%204.0-EB5424.svg)](https://insert-koin.io/)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20AI-brightgreen.svg)](#-privacy-guarantee)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DocScanner** is a fast, modern, privacy-focused mobile application that transforms your device into an offline AI document scanning workstation. Built with **Kotlin Multiplatform (KMP)** and **Jetpack Compose / SwiftUI**, it provides **Android & iOS** support with 100% on-device AI OCR, automatic edge detection, smart auto-categorization, paper size diagnostics, Wi-Fi printing, and searchable PDF creation.

---

## ✨ Features

### 🔍 Core Scanning & OCR
- 📸 **Smart Document Detection & Edge Correction**: Automatically detects document boundaries, crops, flattens, and enhances scans (Google Document Scanner on Android, Apple VisionKit on iOS).
- 🌍 **Multi-Language On-Device AI OCR**: Offline text recognition supporting **Latin, Devanagari (Hindi/Sanskrit/Marathi/Nepali), Chinese, Japanese, and Korean** (Google ML Kit on Android, Apple Vision on iOS).
- 📄 **Searchable & UHD PDF Generation**: Generates searchable PDFs with invisible OCR text overlay. Supports **Standard (150 DPI)**, **High (300 DPI)**, and **Ultra HD (600 DPI Archival)** exports.
- 📋 **One-Tap Copy & Share All OCR Text**: Instant "Copy Page", multi-page "Copy All", and native "Share Text" sheet with tactile haptic feedback.
- 🔍 **Pinch-to-Zoom & Pan Page Lightbox**: Fullscreen interactive page viewer with up to 5x zoom, double-tap zoom toggle, and smooth boundary clamping.

### 📊 Intelligence & Diagnostics
- 🏷️ **Smart Auto-Categorization**: Offline heuristic classifier automatically identifies Invoices, Receipts, ID Cards, Contracts, Notes, and Books from OCR text.
- 📏 **Document Size & Paper Format Diagnostics**: Detects standard formats (**A4**, **US Letter**, **US Legal**, **ID / Credit Card**, **Receipt Roll**) with pixel dimensions, megapixels, and effective DPI fidelity.
- 🔖 **Tags & Custom Labels**: Organize scans with custom searchable `#tags` and filter chips.
- ⚡ **Instant Full-Text Search (FTS4)**: SQLite-powered tokenized full-text search indexing across document titles, extracted OCR body text, and custom tags.

### 🖨️ Sharing, Exporting & Workflow
- 🖨️ **Local Wi-Fi & Virtual PDF Printing**: Stream generated high-resolution PDF pages directly to network printers over Wi-Fi (HP, Canon, Epson, Brother) or print to PDF.
- 📦 **Batch Operations & ZIP Export**: Long-press to enter multi-select mode. Batch merge, batch categorize, batch delete, or export multiple PDFs into a single ZIP archive.
- 📤 **Bulk Import from Gallery**: Select and import multiple photos or document images from device storage directly into the OCR pipeline.
- 📌 **Document Pinning & Sorting**: Pin important documents to the top; sort by Date (Newest/Oldest), Title (A-Z / Z-A), or Page count.

### 🔐 Security, Polish & System
- 🔐 **App Lock & Biometric Authentication**: Protect sensitive documents with Fingerprint, Face ID / Touch ID, or device PIN/pattern (100% on-device).
- 🔔 **Tactile Haptic Feedback**: Subtle, satisfying vibration on camera scan complete, page rotate (90°), pin toggle, and copy actions.
- 🚀 **Native In-App Update System**: Automatically checks GitHub Releases for new versions, shows markdown changelogs, downloads APKs with real-time progress, and launches the native package installer.
- 🌗 **App Theme & Dark Mode**: System Default, Light, and Dark mode manual override with Material 3 dynamic theming and Android 13+ themed app icons.

---

## 🏛️ Multiplatform Architecture

```
DocScanner
├── shared/                         # Kotlin Multiplatform Shared Core (KMP)
│   ├── src/commonMain/             # Shared domain models, Result hierarchy, metrics, classifier & DI
│   │   └── kotlin/com/example/docscanner/
│   │       ├── model/              # Document, Page, CategoryClassifier, SortOrder, DocumentMetrics, AppUpdateInfo, ScannerResult
│   │       ├── repository/         # DocumentRepository interface abstraction
│   │       ├── di/                 # SharedModule (Koin Multiplatform DI)
│   │       └── bridge/             # expect class: PlatformOcrEngine, PlatformBiometrics, PlatformPdfGenerator, PlatformStorage
│   ├── src/commonTest/             # Multiplatform Unit Test Suite (100% pass rate)
│   ├── src/androidMain/            # Android actual implementations (ML Kit, BiometricPrompt, iText 7)
│   └── src/iosMain/                # iOS actual implementations (Apple VisionKit, Vision OCR, LocalAuth, PDFKit)
├── app/                            # Android Application module (Jetpack Compose UI & Koin DI)
├── iosApp/                         # iOS Application (SwiftUI host embedding KMP framework)
└── .github/workflows/build.yml     # Automated multiplatform CI workflow compiling Android APK + iOS IPA
```

---

## 🛠️ Tech Stack & Platform Mappings

| Component | Android (`androidMain` / `app`) | iOS (`iosMain` / `iosApp`) |
|---|---|---|
| **Language** | Kotlin 2.3.20 | Kotlin / Native 2.3.20 & Swift |
| **Dependency Injection** | **Koin 4.0.2** (`koin-android`, `koin-androidx-compose`) | **Koin 4.0.2** (`koin-core`) |
| **Document Scanner** | Google Play Services Document Scanner | Apple **VisionKit** (`VNDocumentCameraViewController`) |
| **Offline OCR Engine**| Google ML Kit Text Recognition v2 | Apple **Vision Framework** (`VNRecognizeTextRequest`) |
| **App Lock / Security**| AndroidX Biometric (`BiometricPrompt`) | Apple **LocalAuthentication** (Face ID / Touch ID) |
| **PDF Generation** | iText 7 (Core + Bouncy Castle) | Apple **PDFKit** (`PDFDocument` / `PDFPage`) |
| **Printing** | Android Native `PrintManager` & `PrintDocumentAdapter` | iOS `UIPrintInteractionController` |
| **File Sandbox** | Android Context `filesDir` & `FileProvider` | Apple Foundation `NSFileManager` Documents |
| **Image Loading** | Coil Compose | UIKit `UIImage` / SwiftUI `AsyncImage` |
| **UI Framework** | Jetpack Compose (Material 3) | SwiftUI & Compose Multiplatform |

---

## 📥 Downloads & Installation

Download pre-built packages from the [GitHub Releases](https://github.com/cyberlog69/DocScanner/releases) section:

### 🤖 Android (`.apk`)
- **`DocScanner-Android-v1.6.0.apk`**: Full standalone package with bundled offline ML Kit models.
- **Install via ADB**:
  ```powershell
  adb install -r DocScanner-Android-v1.6.0.apk
  ```

### 🍏 iOS (`.ipa` & Framework)
- **`DocScanner-iOS-v1.6.0.ipa`**: Signed package ready for sideloading on physical iOS devices (iOS 16.0+) via **AltStore**, **Sideloadly**, or **Xcode Devices & Simulators**.
- **`DocScannerKit.framework`**: Compiled multiplatform binary framework for Xcode projects.

---

## 🔒 Privacy Guarantee

DocScanner is designed from the ground up for strict offline privacy:
- All document scanning, OCR text extraction, image processing, categorization, and PDF generation execute **100% locally on your device**.
- Network access is strictly isolated to querying the public GitHub Releases API when the user explicitly taps "Check for Updates" or if automatic update checking is enabled in Settings.

---

## 📜 License & Third-Party Notices

This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.

### Third-Party Libraries & Notices
- **iText 7/8 Core (`com.itextpdf:itext-core`)**: Distributed under the **GNU AGPLv3** open-source license and available under commercial licensing from Apryse/iText Group NV. If you build or distribute commercial derivative works without making source available under AGPLv3, an iText commercial license is required.
- **Google ML Kit**: Distributed under Google APIs Terms of Service and Apache 2.0 components.
- **Bouncy Castle Adapter**: Distributed under the Bouncy Castle License (MIT-derivative).
- **Apple Vision / VisionKit / PDFKit / LocalAuthentication**: Native iOS system frameworks provided under Apple Developer Agreement.
