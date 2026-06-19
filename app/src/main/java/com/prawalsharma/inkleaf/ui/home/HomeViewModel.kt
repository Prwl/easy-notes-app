package com.prawalsharma.inkleaf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prawalsharma.inkleaf.data.model.Note
import com.prawalsharma.inkleaf.data.model.SortOrder
import com.prawalsharma.inkleaf.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.DATE_MODIFIED,
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.DATE_MODIFIED)
    private val _isSearchActive = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery,
        _sortOrder,
        _isSearchActive
    ) { query, sort, isSearchActive ->
        Triple(query, sort, isSearchActive)
    }.flatMapLatest { (query, sort, isSearchActive) ->
        val notesFlow = if (query.isNotBlank()) {
            repository.searchNotes(query)
        } else {
            repository.getActiveNotes(sort)
        }
        notesFlow.map { notes ->
            HomeUiState(
                notes = notes,
                searchQuery = query,
                sortOrder = sort,
                isSearchActive = isSearchActive
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    // ─── Selection ────────────────────────────────────────────────────────
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun toggleSelection(id: Long) {
        _selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun archiveSelected() = viewModelScope.launch {
        _selectedIds.value.forEach { repository.archiveNote(it) }
        _selectedIds.value = emptySet()
    }

    fun trashSelected() = viewModelScope.launch {
        _selectedIds.value.forEach { repository.moveToTrash(it) }
        _selectedIds.value = emptySet()
    }

    fun hideSelected() = viewModelScope.launch {
        _selectedIds.value.forEach { repository.hideNote(it) }
        _selectedIds.value = emptySet()
    }

    fun pinSelected() = viewModelScope.launch {
        val ids = _selectedIds.value
        val notes = uiState.value.notes.filter { it.id in ids }
        val anyUnpinned = notes.any { !it.isPinned }
        notes.forEach { note ->
            if (anyUnpinned != note.isPinned) repository.togglePin(note)
        }
        _selectedIds.value = emptySet()
    }

    // ─── Search / Sort ────────────────────────────────────────────────────
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSearchToggle() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) _searchQuery.value = ""
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        repository.togglePin(note)
    }

    fun archiveNote(note: Note) = viewModelScope.launch {
        repository.archiveNote(note.id)
    }

    fun trashNote(note: Note) = viewModelScope.launch {
        repository.moveToTrash(note.id)
    }

    fun hideNote(note: Note) = viewModelScope.launch {
        repository.hideNote(note.id)
    }
}
