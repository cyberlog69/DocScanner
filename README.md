# 🚀 DocScanner - Offline AI Document Scanner

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![iOS](https://img.shields.io/badge/Platform-iOS-blue.svg)](https://developer.apple.com/ios)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform%202.3-7F52FF.svg)](https://www.jetbrains.com/kotlin-multiplatform/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Compose%20%26%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-brightgreen.svg)](#-privacy-guarantee)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DocScanner** is a fast, modern, privacy-focused application that transforms your mobile device into an offline AI document scanning workstation. Built with **Kotlin Multiplatform (KMP)** and **Compose**, it supports both **Android & iOS** with 100% on-device AI OCR, document edge detection, smart auto-categorization, and searchable PDF creation—all completely offline without connecting to the cloud.

---

## ✨ Features

- 🍏 **Kotlin Multiplatform (Android & iOS)**: Shared domain models, pure Kotlin heuristic classifier, and native platform bridges (`expect`/`actual`).
- 🔐 **App Lock & Biometric Authentication**: Protect sensitive documents with Fingerprint, Face ID / Touch ID, or device PIN/pattern. 100% offline.
- 🌍 **Multi-Language On-Device AI OCR**: Offline text recognition supporting **Latin, Devanagari (Hindi/Sanskrit/Marathi/Nepali), Chinese, Japanese, and Korean** (Google ML Kit on Android, Apple Vision on iOS).
- 🔍 **Pinch-to-Zoom & Pan Page Lightbox**: Fullscreen interactive page viewer with up to 5x zoom, double-tap zoom toggle, and smooth boundary clamping.
- 📊 **Smart Auto-Categorization**: Offline heuristic classifier automatically identifies Invoices, Receipts, ID Cards, Contracts, Notes, and Books from OCR text.
- 🔖 **Tags & Custom Labels**: Organize scans with custom searchable `#tags` and filter chips.
- 📤 **Bulk Import from Gallery**: Select and import multiple photos/documents from device storage directly into the OCR pipeline.
- 📋 **Enhanced Text Tools**: One-tap "Copy All Text", native "Share Text" sheet, and selectable paragraph text.
- 📸 **Smart Document Detection & Perspective Correction**: Automatically detects document boundaries, crops, flattens, and applies enhancement filters (Google Document Scanner on Android, Apple VisionKit on iOS).
- 📄 **Searchable & UHD PDF Generation**: Generates searchable PDFs with invisible OCR overlay. Supports **Standard (150 DPI)**, **High (300 DPI)**, and **Ultra HD (600 DPI Archival)** exports.
- 📌 **Document Pinning & Favourites**: Pin important documents to keep them floating at the top of your list.
- ✏️ **Page-Level Editing**: Rotate individual pages in 90° increments and delete specific pages from multi-page scans with automated re-indexing.
- 📂 **Multi-Select & Batch Operations**: Long-press cards to enter multi-select mode. Batch delete, batch change categories, or batch pin multiple documents simultaneously.
- 📅 **Flexible Document Sorting**: Sort your collection by Date (Newest/Oldest), Title (A-Z / Z-A), or Page count.
- 🔢 **Storage Stats & Metrics**: Real-time storage footprint calculation showing disk usage per document and total app storage.
- ⚡ **Instant Full-Text Search (FTS4)**: SQLite-powered tokenized full-text search indexing across document titles, extracted OCR body text, and custom tags.
- 🌗 **App Theme & Dark Mode**: System Default, Light, and Dark mode manual override with Material 3 dynamic theming.
- 🎨 **Material 3 Adaptive & Themed Icon**: Dynamic wallpaper-tinted monochrome icons on Android 13+ and instant splash screen dismissal.
- 🔒 **Zero Cloud Uploads**: The application intentionally does not declare `android.permission.INTERNET`. All processing and storage remain strictly on your device.

---

## 🏛️ Multiplatform Architecture

```
DocScanner
├── shared/                         # Kotlin Multiplatform Shared Core (KMP)
│   ├── src/commonMain/             # Shared domain models, classifier & expect declarations
│   │   └── kotlin/com/example/docscanner/
│   │       ├── model/              # Document, Page, CategoryClassifier, SortOrder, OcrLanguage, PdfQuality
│   │       └── bridge/             # expect class: PlatformOcrEngine, PlatformBiometrics, PlatformPdfGenerator, PlatformStorage
│   ├── src/androidMain/            # Android actual implementations (ML Kit, BiometricPrompt, iText 7)
│   └── src/iosMain/                # iOS actual implementations (Apple VisionKit, Vision OCR, LocalAuth, PDFKit)
├── app/                            # Android Application module (Jetpack Compose UI)
├── iosApp/                         # iOS Application (SwiftUI host embedding Compose UI)
│   ├── iosApp/                     # iOSApp.swift, ContentView.swift, Info.plist
└── .github/workflows/build-ios.yml # Automated macOS CI workflow compiling iOS framework
```

---

## 🛠️ Tech Stack & Platform Mappings

| Feature | Android (`androidMain`) | iOS (`iosMain`) |
|---|---|---|
| **Language** | Kotlin 2.3.20 | Kotlin / Native 2.3.20 |
| **Document Scanner** | Google Play Services Document Scanner | Apple **VisionKit** (`VNDocumentCameraViewController`) |
| **Offline OCR Engine**| Google ML Kit Text Recognition v2 | Apple **Vision Framework** (`VNRecognizeTextRequest`) |
| **App Lock / Security**| AndroidX Biometric (`BiometricPrompt`) | Apple **LocalAuthentication** (Face ID / Touch ID) |
| **PDF Generation** | iText 7 (Core + Bouncy Castle) | Apple **PDFKit** (`PDFDocument` / `PDFPage`) |
| **File Sandbox** | Android Context `filesDir` | Apple Foundation `NSFileManager` Documents |
| **Image Loading** | Coil Compose | UIKit `UIImage` / Coil 3 |
| **UI Framework** | Jetpack Compose (M3) | Compose Multiplatform & SwiftUI |

---

## 📥 Download Release

Download the latest release from the [Releases](https://github.com/cyberlog69/DocScanner/releases) section:
- **`app-debug.apk`** with all offline ML models bundled.
- **`DocScannerKit.framework`** for iOS (compiled via GitHub Actions).

---

## 🔒 Privacy Guarantee

```xml
<!-- NOTE: android.permission.INTERNET is intentionally NOT included -->
<!-- This app is 100% offline by design -->
```
DocScanner does not require or request network permissions. Your personal files, identity documents, and scanned records never leave your device.

---

## 📜 License & Third-Party Notices

This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.

### Third-Party Libraries & Dual Licensing
- **iText 7/8 Core (`com.itextpdf:itext-core`)**: Distributed under the **GNU AGPLv3** open-source license and available under commercial licensing from Apryse/iText Group NV. If you build or distribute commercial derivative works without making source available under AGPLv3, an iText commercial license is required.
- **Google ML Kit**: Distributed under Google APIs Terms of Service and Apache 2.0 components.
- **Bouncy Castle Adapter**: Distributed under the Bouncy Castle License (MIT-derivative).
- **Apple Vision / PDFKit / LocalAuthentication**: Native iOS system frameworks provided under Apple Developer Agreement.




