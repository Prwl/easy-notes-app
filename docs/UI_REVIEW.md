# Inkleaf — UI/UX Polish Review

Captured from emulator (Pixel-class, 1080×2400, density 420) running the rebranded release-channel build. Reference shots live in `docs/screenshots/`.

The app is functional, fast and offline-first. The list below is what separates "shipped" from "premium / Play-Store-feature-worthy."

## Severity legend
- 🔴 **Blocker for a polished launch** — fix before publishing.
- 🟡 **High-leverage polish** — fix in v1.0 if time allows; otherwise v1.1.
- 🟢 **Nice-to-have** — schedule for a later release.

---

## 1. 🔴 Dual-color top zone (status bar vs. app bar)
The system status bar shows the device wallpaper / dark grey while the app bar is bright orange immediately below. It looks like two unrelated regions stacked together.

**Fix:** Enable edge-to-edge and tint the status bar to match (or be transparent over) the top app bar.
- In `MainActivity`: `WindowCompat.setDecorFitsSystemWindows(window, false)` is already there.
- Set `window.statusBarColor = Color.Transparent.toArgb()` and let the top bar draw under the status bar via `Modifier.statusBarsPadding()` inside the TopAppBar's container.
- Toggle status-bar icon contrast based on theme: `WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !isDarkTheme`.

## 2. 🟡 Heavy saturated-orange app bar feels 2018-era
Material 3 best practice is a **surface-tinted** elevated app bar that subtly picks up the seed color, with the primary color reserved for the FAB and key actions.

**Fix:**
- Change `TopAppBar` to use `TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)`.
- Keep the orange as `primary` for FAB only.
- Add a 1-dp scrolled elevation tint via `TopAppBarDefaults.enterAlwaysScrollBehavior()` so the bar gains a subtle tonal lift when content scrolls underneath.

## 3. 🟡 No Material You (dynamic color) on Android 12+
A modern Android user expects the app to adopt their wallpaper palette.

**Fix:** In `Theme.kt`, branch:
```kotlin
val colors = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
        else dynamicLightColorScheme(LocalContext.current)
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```
Expose a setting "Use system colors (Android 12+)" in Settings → Appearance.

## 4. 🔴 Generic empty-states
Both Notes and Folders show a centered grey Material icon (note / folder) with a single line of text. Premium apps use a custom illustration + a primary CTA.

**Fix:**
- Replace icon with a custom vector illustration (a leaf doodling on paper for Notes; a stack of leaves for Folders — ties into "Inkleaf" branding).
- Add a primary CTA button right under the message: **"Create your first note"** / **"Create your first folder"**. Tapping triggers the same flow as the FAB.
- Add a one-line subtitle describing the value: *"Capture thoughts, lists and sketches — all stored on your device."*

## 5. 🟡 Editor formatting toolbar is cramped
Eight tightly-packed B/I/U/S/16/A/highlight/align icons compete with the keyboard when text is being typed.

**Fix:**
- Convert to a horizontally scrollable row with `Modifier.horizontalScroll(rememberScrollState())` so icons can be 44 dp (Material a11y minimum touch target) with proper spacing.
- Pin the row to `imePadding()` so it floats above the keyboard reliably.
- Group: **text style** (B/I/U/S) → **size** (16) → **color** (A) → **highlight** → **alignment**. Use thin vertical dividers between groups.

## 6. 🟡 "Add tag" + "Attach image" look like links, not actions
Both appear as plain orange text. New users miss them.

**Fix:** Convert to `AssistChip` / `FilterChip` with leading icons:
- `[+] Add tag` — opens chip-input dialog. Existing tags render as chips inline.
- `[🖼️] Attach image` — opens the picker. Once images are attached, show them as a row of thumbnails with × to remove.

## 7. 🟢 About / Settings is sparse
Currently shows just "Version 1.0.0".

**Fix:** Expand About section with:
- App version + build number (e.g., `1.0.0 (1)`)
- "Rate Inkleaf" → opens Play Store listing
- "Privacy Policy" → opens hosted PRIVACY.md URL in browser
- "Open-source licenses" → use `play-services-oss-licenses` or a simple Compose-rendered list
- "Send feedback" → mailto link with version pre-filled in the subject
- Small Inkleaf logo + tagline at the bottom

## 8. 🟢 Splash screen not visible in normal navigation
Cold-start only. Verify the Android-12 SplashScreen API icon mask renders well (centered, no clipping at the 108×108 dp adaptive icon safe zone).

**Fix:** Open `res/values/themes.xml` `Theme.Inkleaf.Starting` and verify:
- `windowSplashScreenAnimatedIcon` uses a vector with a 66 dp visible safe zone.
- `windowSplashScreenBackground` matches the launch surface color (white for light, near-black for dark).
- Test cold start on both light and dark system themes.

## 9. 🟡 Folders top bar has no actions (inconsistent with Notes)
Notes screen has a search icon; Folders screen has none.

**Fix:** Add a search field to Folders too (filters by folder name) OR add a sort menu (alphabetical / recently used / most notes). Consistency between sibling tabs matters.

## 10. 🟡 FAB is a generic + icon
Premium note apps make the FAB feel like the brand. Currently it's a plain Material + icon.

**Fix options:**
- **A (subtle):** Use `ExtendedFloatingActionButton` with "New note" / "New folder" labels that collapse to icon-only on scroll.
- **B (delightful):** When tapped, expand into a small action menu (Note / Checklist / Drawing) using `AnimatedVisibility` — this matches how Google Keep and Notion behave.

Option B is recommended because it removes a tap (currently users must create a note, then convert to checklist / drawing).

## 11. 🟢 No haptics on key actions
Adds tactile premium feel.

**Fix:** Add `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` on:
- Long-press to multi-select notes.
- Save button in editor.
- PIN entry success / failure.

## 12. 🟢 No long-press multi-select on notes list
Current flow appears to be one-by-one delete. Standard for premium note apps: long-press enters selection mode → action bar at top with delete / move-to-folder / lock all.

**Fix:** Add selection mode state to `HomeViewModel`. Top bar swaps to contextual mode when `selectedIds.isNotEmpty()`.

## 13. 🟢 Editor lacks an undo/redo toolbar
Drawing has undo/redo but the text editor doesn't.

**Fix:** Add undo/redo icons to the right side of the formatting toolbar, backed by a simple text-state stack (cap at 50 like the drawing redo stack).

## 14. 🔴 (Verify) Adaptive icon mask
The app icon must render correctly when the launcher applies square / circle / squircle / rounded-square masks. Some launchers will show a clipped logo if the foreground vector extends past the 66 dp safe zone.

**Fix:** Open `res/mipmap-anydpi-v26/ic_launcher.xml`, confirm:
- Background and foreground are both 108×108 dp.
- Foreground content sits inside the central 66 dp.
- Test by long-pressing the icon on the home screen — different launchers preview different mask shapes.

## 15. 🟢 No dark-theme parity check captured yet
All four screenshots are light theme. Capture matching dark-theme shots to verify:
- Orange app bar doesn't blow out brightness.
- Empty-state icon has enough contrast.
- Editor surface is `surfaceContainerHigh`, not pitch black.

---

## Priorities for Play-Store-ready v1.0

Order of attack:
1. (1) Edge-to-edge status bar
2. (4) Empty-state illustrations + CTA buttons
3. (14) Verify adaptive icon at all mask shapes
4. (6) Tag + image-attachment as chips
5. (2) Surface-tinted app bar + (3) Material You toggle
6. (5) Floating IME-aware formatting toolbar
7. (10) Expandable FAB with note/checklist/drawing
8. (9) Folders search/sort parity
9. (12) Multi-select on notes list
10. (7) Expanded About section

Everything else can ship as v1.1.

---

## Quick-win patches (≤30 min each)
- (1), (2), (7), (9), (11), (13) are all small Compose changes.
- (3), (4), (6), (10), (12) are medium (1-2 hours each).
- (5), (14) require asset / theme work.
