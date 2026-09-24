# 🗺️ DocScanner — Future Roadmap

> **Current Version**: `v1.8.1` (versionCode 9) · Android + iOS · Kotlin Multiplatform 2.3.20
> **Last Updated**: September 2026

---

## 🧭 Strategic Vision

DocScanner's north star is to be the **world's most capable 100% offline AI document scanner** — zero cloud dependency, zero data leakage, maximum intelligence. Every roadmap item is evaluated against three pillars:

| Pillar | Goal |
|---|---|
| 🔒 **Privacy-First** | All AI/ML remains on-device. No cloud round-trips. |
| 📱 **Multiplatform Excellence** | Android & iOS parity via KMP shared core |
| 🤖 **Offline AI Intelligence** | Richer, smarter, faster on-device processing |

---

## 🚀 v1.9.0 — "Smart Flows" _(Near-term · Q4 2026)_

### 🎯 Focus: Workflow Automation & UX Polish

#### 📋 Document Workflows
- **🔁 Multi-Page Reorder Drag & Drop**: Drag-and-drop page reordering within a document (replaces current fixed page index order)
- **✂️ Page Cropper / Re-crop**: Re-crop any saved page without re-scanning using an interactive quadrilateral corner editor
- **🪄 Smart Merge Suggestions**: Detect similar documents scanned in the same session and suggest merging them
- **📐 Auto-Rotate Correction**: Detect and auto-correct rotated pages based on OCR baseline text direction

#### ⌨️ Keyboard & Accessibility
- **⌨️ Hardware Keyboard Shortcuts**: Full keyboard navigation for tablets (Android `KeyEvent` mappings, iOS `UIKeyCommand`)
- **♿ TalkBack / VoiceOver Full Support**: A11y semantic annotations on all Compose / SwiftUI views
- **🔠 Dynamic Font Size Support**: Honor system text size scaling across all UI surfaces

#### 🧪 Quality
- **📊 Instrumented UI Test Suite**: Expand existing `commonTest` to include Compose `UiAutomator` + XCTest flows
- **🧹 Modular Database Migration**: Modular `DatabaseMigration` system for incremental SQLite schema changes (v1 through v5+ established)

---

## 🌟 v2.0.0 — "On-Device Gemini Nano" _(Mid-term · Q1 2027)_

### 🎯 Focus: Next-Gen On-Device AI via Android AICore / Gemini Nano

> All AI features use **Android AICore / Gemini Nano** (API 35+) and **Apple Intelligence** on iOS 18+. Zero data leaves the device.

#### 🤖 AI Intelligence Upgrades
- **💬 Document Q&A Chat**: Ask questions about any scanned document in natural language. Powered by Gemini Nano's `AiCoreSession` with OCR text as grounding context
- **✍️ AI Proofreading & Correction**: Detect and highlight OCR transcription errors using language-model confidence scores
- **🗃️ Generative Smart Summaries**: Replace current extractive summarizer with Gemini Nano generative summaries — multi-language aware
- **🏷️ AI Auto-Tagging**: Auto-suggest tags based on document content (e.g., `#tax-2025`, `#medical`, `#contract`)
- **🌐 AI Translation Layer**: Offline ML-powered translation of OCR text into a user-selected target language (on-device NLLB model)

#### 📐 Platform Architecture
- **🔌 Android ML Model Serving**: Integrate `com.google.android.gms:play-services-mlkit-smart-reply` and `AICore` dependency injection via existing Koin `SharedModule`
- **🍎 iOS Apple Intelligence Bridge**: Add `iosMain` `actual` implementation for `PlatformAiEngine` using Core ML / on-device `MLModel`
- **🧩 New KMP `expect`/`actual` bridges**: `PlatformAiEngine`, `PlatformTranslator`

---

## 📦 v2.1.0 — "Cloud Sync Vault" _(Mid-term · Q2 2027)_

### 🎯 Focus: Opt-In Encrypted Cloud Sync (Privacy-Preserving)

> Cloud sync is **strictly opt-in** and uses client-side E2E encryption before any byte leaves the device. The server stores only opaque ciphertext — zero knowledge.

#### ☁️ Encrypted Cloud Backup
- **🔐 Zero-Knowledge E2E Encrypted Sync**: AES-256 GCM encrypt all document records + page bitmaps client-side (reuse `VaultEncryptionService` keys) before upload
- **📡 Sync Provider Adapters**: Modular `CloudSyncAdapter` interface with first-party Google Drive and iCloud implementations; third-party Nextcloud support
- **🔄 Conflict-Free Replicated Sync**: CRDT-based merge strategy for documents edited across devices
- **📱 Multi-Device Library**: Access and open documents on any signed-in device without re-scanning

#### 🧱 Infrastructure
- **🆔 Multi-Device Document Identity**: Leverage established UUID primary keys across devices (UUID in Document model)
- **🔑 Key Derivation Protocol**: Argon2id key derivation from user passphrase → cloud encryption key (never stored on server)

---

## 🖥️ v2.2.0 — "Desktop & Tablet" _(Mid-term · Q3 2027)_

### 🎯 Focus: Compose Multiplatform Desktop + Tablet-Optimized Layouts

#### 🖥️ Desktop App (Compose Multiplatform)
- **🖥️ macOS & Windows Desktop App**: Extend KMP `shared` module to `jvmMain` target. Compose Multiplatform desktop window with drag-and-drop import
- **📁 Bulk File Import**: Drag PDF/image folders onto desktop app → batch OCR pipeline
- **🖨️ Direct Print Dialog**: Native `java.awt.print` integration for macOS/Windows printing

#### 📱 Tablet / Large Screen
- **🗂️ Two-Pane Adaptive Layout**: List-Detail split-screen on Android tablets (`WindowSizeClass` adaptive) and iPad (UISplitViewController)
- **🖊️ Stylus / Apple Pencil Annotation**: Draw ink annotations directly onto scanned page images; save as an annotation layer

---

## 🔌 v2.3.0 — "Integrations & Extensions" _(Long-term · Q4 2027)_

### 🎯 Focus: Third-Party App Ecosystem

#### 🔗 App Integrations
- **📧 Email Export**: Direct email composition with PDF attachment via Android `Intent.ACTION_SEND` + iOS `MFMailComposeViewController`
- **🗂️ Files App / SAF Integration**: Full Android Storage Access Framework (SAF) + iOS Files App provider — appear as a file provider in system pickers
- **🔗 Share Sheet Extension (iOS)**: iOS Share Extension to scan incoming image attachments directly into DocScanner
- **🤝 Android Quick Settings Tile**: One-tap scan shortcut from notification shade
- **⌚ Wear OS / watchOS Companion**: View recently scanned document thumbnails + copy OCR text from wrist

#### 🔧 Developer / Power User
- **📡 REST API Mode** _(optional, LAN-only)_: Expose a local HTTP server for programmatic document ingestion from PC automation scripts (`curl` / Python)
- **🧩 Tasker / Shortcuts Integration**: Android Tasker plug-in + iOS Shortcuts `AppIntent` actions for scan, export, and search

---

## 🧰 v3.0.0 — "Document Intelligence Platform" _(Long-term · 2028)_

### 🎯 Focus: Transform DocScanner into a full offline document intelligence engine

#### 🧠 Advanced AI
- **🔍 Semantic Search**: Vector embedding of OCR text (on-device MiniLM model) → cosine similarity search across entire library
- **🗺️ Document Graph / Knowledge Map**: Automatically link related documents by shared entities (names, amounts, dates) into a visual knowledge graph
- **📊 Financial Dashboard**: Aggregate receipt/invoice OCR data → monthly spend charts, tax summaries, and trend analytics (100% offline)
- **🤖 Agentic Document Workflows**: Chain OCR → extract → classify → route → archive pipelines triggered by document content rules

#### 🌍 Globalization
- **🌐 RTL Language Support**: Full Arabic / Hebrew RTL UI layout mirroring
- **🔤 Extended OCR Languages**: Add Tamil, Telugu, Arabic, Thai language packs via ML Kit custom models
- **🇮🇳 Aadhaar / PAN Card Parser**: India-specific ID card heuristic parser (name, DOB, UID masking)

---

## 🛠️ Ongoing / Cross-Cutting Concerns

| Area | Work Item |
|---|---|
| 🏗️ **Architecture** | Migrate SQLite database repository to Room KMP 2.7+ or SQLDelight in `shared/commonMain` for true multiplatform parity |
| 🧪 **Testing** | Achieve 80%+ unit test coverage; add screenshot tests via Paparazzi (Android) and SnapshotTesting (iOS) |
| ⚡ **Performance** | Profile & optimize `PdfGenerator` for 600 DPI archival exports on mid-range devices |
| 📦 **Distribution** | Google Play Store submission (play flavor already configured); F-Droid repository listing |
| 🔐 **Security Audit** | Third-party pen-test of `VaultEncryptionService` AES-256 GCM implementation and keystore binding |
| 📜 **License Compliance** | Migrate from iText AGPLv3 to Apache PDFBox (`pdfbox-android`) under Apache 2.0 |
| 🍎 **iOS Parity** | Shared parsers (`ReceiptParser`, `TableExtractor`, `BusinessCardParser`, `DocumentSummarizer`) promoted to `shared/commonMain`; complete bridges for `BackupRestoreService` and `SignatureService` |

---

## 📊 Version Summary Table

| Version | Theme | Target | Key Deliverable |
|---|---|---|---|
| **v1.9.0** | Smart Flows | Q4 2026 | Drag-drop reorder, re-crop, a11y |
| **v2.0.0** | Gemini Nano AI | Q1 2027 | On-device Q&A, auto-tagging, AI summaries |
| **v2.1.0** | Cloud Sync Vault | Q2 2027 | E2E encrypted opt-in cloud backup |
| **v2.2.0** | Desktop & Tablet | Q3 2027 | Compose Multiplatform desktop, two-pane tablet |
| **v2.3.0** | Integrations | Q4 2027 | Files App, Tasker, Wear OS, Share Extensions |
| **v3.0.0** | Intelligence Platform | 2028 | Semantic search, knowledge graph, financial analytics |
