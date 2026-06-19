package com.prawalsharma.inkleaf.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prawalsharma.inkleaf.data.model.Note
import com.prawalsharma.inkleaf.data.repository.NoteRepository
import com.prawalsharma.inkleaf.ui.editor.richtext.RichTextState
import com.prawalsharma.inkleaf.ui.theme.NoteColors
import com.prawalsharma.inkleaf.utils.RichTextSerializer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val noteId: Long = -1L,
    val title: String = "",
    val color: Int = NoteColors.first(),
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isHidden: Boolean = false,
    val isLocked: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true,
    val folderId: Long? = null,
    val tags: List<String> = emptyList(),
    val imageUris: List<String> = emptyList(),
    val drawingPath: String = "",
    /** True when note is locked and user has not yet authenticated this session. */
    val requiresUnlock: Boolean = false,
    /** True iff a PIN is registered for this note (used by lock UI). */
    val hasPinSet: Boolean = false,
    /** Original createdAt — preserved across updates so we never overwrite it with 0. */
    val createdAt: Long = System.currentTimeMillis(),
    // Raw serialized content (rich text JSON or plain text) – used for initial load only
    val rawContent: String = ""
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val folderRepository: com.prawalsharma.inkleaf.data.repository.FolderRepository,
    private val userPreferences: com.prawalsharma.inkleaf.data.prefs.UserPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** Exposed to EditorScreen for the folder picker dialog. */
    val foldersFlow = folderRepository.getAllFolders()

    private var autoSaveJob: Job? = null
    private var currentNoteId: Long = -1L

    // Hold a reference set by EditorScreen so auto-save can pull live content
    private var richTextStateRef: RichTextState? = null
    private var currentTitle: String = ""

    init {
        val noteId = savedStateHandle.get<Long>("noteId") ?: -1L
        currentNoteId = noteId
        if (noteId != -1L) loadNote(noteId)
        else _uiState.update { it.copy(isLoading = false) }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            if (note != null) {
                currentTitle = note.title
                val hasPin = if (note.isLocked) userPreferences.hasPin(id) else false
                _uiState.update {
                    it.copy(
                        noteId = note.id,
                        title = note.title,
                        rawContent = note.content,
                        color = note.color,
                        isPinned = note.isPinned,
                        isArchived = note.isArchived,
                        isHidden = note.isHidden,
                        isLocked = note.isLocked,
                        folderId = note.folderId,
                        tags = if (note.tags.isBlank()) emptyList() else note.tags.split(",").map { t -> t.trim() }.filter { t -> t.isNotBlank() },
                        imageUris = if (note.imageUris.isBlank()) emptyList() else note.imageUris.split(",").filter { u -> u.isNotBlank() },
                        drawingPath = note.drawingPath,
                        requiresUnlock = note.isLocked,
                        hasPinSet = hasPin,
                        createdAt = note.createdAt,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onUnlocked() = _uiState.update { it.copy(requiresUnlock = false) }

    fun onPinSet(noteId: Long, pin: String) = viewModelScope.launch {
        userPreferences.setPin(noteId, pin)
        _uiState.update { it.copy(hasPinSet = true) }
    }

    /** Suspend verifier exposed to the lock screen; returns true if PIN matches. */
    suspend fun verifyPin(candidate: String): Boolean {
        val id = _uiState.value.noteId
        return if (id == -1L) false else userPreferences.verifyPin(id, candidate)
    }

    /** Called by EditorScreen once loading is done to populate the Compose-side RichTextState. */
    fun populateRichTextState(richTextState: RichTextState) {
        richTextStateRef = richTextState
        val content = _uiState.value.rawContent
        if (content.isBlank()) {
            richTextState.loadPlainText("")
        } else if (RichTextSerializer.isRichText(content)) {
            val decoded = RichTextSerializer.decode(content)
            richTextState.loadContent(decoded.annotatedString, decoded.checklistItems)
        } else {
            richTextState.loadPlainText(content)
        }
    }

    fun onTitleChange(title: String) {
        currentTitle = title
        _uiState.update { it.copy(title = title, isSaved = false) }
        scheduleAutoSave()
    }

    fun onColorChange(color: Int) {
        _uiState.update { it.copy(color = color) }
        scheduleAutoSave()
    }

    fun onFolderChange(folderId: Long?) {
        _uiState.update { it.copy(folderId = folderId) }
        scheduleAutoSave()
    }

    fun onTagAdd(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isBlank() || _uiState.value.tags.contains(trimmed)) return
        _uiState.update { it.copy(tags = it.tags + trimmed) }
        scheduleAutoSave()
    }

    fun onTagRemove(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
        scheduleAutoSave()
    }

    fun onImageUriAdded(uri: String) {
        _uiState.update { it.copy(imageUris = it.imageUris + uri) }
        scheduleAutoSave()
    }

    fun onImageUriRemoved(uri: String) {
        _uiState.update { it.copy(imageUris = it.imageUris - uri) }
        scheduleAutoSave()
    }

    fun onDrawingPathChanged(path: String) {
        _uiState.update { it.copy(drawingPath = path) }
        scheduleAutoSave()
    }

    fun saveFromRichText(title: String, richTextState: RichTextState) {
        autoSaveJob?.cancel()
        val serialized = RichTextSerializer.encode(
            richTextState.annotatedString,
            richTextState.checklistItems
        )
        persist(title, serialized, richTextState)
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2_000)
            richTextStateRef?.let { state ->
                saveFromRichText(currentTitle, state)
            }
        }
    }

    private fun persist(title: String, serializedContent: String, richTextState: RichTextState) {
        if (title.isBlank() && richTextState.plainText.isBlank() && richTextState.checklistItems.isEmpty()) return

        viewModelScope.launch {
            val state = _uiState.value
            val isNew = currentNoteId == -1L
            val note = Note(
                id = if (isNew) 0 else currentNoteId,
                title = title,
                content = serializedContent,
                color = state.color,
                isPinned = state.isPinned,
                isArchived = state.isArchived,
                isHidden = state.isHidden,
                isLocked = state.isLocked,
                folderId = state.folderId,
                tags = state.tags.joinToString(","),
                imageUris = state.imageUris.joinToString(","),
                drawingPath = state.drawingPath,
                updatedAt = System.currentTimeMillis(),
                createdAt = if (isNew) System.currentTimeMillis() else state.createdAt
            )
            val savedId = repository.saveNote(note)
            currentNoteId = savedId
            _uiState.update { it.copy(noteId = savedId, isSaved = true) }
        }
    }

    fun trashNote() {
        if (currentNoteId != -1L) {
            viewModelScope.launch { repository.moveToTrash(currentNoteId) }
        }
    }

    fun togglePin() {
        val state = _uiState.value
        if (state.noteId == -1L) return
        viewModelScope.launch {
            repository.togglePin(Note(id = state.noteId, isPinned = state.isPinned))
            _uiState.update { it.copy(isPinned = !it.isPinned) }
        }
    }

    /**
     * Toggle the lock state for the current note.
     * - To LOCK: requires a PIN to be set first (pass non-blank pin).
     * - To UNLOCK: pass empty pin; we also clear the stored PIN.
     */
    fun toggleLock(pin: String = "") = viewModelScope.launch {
        val state = _uiState.value
        if (state.noteId == -1L) return@launch  // can't lock unsaved notes
        val newLocked = !state.isLocked
        if (newLocked) {
            if (pin.isBlank()) return@launch
            userPreferences.setPin(state.noteId, pin)
            repository.setLocked(state.noteId, true)
            _uiState.update { it.copy(isLocked = true, hasPinSet = true) }
        } else {
            userPreferences.clearPin(state.noteId)
            repository.setLocked(state.noteId, false)
            _uiState.update { it.copy(isLocked = false, hasPinSet = false, requiresUnlock = false) }
        }
    }

    /** Build the current Note snapshot for share/export operations. */
    fun currentNoteSnapshot(title: String, richTextState: RichTextState): Note {
        val state = _uiState.value
        val serialized = RichTextSerializer.encode(
            richTextState.annotatedString,
            richTextState.checklistItems
        )
        return Note(
            id = state.noteId.coerceAtLeast(0),
            title = title,
            content = serialized,
            color = state.color,
            isPinned = state.isPinned,
            isArchived = state.isArchived,
            isHidden = state.isHidden,
            isLocked = state.isLocked,
            folderId = state.folderId,
            tags = state.tags.joinToString(","),
            imageUris = state.imageUris.joinToString(","),
            drawingPath = state.drawingPath,
            updatedAt = System.currentTimeMillis(),
            createdAt = if (state.noteId == -1L) System.currentTimeMillis() else state.createdAt
        )
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        // Note: do not attempt a final save here — viewModelScope is cancelled.
        // EditorScreen calls saveFromRichText on back press as the proper save path.
    }
}
