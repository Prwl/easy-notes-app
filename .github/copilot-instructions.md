# EasyNotes — Copilot Instructions

## Build Commands

```bash
export ANDROID_HOME=~/Library/Android/sdk

./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run all unit tests
./gradlew connectedAndroidTest   # Run instrumented tests (requires device/emulator)

# Run a single unit test class
./gradlew :app:testDebugUnitTest --tests "com.easynotes.SomeTest"

# Run a single instrumented test
./gradlew :app:connectedDebugAndroidTest --tests "com.easynotes.SomeTest"
```

Java 17 is pre-configured in `gradle.properties` via `org.gradle.java.home`. Do not change the JDK path — it points to the Homebrew-installed OpenJDK 17 on this machine.

## Architecture

**MVVM + Repository** with a strict layering rule: UI → ViewModel → Repository → DAO. No screen or Composable ever touches `NoteDao`/`FolderDao` directly.

```
data/
  model/Note.kt          — Room entity + SortOrder enum (folderId nullable Long, tags CSV String)
  model/Folder.kt        — Room entity (id, name, color Int, createdAt)
  db/NoteDao.kt          — all SQL queries (Flow for lists, suspend for writes)
  db/FolderDao.kt        — folder CRUD + getNoteCountForFolder(Flow<Int>)
  db/NoteDatabase.kt     — Room v2 (Note + Folder); MIGRATION_1_2 adds folders table
  repository/            — NoteRepository, FolderRepository
di/AppModule.kt          — Hilt SingletonComponent: DB → NoteDao/FolderDao → Repos
navigation/Screen.kt     — sealed class; bottomNavRoutes set for Home/Folders/Settings
ui/
  home/HomeScreen.kt + HomeViewModel.kt
  home/ArchiveTrashScreens.kt — ArchiveViewModel + TrashViewModel co-located here
  editor/EditorScreen.kt + EditorViewModel.kt
  folders/FolderListScreen.kt + FolderViewModel.kt  — list + create/rename/delete
  folders/FolderNotesScreen.kt + FolderNotesViewModel — notes filtered by folderId
  settings/SettingsScreen.kt   — links to Archive, Trash; placeholder for Theme/Language
  theme/Color.kt, Theme.kt, Type.kt
MainActivity.kt          — hosts EasyNotesNavGraph() with outer Scaffold + NavigationBar
```

**Bottom navigation:** Outer `Scaffold` in `EasyNotesNavGraph` shows `NavigationBar` only when `currentRoute in bottomNavRoutes`. Use `launchSingleTop = true` + `popUpTo(Screen.Home.route)` when switching tabs.

**Data flow for lists:** `NoteDao` returns `Flow<List<Note>>` → `NoteRepository` passes it through unchanged → `ViewModel` uses `flatMapLatest`/`stateIn` to expose `StateFlow<UiState>` → Composable collects with `collectAsStateWithLifecycle()`.

**Data flow for writes:** Composable calls ViewModel function → `viewModelScope.launch { repository.someMethod() }` → `suspend` DAO function.

## Key Conventions

### Note identity
- `noteId = -1L` means **new, unsaved note**. The editor creates a `Note(id = 0)` for insert; Room assigns the real ID, which `EditorViewModel` stores in `currentNoteId`.
- Deletion is always soft: set `isDeleted = 1`. Permanent deletion only happens via `emptyTrash()` or an explicit "Delete permanently" action.

### Note colors
- Colors are stored as `Int` (ARGB) on the `Note` entity.
- The canonical palette lives in `ui/theme/Color.kt` as `NoteColors: List<Int>`. Phase 4 will expand this list to 200+.
- Content color (text/icon tint on a card) is computed inline: two specific dark hex values (`0xFF1A1A2E`, `0xFF16213E`) get `Color.White`; everything else gets `Color.Black`. Reproduce this exact check wherever note backgrounds are rendered.

### UiState pattern
Each screen has a `data class XxxUiState(...)` defined in the same file as its ViewModel. State is never held in the Composable itself — all state lives in the ViewModel and flows down as a single `StateFlow<UiState>`.

### Auto-save in the editor
`EditorViewModel` debounces saves with a `Job`: every content/title/color change cancels the previous job and schedules a new `saveNote()` after 2 000 ms. `onCleared()` also triggers a final save. Do not add extra save triggers without cancelling `autoSaveJob` first.

### Rich text content (Phase 2+)
Note content is stored as JSON in `Note.content` when the note has any formatting. Use `RichTextSerializer.isRichText(content)` to detect, `decode()` to read, and `encode(annotatedString, checklists)` to write. Plain text notes are stored as-is.

`RichTextState` (in `ui/editor/richtext/`) is a `@Stable` Compose class (not a ViewModel) that owns the character-level span arrays. It is `remember`-ed in `EditorScreen` and populated via `EditorViewModel.populateRichTextState()` once loading completes. The ViewModel holds only the serialized string; the live `AnnotatedString` lives in `RichTextState`.

**Method naming rule:** `RichTextState` has `var fontSize`, `var textColor`, `var textAlign` etc. as Compose `mutableStateOf` properties. The corresponding "apply to selection" methods are named `applyFontSize`, `applyTextColor`, `applyAlignment` (not `set*`) to avoid JVM setter name clashes.

**Checklist items** are stored in `RichTextState.checklistItems: List<ChecklistItem>` separately from the `AnnotatedString`. They are serialized alongside the rich text in the same JSON blob under the `"checklist"` key.

1. Add the `@Query`/`@Insert`/`@Update` method to `NoteDao`.
2. Add a wrapper in `NoteRepository` (never expose the DAO type outside `data/`).
3. Call the repository method from the ViewModel.

### Navigation
Routes are defined as `data object` members of the `Screen` sealed class. Use `Screen.Xxx.route` for `NavHost` destinations and `Screen.Xxx.createRoute(...)` for `navigate()` calls. Arguments use `NavType.LongType`; `-1L` is the convention for "no argument / create new".

### Hilt
Every ViewModel is `@HiltViewModel` with `@Inject constructor`. Every Activity/Fragment entry point is `@AndroidEntryPoint`. All singletons are provided in `di/AppModule.kt` under `SingletonComponent`.

### Icon imports
Use `Icons.AutoMirrored.Filled.*` for directional icons (Sort, ArrowBack, NoteAdd). Using `Icons.Default.*` for these will produce deprecation warnings in Kotlin 2.1.

## Project Roadmap (phases)

| Phase | Status | Focus |
|-------|--------|-------|
| 1 | ✅ Done | Foundation: Room, Home, Editor, Nav |
| 2 | ✅ Done | Rich text editor (bold/italic/underline, font size/color, alignment, checklists) |
| 3 | ✅ Done | Folders, tags, bottom nav, Settings screen |
| 4 | ✅ Done | 200+ note colors (grouped), dark/light/system theme, custom fonts, animations |
| 5 | Pending | Widget, note lock (BiometricPrompt), PDF export, image attachments, drawing canvas |
| 6 | Pending | Local backup/restore, Google Drive backup, splash screen, perf tuning |

See `EASY_NOTES_PROJECT.md` for detailed feature checklists and ready-to-use prompts for each phase.

## Important Constraints
- **No ads, no analytics, no paywalls.** All features are free by default.
- **Offline-first.** No network dependencies for core functionality.
- **Min SDK 26** (Android 8.0). Do not use APIs below API 26 without a compat check.
- Room database is version **2**. Any further schema change requires bumping the version and adding a new `Migration(N, N+1)` in `NoteDatabase.MIGRATION_*`, then registering it in `AppModule` via `addMigrations(...)`.
