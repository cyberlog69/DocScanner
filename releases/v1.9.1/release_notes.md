## DocScanner v1.9.1 — Vault & Data-Integrity Hardening

This release is about making the encrypted vault, backups, and the in-app updater
fail safely instead of silently doing the wrong thing. Every change keeps scanning,
OCR, and PDF generation fully on-device.

---

### Encrypted Vault

- **Ciphertext at rest.** Vault documents now live in their own `files/vault/`
  directory, separate from the library. They are decrypted only while the vault is
  open, and re-encrypted when you close the vault, leave the vault screen, or
  background the app.
- **Crash recovery.** If the process dies while the vault is open, the files are
  re-encrypted on the next start.
- **Transactional moves.** Moving a document into or out of the vault rolls back on
  any failure, so a half-encrypted document is never left behind.
- **Existing vault documents are migrated** to the new location the first time you
  unlock the vault.
- **Streaming crypto.** Encryption and in-place decryption stream through the
  cipher, so large PDFs no longer spike memory.

### Backups and device transfer

- Vault documents are excluded from `.zip` backups and from Android
  device-to-device transfer. Their key is bound to this device, so they could not be
  decrypted anywhere else.
- Restores report failed documents and pages instead of claiming success.
- Crafted backup archives are rejected past 20,000 entries or 2 GB uncompressed.

### Updater

- GitHub APK downloads are verified with SHA-256 before installation.
- Automatic update checks are **off by default** and can be enabled in Settings.

### OCR

- The recognition mode (Auto / Printed / Handwriting) is now a real setting and is
  applied by the scanner, the document detail screen, and the shared pipeline.
- Handwriting mode applies a shared contrast/stroke-enhancement pass instead of
  guessing from image statistics.

### Reliability

- Database migrations no longer swallow exceptions; a missing migration step fails
  loudly instead of leaving a half-migrated database.
- Debug builds use the debug key and never touch release signing material.
- Document cards were split out of the document list screen, and the unused
  view-model factory was removed.

---

### Signing key change — action required for existing sideload users

The previous release key was exposed and has been retired. Builds from v1.9.1
onward are signed with a new key, so **Android will refuse to install this update
over an older build**. Export anything you need, uninstall the old app, then install
v1.9.1. Uninstalling removes documents stored inside the app.

Store installs are unaffected by the key change.

---

### Release assets

- `DocScanner-Android-v1.9.1.apk`
- `DocScanner-iOS-v1.9.1.ipa`
- `DocScannerKit-iOS-Framework.zip`

---

### Known limitations

- iOS remains a partial-parity SwiftUI app; it does not yet use the shared Kotlin
  core for scanning, OCR, or vault.
- No instrumented/UI test suite; the vault lifecycle is covered by unit tests and
  manual device checks.
