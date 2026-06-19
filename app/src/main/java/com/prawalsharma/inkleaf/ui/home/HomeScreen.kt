package com.prawalsharma.inkleaf.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.data.model.Note
import com.prawalsharma.inkleaf.data.model.SortOrder
import com.prawalsharma.inkleaf.ui.theme.needsWhiteContent
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToHidden: () -> Unit,
    bottomPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val isSelecting = selectedIds.isNotEmpty()

    BackHandler(enabled = isSelecting) { viewModel.clearSelection() }
    BackHandler(enabled = fabExpanded) { fabExpanded = false }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (isSelecting) {
                // Contextual top bar for multi-select
                TopAppBar(
                    title = { Text(stringResource(R.string.n_selected, selectedIds.size)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, stringResource(R.string.cd_clear_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.pinSelected() }) {
                            Icon(Icons.Default.PushPin, null)
                        }
                        IconButton(onClick = { viewModel.archiveSelected() }) {
                            Icon(Icons.Default.Archive, null)
                        }
                        IconButton(onClick = { viewModel.hideSelected() }) {
                            Icon(Icons.Default.VisibilityOff, null)
                        }
                        IconButton(onClick = { viewModel.trashSelected() }) {
                            Icon(Icons.Default.Delete, null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            } else if (uiState.isSearchActive) {
                SearchTopBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClose = viewModel::onSearchToggle
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconButton(onClick = viewModel::onSearchToggle) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                        }
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.cd_sort))
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.displayName(context)) },
                                    onClick = {
                                        viewModel.onSortOrderChange(sort)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOrder == sort) {
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more))
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.archive)) },
                                onClick = { onNavigateToArchive(); showOverflowMenu = false },
                                leadingIcon = { Icon(Icons.Default.Archive, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.trash)) },
                                onClick = { onNavigateToTrash(); showOverflowMenu = false },
                                leadingIcon = { Icon(Icons.Default.Delete, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.hidden_notes)) },
                                onClick = { onNavigateToHidden(); showOverflowMenu = false },
                                leadingIcon = { Icon(Icons.Default.VisibilityOff, null) }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            // Speed-dial FAB
            Box(contentAlignment = Alignment.BottomEnd) {
                // Scrim behind speed-dial
                if (fabExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.32f))
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.7f, animationSpec = tween(120)),
                        exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.7f, animationSpec = tween(100))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            SpeedDialItem(label = stringResource(R.string.fab_drawing), icon = Icons.Default.Brush) {
                                fabExpanded = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNewNote()
                            }
                            SpeedDialItem(label = stringResource(R.string.fab_checklist), icon = Icons.Default.CheckBox) {
                                fabExpanded = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNewNote()
                            }
                            SpeedDialItem(label = stringResource(R.string.fab_note), icon = Icons.Default.Edit) {
                                fabExpanded = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNewNote()
                            }
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (fabExpanded) {
                                fabExpanded = false
                            } else {
                                fabExpanded = true
                            }
                        },
                        icon = {
                            Icon(
                                if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = stringResource(R.string.new_note)
                            )
                        },
                        text = { Text(stringResource(R.string.fab_new)) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            if (uiState.notes.isEmpty()) {
                if (uiState.searchQuery.isNotBlank()) {
                    RichEmptyState(
                        illustration = { EmptySearchIllustration() },
                        title = stringResource(R.string.empty_search_title),
                        subtitle = stringResource(R.string.empty_search_subtitle),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    RichEmptyState(
                        illustration = { NotesEmptyIllustration() },
                        title = stringResource(R.string.empty_notes_title),
                        subtitle = stringResource(R.string.empty_notes_subtitle),
                        ctaLabel = stringResource(R.string.empty_notes_cta),
                        onCta = onNewNote,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            } else {
                val gridState = rememberLazyStaggeredGridState()
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 8.dp, end = 8.dp, top = 8.dp,
                        bottom = 8.dp + bottomPadding.calculateBottomPadding()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(spring()) + scaleIn(initialScale = 0.92f, animationSpec = spring())
                        ) {
                            NoteCard(
                                note = note,
                                isSelected = note.id in selectedIds,
                                isSelecting = isSelecting,
                                onClick = {
                                    if (isSelecting) {
                                        viewModel.toggleSelection(note.id)
                                    } else {
                                        onNoteClick(note.id)
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleSelection(note.id)
                                },
                                onPin = { viewModel.togglePin(note) },
                                onArchive = { viewModel.archiveNote(note) },
                                onDelete = { viewModel.trashNote(note) },
                                onHide = { viewModel.hideNote(note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedDialItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            tonalElevation = 2.dp
        ) {
            Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(8.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_notes_placeholder)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_search))
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            focusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onHide: (() -> Unit)? = null,
    pinMenuLabel: String? = null,
    pinMenuIcon: ImageVector = Icons.Default.PushPin,
    showArchiveAction: Boolean = true,
    isSelected: Boolean = false,
    isSelecting: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    val bgColor = Color(note.color)
    val contentColor = if (needsWhiteContent(note.color)) Color.White else Color.Black
    val context = LocalContext.current

    val borderModifier = if (isSelected)
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (!isSelecting) {
                        onLongClick()
                    } else {
                        onClick()
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(12.dp)) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (note.isLocked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.cd_locked),
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.note_locked),
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                } else if (note.content.isNotBlank()) {
                    val preview = com.prawalsharma.inkleaf.utils.RichTextSerializer.toPlainText(note.content)
                    if (preview.isNotBlank()) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall.copy(color = contentColor.copy(alpha = 0.8f)),
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatDate(context, note.updatedAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                )
            }

            // Check overlay when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            if (!isSelected && !isSelecting) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.cd_pinned),
                            tint = contentColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_note_options),
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Long-press context menu (only shown when not in selection mode)
            if (!isSelecting) {
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(pinMenuLabel ?: if (note.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin)) },
                        onClick = { onPin(); showMenu = false },
                        leadingIcon = { Icon(pinMenuIcon, null) }
                    )
                    if (showArchiveAction) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.archive)) },
                            onClick = { onArchive(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Archive, null) }
                        )
                    }
                    if (onHide != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hide_note)) },
                            onClick = { onHide(); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        }
    }
}

// ─── Rich Empty States ────────────────────────────────────────────────────────

@Composable
fun RichEmptyState(
    illustration: @Composable () -> Unit,
    title: String,
    subtitle: String,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(140.dp)) { illustration() }
        Spacer(Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (ctaLabel != null && onCta != null) {
            Spacer(Modifier.height(20.dp))
            FilledTonalButton(onClick = onCta) {
                Text(ctaLabel)
            }
        }
    }
}

// Backwards-compat version for ArchiveTrashScreens
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.AutoMirrored.Filled.NoteAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

// ─── Illustrations (Compose Canvas) ──────────────────────────────────────────

@Composable
fun NotesEmptyIllustration() {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // Leaf body
        val leafPath = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            cubicTo(w * 0.85f, h * 0.1f, w * 0.9f, h * 0.5f, w * 0.5f, h * 0.85f)
            cubicTo(w * 0.1f, h * 0.5f, w * 0.15f, h * 0.1f, w * 0.5f, h * 0.1f)
        }
        drawPath(leafPath, primary)
        // Leaf midrib
        drawLine(secondary, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.8f), strokeWidth = 3.dp.toPx())
        // Small veins
        for (i in 1..4) {
            val y = h * (0.25f + i * 0.1f)
            drawLine(secondary, Offset(w * 0.5f, y), Offset(w * 0.65f, y - h * 0.05f), strokeWidth = 1.5f.dp.toPx())
            drawLine(secondary, Offset(w * 0.5f, y), Offset(w * 0.35f, y - h * 0.05f), strokeWidth = 1.5f.dp.toPx())
        }
        // Pen/ink stroke crossing the leaf
        val penPath = Path().apply {
            moveTo(w * 0.35f, h * 0.7f)
            lineTo(w * 0.75f, h * 0.25f)
        }
        drawPath(penPath, secondary, style = Stroke(width = 3.dp.toPx()))
        // Pen tip
        drawCircle(secondary, radius = 4.dp.toPx(), center = Offset(w * 0.75f, h * 0.25f))
    }
}

@Composable
fun FoldersEmptyIllustration() {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // Back folder
        drawRoundRect(secondary, topLeft = Offset(w * 0.15f, h * 0.3f), size = Size(w * 0.7f, h * 0.5f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
        // Front folder tab
        val tabPath = Path().apply {
            moveTo(w * 0.22f, h * 0.2f)
            lineTo(w * 0.45f, h * 0.2f)
            cubicTo(w * 0.5f, h * 0.2f, w * 0.5f, h * 0.28f, w * 0.55f, h * 0.28f)
            lineTo(w * 0.78f, h * 0.28f)
            lineTo(w * 0.78f, h * 0.72f)
            lineTo(w * 0.22f, h * 0.72f)
            close()
        }
        drawPath(tabPath, primary)
        // Lines inside folder
        drawLine(Color.White.copy(alpha = 0.5f), Offset(w * 0.33f, h * 0.45f), Offset(w * 0.67f, h * 0.45f), 2.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.5f), Offset(w * 0.33f, h * 0.55f), Offset(w * 0.67f, h * 0.55f), 2.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.35f), Offset(w * 0.33f, h * 0.63f), Offset(w * 0.55f, h * 0.63f), 2.dp.toPx())
    }
}

@Composable
fun ArchiveEmptyIllustration() {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // Box body
        drawRoundRect(secondary, topLeft = Offset(w * 0.1f, h * 0.4f), size = Size(w * 0.8f, h * 0.45f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
        // Lid
        val lidPath = Path().apply {
            moveTo(w * 0.05f, h * 0.37f)
            lineTo(w * 0.95f, h * 0.37f)
            lineTo(w * 0.88f, h * 0.44f)
            lineTo(w * 0.12f, h * 0.44f)
            close()
        }
        drawPath(lidPath, primary)
        // Flap on lid
        drawRoundRect(primary.copy(alpha = 0.8f), topLeft = Offset(w * 0.35f, h * 0.3f), size = Size(w * 0.3f, h * 0.1f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
        // Down arrow inside box
        drawLine(secondary.copy(alpha = 0.7f), Offset(w * 0.5f, h * 0.52f), Offset(w * 0.5f, h * 0.72f), 3.dp.toPx())
        drawLine(secondary.copy(alpha = 0.7f), Offset(w * 0.5f, h * 0.72f), Offset(w * 0.41f, h * 0.63f), 3.dp.toPx())
        drawLine(secondary.copy(alpha = 0.7f), Offset(w * 0.5f, h * 0.72f), Offset(w * 0.59f, h * 0.63f), 3.dp.toPx())
    }
}

@Composable
fun TrashEmptyIllustration() {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    val secondary = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        // Trash can body
        val bodyPath = Path().apply {
            moveTo(w * 0.22f, h * 0.35f)
            lineTo(w * 0.3f, h * 0.82f)
            cubicTo(w * 0.3f, h * 0.87f, w * 0.7f, h * 0.87f, w * 0.7f, h * 0.82f)
            lineTo(w * 0.78f, h * 0.35f)
            close()
        }
        drawPath(bodyPath, secondary)
        // Lid
        drawRoundRect(primary, topLeft = Offset(w * 0.15f, h * 0.28f), size = Size(w * 0.7f, h * 0.08f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
        // Handle
        drawRoundRect(primary.copy(alpha = 0.8f), topLeft = Offset(w * 0.38f, h * 0.2f), size = Size(w * 0.24f, h * 0.1f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
        // Lines on body
        drawLine(Color.White.copy(alpha = 0.4f), Offset(w * 0.4f, h * 0.45f), Offset(w * 0.38f, h * 0.75f), 2.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.4f), Offset(w * 0.5f, h * 0.44f), Offset(w * 0.5f, h * 0.76f), 2.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.4f), Offset(w * 0.6f, h * 0.45f), Offset(w * 0.62f, h * 0.75f), 2.dp.toPx())
    }
}

@Composable
fun EmptySearchIllustration() {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        val cx = w * 0.45f; val cy = h * 0.42f; val r = minOf(w, h) * 0.28f
        drawCircle(color = primary, radius = r, center = Offset(cx, cy), style = Stroke(width = 4.dp.toPx()))
        drawLine(primary, Offset(cx + r * 0.7f, cy + r * 0.7f), Offset(w * 0.82f, h * 0.78f), strokeWidth = 5.dp.toPx())
    }
}

private fun formatDate(context: android.content.Context, millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000 -> context.getString(R.string.time_just_now)
        diff < 3_600_000 -> context.getString(R.string.time_minutes_ago, diff / 60_000)
        diff < 86_400_000 -> context.getString(R.string.time_hours_ago, diff / 3_600_000)
        diff < 604_800_000 -> context.getString(R.string.time_days_ago, diff / 86_400_000)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

private fun SortOrder.displayName(context: android.content.Context) = when (this) {
    SortOrder.DATE_MODIFIED -> context.getString(R.string.date_modified)
    SortOrder.DATE_CREATED -> context.getString(R.string.date_created)
    SortOrder.TITLE -> context.getString(R.string.title)
    SortOrder.COLOR -> context.getString(R.string.color)
}
