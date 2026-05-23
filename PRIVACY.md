# Inkleaf — Privacy Policy

**Effective date:** 2026-01-01
**Last updated:** 2026-01-01
**Publisher:** Prawal Sharma

## Summary (Plain English)

Inkleaf is a fully offline note-taking app. **We do not collect, transmit, sell, share, or store any of your personal data.** Everything you write stays on your device. There are no ads, no analytics, no third-party SDKs, and no user accounts.

## What data the app handles

The app only reads and writes data **locally on your device**:

| Data | Where it is stored | Purpose | Ever leaves the device? |
|---|---|---|---|
| Notes you create (title, content, color, tags, folders) | App-private SQLite database (`/data/data/com.prawalsharma.inkleaf/`) | Core app functionality | **No** |
| Images you attach to notes | URIs to your device photo library; granted persistent permission so the image can be displayed later | Display attached images inside notes | **No** |
| Drawings you create | PNG files in the app's private storage | Display drawings inside notes | **No** |
| PIN you set on a locked note | Stored as a salted SHA-256 **hash** in the app's private DataStore (the PIN itself is never stored) | Unlock your locked notes | **No** |
| Biometric (fingerprint/face) | Handled entirely by Android's BiometricPrompt system API; the app never sees your biometric data | Unlock locked notes | **No** |
| Theme & font preference | App-private DataStore | UI personalization | **No** |
| Backup files you export | Saved by you to a location you choose (e.g., Downloads) | Manual backup/restore | **No (unless you upload them yourself)** |

## What data is NOT collected

- No usage analytics (no Firebase Analytics, no Google Analytics, no Mixpanel, no Amplitude, no Crashlytics, no Sentry).
- No advertising or ad-id tracking.
- No third-party SDKs of any kind.
- No telemetry, crash reports, or "improve the product" data.
- No account, no email, no phone number, no name.
- No location.
- No contacts.
- No microphone or camera unless you explicitly attach an image you took yourself.

## Network access

The app does not make any network requests. It does not connect to any server controlled by us or anyone else.

## Permissions explained

- **USE_BIOMETRIC** — to let Android's biometric prompt unlock notes you've protected with a PIN.
- **READ_EXTERNAL_STORAGE** (Android 12 and below only) — to attach images from your device.

The app does **not** request internet access, location, contacts, microphone, camera, calendar, SMS, or call logs.

## Backups (Google Drive auto-backup)

The app's manifest explicitly **excludes** your notes database, drawings, and stored PIN hashes from Android's automatic Google Drive backup. If you want a backup, use the **Export Backup** feature in Settings — that backup file is created on-device and only goes anywhere if you choose to share it.

## Children's privacy

Inkleaf does not knowingly collect any data from anyone, including children under 13. It is suitable for all ages.

## Your rights

Because we hold no personal data about you, there is nothing for us to disclose, modify, or delete on a server. You have full control: uninstalling Inkleaf erases all app data.

## Changes to this policy

If we ever change the data-handling behaviour of Inkleaf, we will update this policy and bump the effective date.

## Contact

For privacy questions, contact: `prawal.sharma.inkleaf@gmail.com` (replace with your real address before publishing).
