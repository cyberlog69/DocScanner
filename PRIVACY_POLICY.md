# Privacy Policy for DocScanner

**Effective Date:** September 7, 2026  
**Last Updated:** September 7, 2026  

DocScanner ("we", "our", or "the app") is a privacy-first, 100% on-device mobile document scanning application available for Android and iOS. Your privacy is our highest priority, and DocScanner is engineered from the ground up to operate completely offline without data collection, telemetry, or remote servers.

---

## 1. Zero Data Collection & Zero Tracking

- **No Personal Information Collected:** DocScanner does not collect, record, transmit, or monetize your personal information, contact information, identity, or location.
- **No Third-Party Analytics or Trackers:** DocScanner contains no third-party tracking SDKs, no advertisement frameworks, no crash loggers, and no usage analytics (e.g., no Google Analytics, no Firebase, no Facebook SDK).
- **No Cloud Uploads:** All document processing, Optical Character Recognition (OCR), image enhancements, perspective correction, and PDF creation are executed entirely on your local device. Your documents never leave your phone.

---

## 2. Device Permissions and How They Are Used

DocScanner requests only the minimum permissions necessary to deliver document scanning functionality:

| Permission | Purpose | Data Handling |
|---|---|---|
| **Camera (`android.permission.CAMERA`)** | Used in real time to capture physical documents, receipts, and ID cards. | Video feed and photographs are processed solely on-device. Images are stored only in your private local database or exported to your designated storage. |
| **Storage / Media Access (`READ_EXTERNAL_STORAGE` on API ≤ 28)** | Used strictly on older Android versions to allow importing existing photos from your gallery or saving PDFs. | On Android 10+ (API 29+), DocScanner uses Android Scoped Storage and the system Photo Picker, requiring no broad storage permissions. |
| **Biometric Hardware (`android.permission.USE_BIOMETRIC`)** | Used to authenticate you before unlocking the app or accessing the Encrypted Document Vault. | Authentication is performed entirely by Android's hardware-backed `BiometricPrompt` and Android Keystore. DocScanner never accesses or stores your biometric data (fingerprint, face recognition data, or PIN). |

---

## 3. Network Access Policy

- **Google Play Edition (`play` flavor):** Does **not** request or use the `android.permission.INTERNET` permission. The Google Play edition operates in complete network isolation and is technically incapable of sending data over the internet.
- **Standalone Sideload Edition (`github` flavor):** Requests `INTERNET` solely and exclusively to check for newer app releases on GitHub and download update APK files when initiated by the user. Document content, scan text, and media are never transmitted.

---

## 4. On-Device Encryption & Security

- Documents placed in the **Encrypted Vault** are protected using **AES-256 GCM** authenticated encryption with cryptographic keys managed by the hardware-backed **Android Keystore**.
- Unlocking the vault occurs strictly in volatile device memory and is never persisted unencrypted to permanent storage without user action.

---

## 5. Third-Party Libraries

DocScanner utilizes the following on-device open-source libraries:
- **Google ML Kit Document Scanner & Text Recognition:** Operates with pre-bundled offline models. No scanned imagery or extracted text is transmitted to Google.
- **iText 7 / Apache PDFBox:** Used for on-device PDF synthesis and text-layer overlay.

---

## 6. Children's Privacy

DocScanner does not collect data from any user, including children under the age of 13.

---

## 7. Contact Us

If you have any questions, feedback, or concerns regarding this Privacy Policy, you may contact us via our public GitHub repository:
- **Repository:** [https://github.com/cyberlog69/DocScanner](https://github.com/cyberlog69/DocScanner)
- **Issues:** [https://github.com/cyberlog69/DocScanner/issues](https://github.com/cyberlog69/DocScanner/issues)
