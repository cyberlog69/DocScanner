# 🗺️ DocScanner — Future Roadmap

> **Released**: `v1.9.0` (versionCode 11) · **In development on `main`**: `v1.9.1` (handwriting OCR modes)
> **Platforms**: Android (primary), iOS (partial parity) · Kotlin Multiplatform 2.3.20
> **Last Updated**: November 2026

---

## 🧭 Strategic Vision

DocScanner's north star is to be the **world's most capable 100% offline AI document scanner** — zero cloud dependency, zero data leakage, maximum intelligence. Every roadmap item is evaluated against three pillars:

| Pillar | Goal |
|---|---|
| 🔒 **Privacy-First** | All AI/ML remains on-device. No cloud round-trips. |
| 📱 **Multiplatform Excellence** | Android & iOS parity via KMP shared core |
| 🤖 **Offline AI Intelligence** | Richer, smarter, faster on-device processing |

> **Current reality check (Nov 2026):** the shared module holds models, parsers and
> platform bridges, but the Android app still owns most business logic (PDF, vault,
> backup, signatures). The iOS app is a SwiftUI reimplementation covering scanning,
> OCR and PDF export only. Closing that gap is tracked under
> "iOS Parity" below and is a prerequisite for any claim of full parity.

---

## ✅ v1.9.0 — "Smart Flows" _(Released · Q4 2026)_

### 🎯 Focus: Workflow Automation & UX Polish

#### 📋 Document Workflows
- **✅ 🔁 Multi-Page Reorder**: Accessible page reordering dialog with sequence preview, re-indexing, thumbnail synchronization, and automatic PDF regeneration
- **✅ ✂️ Interactive Page Re-crop**: 4-point draggable corner handles, perspective warp (`Matrix.setPolyToPoly`), OCR text re-extraction, and PDF regeneration
- **✅ 🪄 Smart Merge Suggestions**: Temporal session clustering (<25 mins) and category/title heuristics with 1-tap merge suggestion banner and dismissal
- **✅ 📐 Auto-Rotate Correction**: Baseline OCR orientation detection (clustering into 0°, 90°, 180°, 270° quadrants) with single-page and batch auto-rotate
- **✅ ⌨️ Hardware Keyboard Navigation**: Full keyboard navigation shortcuts for tablets and desktops (Ctrl+N, Ctrl+F, Ctrl+R, Ctrl+P, Ctrl+S, Escape, arrow keys)
- **✅ 🧹 Modular Database Migration**: Modular `DatabaseMigration` system for incremental SQLite schema changes (v1 through v5+ established)
- **✅ 🧪 Expanded Unit Test Suite**: Comprehensive tests for quadrilateral geometry, auto-rotate angle voting, and smart merge heuristics in `shared` commonTest

---

## 🚧 v1.9.1 — "Handwriting OCR" _(In development on `main`)_

- **✍️ Recognition Modes**: `OcrRecognitionMode` (Auto / Printed / Handwriting) selectable in Settings and applied to scan, re-OCR, and batch flows
- **🖼️ Contrast Pre-processing**: Shared stroke-enhancement pass for faint handwriting, applied only in Handwriting mode
- **🔐 Vault Lifecycle**: Vault files live under `files/vault/`, outside the library and excluded from backup/device transfer; they are decrypted only while the vault is open and re-encrypted when it closes or the app is backgrounded
- **🧹 Shared OCR Pipeline**: Single preprocessing implementation in `shared`, consumed by both the Android app and the KMP bridge

> Note: ML Kit and Apple Vision do not expose a handwriting/print classifier. The app
> therefore applies a handwriting *optimisation profile* rather than claiming
> per-line handwriting detection.

---

## 🌟 v2.0.0 — "On-Device Gemini Nano" _(Mid-term · Q1 2027)_

### 🎯 Focus: Next-Gen On-Device AI via Android AICore / Gemini Nano

> All AI features use **Android AICore / Gemini Nano** (API 35+) and **Apple Intelligence** on iOS 18+. Zero data leaves the device.

#### 🤖 AI Intelligence Upgrades
- **💬 Document Q&A Chat**: Ask questions about any scanned document in natural language. Powered by Gemini Nano's `AiCoreSession` with OCR text as grounding context
- **✍️ Handwriting-Aware Field Segmentation**: Heuristic/AI segmentation classifying extracted text blocks into printed vs. handwritten fields (`isHandwritten: Boolean`) to automatically isolate handwritten entries and signatures
- **🔍 AI Proofreading & Correction**: Detect and highlight OCR transcription errors using language-model confidence scores
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
- **🔐 Zero-Knowledge E2E Encrypted Sync**: AES-256 GCM encrypt all document records + page bitmaps client-side before upload, using a passphrase-derived key (Android Keystore vault keys are device-bound and cannot be exported)
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
- **🖊️ Stylus / Apple Pencil Annotation & Digital Ink**: Draw ink annotations directly onto scanned page images; save as an annotation layer. Offline handwriting-to-text recognition via ML Kit Digital Ink Recognition (Android) & PencilKit (iOS)

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
| 🏗️ **Architecture** | Remove the remaining duplicate DI container (done), keep one Koin graph; consider Room KMP 2.7+ or SQLDelight in `shared/commonMain` for true multiplatform persistence |
| 🧪 **Testing** | Run the unit-test suite in CI on every push (done); add repository, backup/restore and vault tests; target 80% coverage; screenshot tests via Paparazzi (Android) and SnapshotTesting (iOS) |
| ⚡ **Performance** | Profile & optimize `PdfGenerator` for 600 DPI archival exports on mid-range devices; split the 2,000-line document detail screen into per-tool composables |
| 📦 **Distribution** | Google Play Store submission (play flavor already configured); F-Droid repository listing; sign release builds from CI secrets instead of a checked-in keystore |
| 🔐 **Security Audit** | Third-party pen-test of `VaultEncryptionService` AES-256 GCM implementation and keystore binding |
| 📜 **License Compliance** | ✅ Migrated from iText AGPLv3 to Apache PDFBox (`pdfbox-android`) under Apache 2.0 |
| 🍎 **iOS Parity** | Import the KMP framework from the Swift app and delete duplicated Swift logic; complete bridges for `BackupRestoreService`, `SignatureService`, vault and folders |

---

## 📊 Version Summary Table

| Version | Theme | Target | Key Deliverable |
|---|---|---|---|
| **v1.9.0** | Smart Flows | Q4 2026 | Drag-drop reorder, re-crop, a11y |
| **v1.9.1** | Handwriting OCR | Q4 2026 | Recognition modes, shared preprocessing |
| **v2.0.0** | Gemini Nano & Handwriting AI | Q1 2027 | On-device Q&A, field segmentation, AI summaries |
| **v2.1.0** | Cloud Sync Vault | Q2 2027 | E2E encrypted opt-in cloud backup |
| **v2.2.0** | Desktop & Tablet | Q3 2027 | Compose Multiplatform desktop, two-pane tablet, digital ink handwriting |
| **v2.3.0** | Integrations | Q4 2027 | Files App, Tasker, Wear OS, Share Extensions |
| **v3.0.0** | Intelligence Platform | 2028 | Semantic search, knowledge graph, financial analytics |
