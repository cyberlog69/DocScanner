# 🚀 DocScanner - Offline AI Document Scanner

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.3-purple.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline-brightgreen.svg)](#privacy-guarantee)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DocScanner** is a fast, modern, privacy-focused Android application that transforms your device into an offline AI document scanning workstation. Powered by Google ML Kit and Jetpack Compose, it detects documents, corrects perspective, performs on-device OCR, and generates searchable PDFs—all locally without connecting to the cloud.

---

## ✨ Features

- 📸 **Smart Document Detection & Perspective Correction**: Automatically detects document boundaries, crops, flattens, and applies enhancement filters using Google Play Services Document Scanner API.
- 🔍 **100% On-Device AI OCR**: Bundled ML Kit Text Recognition v2 extracts text directly from images offline with high accuracy.
- 📄 **Searchable PDF Generation**: Embeds an invisible text layer over high-resolution scanned page images using iText 7.
- ⚡ **Instant Full-Text Search (FTS4)**: SQLite-powered full-text search indexing across document titles and extracted OCR body text.
- 🏷️ **Smart Categorization**: Organize documents by category (Receipts, ID Cards, Notes, Contracts, Invoices, Books, etc.) with animated category filtering.
- 🎨 **Modern Jetpack Compose & Material 3 UI**: Clean Indigo/Teal design system with smooth animations, grid/list view toggle, and dark/light dynamic theming.
- 🔒 **Zero Cloud Uploads**: The application intentionally does not declare `android.permission.INTERNET`. All processing and storage remain strictly on your device.

---

## 🏛️ Architecture

```
DocScanner
├── app/src/main/java/com/example/docscanner/
│   ├── data/
│   │   ├── db/          # Native SQLiteOpenHelper with FTS4 virtual tables & reactive Flow queries
│   │   ├── model/       # Document, Page, and DocumentCategory domain models
│   │   └── repository/  # DocumentRepository layer
│   ├── service/
│   │   ├── DocumentScannerService.kt # ML Kit Document Scanner integration
│   │   ├── FileStorageService.kt     # App sandbox storage & FileProvider URI management
│   │   ├── OcrService.kt             # Bundled ML Kit Text Recognition v2 engine
│   │   └── PdfGenerator.kt           # iText 7 searchable PDF generator
│   ├── ui/
│   │   ├── camera/      # Document scanner launch screen & OCR state handling
│   │   ├── documents/   # Document list & multi-page detail screens
│   │   ├── navigation/  # Type-safe Compose navigation routes & factories
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
| **Database** | SQLite + FTS4 Full-Text Search |
| **Image Loading** | Coil Compose |
| **Asynchrony** | Kotlin Coroutines & Flow |

---

## 📥 Download Beta Release

Download the latest APK from the [Releases](https://github.com/cyberlog69/DocScanner/releases) section:
- **`app-debug.apk`** (~75.8 MB with all offline ML models bundled).

---

## 🔒 Privacy Guarantee

```xml
<!-- NOTE: android.permission.INTERNET is intentionally NOT included -->
<!-- This app is 100% offline by design -->
```
DocScanner does not require or request network permissions. Your personal files, identity documents, and scanned records never leave your device.
