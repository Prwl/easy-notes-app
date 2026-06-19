package com.prawalsharma.inkleaf.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.data.model.Note
import com.prawalsharma.inkleaf.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- Archive ViewModel ---
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {
    val notes = repository.getArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unarchive(note: Note) = viewModelScope.launch { repository.unarchiveNote(note.id) }
    fun trash(note: Note) = viewModelScope.launch { repository.moveToTrash(note.id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archive)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (notes.isEmpty()) {
                RichEmptyState(
                    illustration = { ArchiveEmptyIllustration() },
                    title = stringResource(R.string.empty_archive_title),
                    subtitle = stringResource(R.string.empty_archive_subtitle),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onPin = { viewModel.unarchive(note) },
                            onArchive = { viewModel.unarchive(note) },
                            onDelete = { viewModel.trash(note) }
                        )
                    }
                }
            }
        }
    }
}

// --- Trash ViewModel ---
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {
    val notes = repository.getTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(note: Note) = viewModelScope.launch { repository.restoreFromTrash(note.id) }
    fun deletePermanently(note: Note) = viewModelScope.launch { repository.deleteNotePermanently(note) }
    fun emptyTrash() = viewModelScope.launch { repository.emptyTrash() }
}

// --- Hidden Notes ViewModel ---
@HiltViewModel
class HiddenNotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {
    val notes = repository.getHiddenNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun unhide(note: Note) = viewModelScope.launch { repository.unhideNote(note.id) }
    fun trash(note: Note) = viewModelScope.launch { repository.moveToTrash(note.id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenNotesScreen(
    onBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    viewModel: HiddenNotesViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hidden_notes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (notes.isEmpty()) {
                RichEmptyState(
                    illustration = { EmptySearchIllustration() },
                    title = stringResource(R.string.empty_hidden_title),
                    subtitle = stringResource(R.string.empty_hidden_subtitle),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onPin = { viewModel.unhide(note) },
                            onArchive = { viewModel.unhide(note) },
                            onDelete = { viewModel.trash(note) },
                            pinMenuLabel = stringResource(R.string.unhide_note),
                            pinMenuIcon = Icons.Default.Visibility,
                            showArchiveAction = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var showEmptyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (notes.isNotEmpty()) {
                        TextButton(onClick = { showEmptyDialog = true }) {
                            Text(stringResource(R.string.btn_empty_trash), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (notes.isEmpty()) {
                RichEmptyState(
                    illustration = { TrashEmptyIllustration() },
                    title = stringResource(R.string.empty_trash_title),
                    subtitle = stringResource(R.string.empty_trash_subtitle),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { viewModel.restore(note) },
                            onPin = { viewModel.restore(note) },
                            onArchive = { viewModel.restore(note) },
                            onDelete = { viewModel.deletePermanently(note) }
                        )
                    }
                }
            }
        }
    }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text(stringResource(R.string.dialog_empty_trash_title)) },
            text = { Text(stringResource(R.string.dialog_empty_trash_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyTrash(); showEmptyDialog = false }) {
                    Text(stringResource(R.string.btn_delete_all), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
