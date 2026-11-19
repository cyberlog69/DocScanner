# Security Policy

## Supported Versions

DocScanner is distributed as a standalone app; only the latest published release
receives security fixes.

| Version | Supported |
| ------- | --------- |
| 1.9.x   | :white_check_mark: |
| < 1.9   | :x: |

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
