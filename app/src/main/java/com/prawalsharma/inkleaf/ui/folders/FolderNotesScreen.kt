package com.prawalsharma.inkleaf.ui.folders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.ui.home.NoteCard
import com.prawalsharma.inkleaf.ui.home.RichEmptyState
import com.prawalsharma.inkleaf.ui.home.FoldersEmptyIllustration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderNotesScreen(
    onBack: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    viewModel: FolderNotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folderName = uiState.folder?.name ?: stringResource(R.string.fallback_folder_name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName, fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.AutoMirrored.Filled.NoteAdd, stringResource(R.string.cd_new_note_folder), tint = Color.White)
            }
        }
    ) { padding ->
        if (uiState.notes.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                RichEmptyState(
                    illustration = { FoldersEmptyIllustration() },
                    title = stringResource(R.string.empty_folder_notes_title),
                    subtitle = stringResource(R.string.empty_folder_notes_subtitle),
                    ctaLabel = stringResource(R.string.empty_folder_notes_cta),
                    onCta = onNewNote
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(uiState.notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note.id) },
                        onPin = { viewModel.togglePin(note) },
                        onArchive = {},
                        onDelete = { viewModel.trashNote(note) }
                    )
                }
            }
        }
    }
}
