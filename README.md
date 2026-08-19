# 🚀 DocScanner - Offline AI Document Scanner

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.3-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-brightgreen.svg)](#-privacy-guarantee)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DocScanner** is a fast, modern, privacy-focused Android application that transforms your device into an offline AI document scanning workstation. Powered by Google ML Kit and Jetpack Compose, it detects documents, corrects perspective, performs on-device OCR across multiple languages, categorizes documents smartly, and generates searchable PDFs—all locally without connecting to the cloud.

---

## ✨ Features

- 🔐 **App Lock & Biometric Authentication**: Protect sensitive documents with Fingerprint, Face Unlock, or device PIN/pattern. 100% offline via `BiometricPrompt`.
- 🌍 **Multi-Language On-Device OCR**: Offline text recognition supporting **Latin, Devanagari (Hindi/Sanskrit/Marathi/Nepali), Chinese, Japanese, and Korean**.
- 🔍 **Pinch-to-Zoom & Pan Page Lightbox**: Fullscreen interactive page viewer with up to 5x zoom, double-tap zoom toggle, and smooth boundary clamping.
- 📊 **Smart Auto-Categorization**: Offline heuristic classifier automatically identifies Invoices, Receipts, ID Cards, Contracts, Notes, and Books from OCR text.
- 🔖 **Tags & Custom Labels**: Organize scans with custom searchable `#tags` and filter chips.
- 📤 **Bulk Import from Gallery**: Select and import multiple photos/documents from device storage directly into the OCR pipeline.
- 📋 **Enhanced Text Tools**: One-tap "Copy All Text", native "Share Text" sheet, and selectable paragraph text.
- 📸 **Smart Document Detection & Perspective Correction**: Automatically detects document boundaries, crops, flattens, and applies enhancement filters using Google Play Services Document Scanner API.
- 📄 **Searchable & UHD PDF Generation**: Generates searchable PDFs with invisible OCR overlay. Supports **Standard (150 DPI)**, **High (300 DPI)**, and **Ultra HD (600 DPI Archival)** exports using iText 7.
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

## 🏛️ Architecture

```
DocScanner
├── app/src/main/java/com/example/docscanner/
│   ├── data/
│   │   ├── db/          # SQLiteOpenHelper with FTS4 virtual tables, v4 migration & Flow queries
│   │   ├── model/       # Document, Page, CategoryClassifier, SortOrder, and DocumentCategory
│   │   ├── pref/        # ScannerPreferences (OcrLanguage, BiometricLock, Quality, Theme)
│   │   └── repository/  # DocumentRepository layer
│   ├── service/
│   │   ├── DocumentScannerService.kt # ML Kit Document Scanner integration
│   │   ├── FileStorageService.kt     # App sandbox storage, rotation & FileProvider URI management
│   │   ├── OcrService.kt             # Multi-language ML Kit Text Recognition v2 engine
│   │   └── PdfGenerator.kt           # iText 7 searchable PDF generator with multi-DPI profiles
│   ├── ui/
│   │   ├── camera/      # Document scanner launch screen & OCR state handling
│   │   ├── components/  # ZoomableImageViewer, Material 3 Brand Logo & UI widgets
│   │   ├── documents/   # Document list (grid/list/batch/sort/tags) & detail screens
│   │   ├── navigation/  # Type-safe Compose navigation routes & factories
│   │   ├── settings/    # SettingsDialog for Biometric Lock, OCR Language, Theme & Quality
│   │   └── theme/       # Material 3 Color Schemes & Typography
│   ├── DocScannerApp.kt # Application container & Service Locator
│   └── MainActivity.kt  # FragmentActivity with Biometric Lock & edge-to-edge Compose
```

---

## 🛠️ Tech Stack & Libraries

| Category | Technology |
|---|---|
| **Language** | Kotlin 2.3.20 |
| **Build Tool** | Gradle 9.1.0 & Android Gradle Plugin 9.0.1 |
| **UI Framework** | Jetpack Compose & Material Design 3 |
| **Security** | AndroidX Biometric (`BiometricPrompt`) |
| **Document Scanner** | Google Play Services Document Scanner API |
| **OCR Engine** | ML Kit Text Recognition v2 (Latin, Devanagari, Chinese, Japanese, Korean) |
| **PDF Generation** | iText 7 (Core + Bouncy Castle) |
| **Database** | SQLite + FTS4 Full-Text Search (Migration v4) |
| **Image Loading** | Coil Compose |
| **Asynchrony** | Kotlin Coroutines & Flow |

---

## 📥 Download Release

Download the latest APK from the [Releases](https://github.com/cyberlog69/DocScanner/releases) section:
- **`app-debug.apk`** with all offline ML models bundled.

---

## 🔒 Privacy Guarantee

```xml
<!-- NOTE: android.permission.INTERNET is intentionally NOT included -->
<!-- This app is 100% offline by design -->
```
DocScanner does not require or request network permissions. Your personal files, identity documents, and scanned records never leave your device.


