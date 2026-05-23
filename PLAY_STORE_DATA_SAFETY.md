# Play Console — Data Safety Form Answers

When you fill out the **Data safety** section in Google Play Console for Inkleaf, use these answers. All justifications match what the app actually does.

## Section 1 — Data collection and security

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | **Not applicable** (no data is transmitted) |
| Do you provide a way for users to request that their data be deleted? | **Yes** — uninstalling the app removes all data. There is no server-side data. |

## Section 2 — Data types

For every category below, select **"No, my app does not collect or share this type of user data"**:

- Personal info (name, email, address, user IDs, etc.)
- Financial info
- Health and fitness
- Messages
- Photos and videos *(see note below)*
- Audio files
- Files and docs *(see note below)*
- Calendar
- Contacts
- App activity
- Web browsing
- App info and performance (no crash logs, no diagnostics)
- Device or other IDs

**Note on photos and "files and docs":** The app lets users attach images or import a backup file via Android's system file/photo picker. The app **stores** the user's selection in its own private storage and does not share it with anyone. Google Play's definition of "collection" requires the data to be transmitted off the device; since this app never transmits data, the correct answer is still "does not collect."

## Section 3 — Permissions justification

| Permission | Why the app requests it |
|---|---|
| `USE_BIOMETRIC` | Allow the user to unlock locked notes with their device biometric. No biometric data is ever read or stored by the app — Android's BiometricPrompt API handles the auth and returns only a success/fail signal. |
| `READ_EXTERNAL_STORAGE` (maxSdkVersion=32) | Required only on Android 12 and below so the user can attach photos. On Android 13+, the system photo picker is used and no permission is needed. |

## Section 4 — Ads

- **Does your app contain ads?** No.
- **Ad networks used?** None.

## Section 5 — Families policy (if you target the Designed for Families program)

The app is offline, no ads, no analytics, no data collection — it satisfies all Families policy requirements out of the box. If you want to target families, declare the target age range under **App content → Target audience and content**.

## Section 6 — Content rating

Suggested ratings:
- **IARC questionnaire**: answer "No" to every question about violence, sexual content, drug references, gambling, social interaction, location sharing, etc.
- **Expected outcome:** Everyone (3+) on all rating boards.

## Section 7 — Government apps / News apps / COVID apps

None apply.

---

## Submission checklist

Before clicking **Submit for review** in Play Console:

- [ ] App name: **Inkleaf**
- [ ] Short description (80 chars max) written
- [ ] Full description (4000 chars max) written and does NOT reference "Easy Notes — Notepad, Memo" or any other published competitor
- [ ] Privacy policy URL set (host `PRIVACY.md` rendered as HTML at a public URL — e.g., a GitHub Pages site)
- [ ] Data safety section filled in per this document
- [ ] Content rating questionnaire completed
- [ ] App category set: **Productivity**
- [ ] Contact email set to a real, monitored address
- [ ] Screenshots uploaded (at least 2 phone screenshots, ideally 4–8)
- [ ] Hi-res icon (512×512) uploaded
- [ ] Feature graphic (1024×500) uploaded
- [ ] Signed AAB uploaded to **Internal testing** track first; only promote to Production after testing on a real device
