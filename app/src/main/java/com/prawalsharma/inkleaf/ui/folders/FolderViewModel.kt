package com.prawalsharma.inkleaf.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prawalsharma.inkleaf.data.model.Folder
import com.prawalsharma.inkleaf.data.model.Note
import com.prawalsharma.inkleaf.data.repository.FolderRepository
import com.prawalsharma.inkleaf.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderWithCount(val folder: Folder, val noteCount: Int)

data class FolderListUiState(
    val folders: List<FolderWithCount> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FolderListUiState> = folderRepository.getAllFolders()
        .flatMapLatest { folders ->
            if (folders.isEmpty()) {
                flowOf(FolderListUiState(emptyList(), isLoading = false))
            } else {
                combine(
                    folders.map { folder ->
                        folderRepository.getNoteCountForFolder(folder.id)
                            .map { count -> FolderWithCount(folder, count) }
                    }
                ) { counts -> FolderListUiState(counts.toList(), isLoading = false) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderListUiState())

    fun createFolder(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) folderRepository.createFolder(name)
    }

    fun renameFolder(folder: Folder, newName: String) = viewModelScope.launch {
        if (newName.isNotBlank()) folderRepository.renameFolder(folder, newName)
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        folderRepository.deleteFolder(folder)
    }
}

// ─── FolderNotesViewModel ──────────────────────────────────────────────────

data class FolderNotesUiState(
    val folder: Folder? = null,
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FolderNotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val folderRepository: FolderRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val folderId: Long = savedStateHandle.get<Long>("folderId") ?: -1L

    val uiState: StateFlow<FolderNotesUiState> = combine(
        flow { emit(folderRepository.getFolderById(folderId)) },
        noteRepository.getNotesByFolder(folderId)
    ) { folder, notes ->
        FolderNotesUiState(folder = folder, notes = notes, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderNotesUiState())

    fun trashNote(note: Note) = viewModelScope.launch { noteRepository.moveToTrash(note.id) }
    fun togglePin(note: Note) = viewModelScope.launch { noteRepository.togglePin(note) }
}
