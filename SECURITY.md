# Security Policy

## Supported Versions

DocScanner is distributed as a standalone app; only the latest published release
receives security fixes.

| Version | Supported |
| ------- | --------- |
| 1.9.x   | :white_check_mark: |
| < 1.9   | :x: |

## Release signing & key rotation

Releases up to `v1.9.0` were signed with a keystore that was committed to this
repository, so that key must be treated as compromised:

- Retired signer SHA-256: `7673261baa111b5e75784c215c6f9a214c481e5ceff404d6cde5df14ffda797e`

From the next release onward, APKs are signed with a new key and carry an APK
Signature Scheme **v3 rotation lineage**:

- The retired key signs the v1/v2 blocks, which is what Android 7.0-8.1
  (API 24-27) verify.
- The new key signs the v3 block with a lineage proving the retired key
  authorised it, which is what Android 9+ (API 28+) verifies.

Installs therefore update in place instead of requiring an uninstall.

**Residual risk**: anyone can sign an APK with the retired key, and API 24-27
devices accept it as an update. Devices that install a lineage-bearing release
are protected because their trusted signer becomes the new key. Raising
`minSdk` to 28 would drop the retired key entirely.

Verify a downloaded APK:

```bash
apksigner verify --print-certs DocScanner-Android-v1.9.1.apk
apksigner lineage --in DocScanner-Android-v1.9.1.apk --print-certs
```

## Reporting a Vulnerability

Please **do not open a public issue** for security problems.

Use GitHub's private vulnerability reporting:
[Report a vulnerability](https://github.com/cyberlog69/DocScanner/security/advisories/new)

Include:

- Affected version (visible in Settings → About)
- Android version and device model
- Steps to reproduce, and any proof-of-concept files
- Whether the issue affects the encrypted vault, backup/restore, or the
  update mechanism

You can expect an acknowledgement within 7 days and a status update within
30 days. If the report is accepted, a fix is shipped in the next release and
you are credited in the release notes unless you prefer otherwise.

## Scope

In scope: on-device data handling, the AES-256 GCM vault implementation,
backup/restore archive handling, the in-app updater, and exported PDFs.

Out of scope: issues that require a rooted device or physical access to an
unlocked device, and third-party services (GitHub Releases) outside this repo.
