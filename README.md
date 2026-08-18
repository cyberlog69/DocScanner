# 🚀 DocScanner - Offline AI Document Scanner

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.3-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-brightgreen.svg)](#-privacy-guarantee)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DocScanner** is a fast, modern, privacy-focused Android application that transforms your device into an offline AI document scanning workstation. Powered by Google ML Kit and Jetpack Compose, it detects documents, corrects perspective, performs on-device OCR, and generates searchable PDFs—all locally without connecting to the cloud.

---

## ✨ Features

- 📸 **Smart Document Detection & Perspective Correction**: Automatically detects document boundaries, crops, flattens, and applies enhancement filters using Google Play Services Document Scanner API.
- 🔍 **100% On-Device AI OCR**: Bundled ML Kit Text Recognition v2 extracts text directly from images offline with high accuracy.
- 📄 **Searchable & UHD PDF Generation**: Generates searchable PDFs with invisible OCR overlay. Supports **Standard (150 DPI)**, **High (300 DPI)**, and **Ultra HD (600 DPI Archival)** exports using iText 7.
- 📌 **Document Pinning & Favourites**: Pin important documents to keep them floating at the top of your list.
- ✏️ **Page-Level Editing**: Rotate individual pages in 90° increments and delete specific pages from multi-page scans with automated re-indexing.
- 📂 **Multi-Select & Batch Operations**: Long-press cards to enter multi-select mode. Batch delete, batch change categories, or batch pin multiple documents simultaneously.
- 📅 **Flexible Document Sorting**: Sort your collection by Date (Newest/Oldest), Title (A-Z / Z-A), or Page count.
- 🔢 **Storage Stats & Metrics**: Real-time storage footprint calculation showing disk usage per document and total app storage.
- ⚡ **Instant Full-Text Search (FTS4)**: SQLite-powered tokenized full-text search indexing across document titles and extracted OCR body text.
- 🏷️ **Smart Categorization**: Organize documents by category (Receipts, ID Cards, Notes, Contracts, Invoices, Books, etc.) with animated category filtering.
- 🌗 **App Theme & Dark Mode**: System Default, Light, and Dark mode manual override with Material 3 dynamic theming.
- 🎨 **Modern Jetpack Compose UI**: Clean Indigo/Teal design system with custom brand logo, smooth transitions, and responsive grid/list views.
- 🔒 **Zero Cloud Uploads**: The application intentionally does not declare `android.permission.INTERNET`. All processing and storage remain strictly on your device.

---

## 🏛️ Architecture

```
DocScanner
├── app/src/main/java/com/example/docscanner/
│   ├── data/
│   │   ├── db/          # SQLiteOpenHelper with FTS4 virtual tables, v3 migration & Flow queries
│   │   ├── model/       # Document, Page, SortOrder, and DocumentCategory domain models
│   │   ├── pref/        # ScannerPreferences (Camera & PDF Quality, ThemeMode, Auto-OCR)
│   │   └── repository/  # DocumentRepository layer
│   ├── service/
│   │   ├── DocumentScannerService.kt # ML Kit Document Scanner integration
│   │   ├── FileStorageService.kt     # App sandbox storage, rotation & FileProvider URI management
│   │   ├── OcrService.kt             # Bundled ML Kit Text Recognition v2 engine
│   │   └── PdfGenerator.kt           # iText 7 searchable PDF generator with multi-DPI profiles
│   ├── ui/
│   │   ├── camera/      # Document scanner launch screen & OCR state handling
│   │   ├── components/  # Material 3 Custom Brand Logo & common UI widgets
│   │   ├── documents/   # Document list (grid/list/batch/sort) & multi-page detail screens
│   │   ├── navigation/  # Type-safe Compose navigation routes & factories
│   │   ├── settings/    # SettingsDialog for Theme, Quality & OCR controls
│   │   └── theme/       # Material 3 Color Schemes & Typography
│   ├── DocScannerApp.kt # Application container & Service Locator
│   └── MainActivity.kt  # Edge-to-edge Compose entry point
```

---

## 🛠️ Tech Stack & Libraries

| Category | Technology |
|---|---|
| **Language** | Kotlin 2.3.20 |
| **Build Tool** | Gradle 9.1.0 & Android Gradle Plugin 9.0.1 |
| **UI Framework** | Jetpack Compose & Material Design 3 |
| **Document Scanner** | Google Play Services Document Scanner API |
| **OCR Engine** | ML Kit Text Recognition v2 (bundled on-device) |
| **PDF Generation** | iText 7 (Core + Bouncy Castle) |
| **Database** | SQLite + FTS4 Full-Text Search (Migration v3) |
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

