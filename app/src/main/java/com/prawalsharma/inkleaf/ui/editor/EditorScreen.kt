package com.prawalsharma.inkleaf.ui.editor

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.ui.editor.richtext.*
import com.prawalsharma.inkleaf.ui.lock.NoteLockScreen
import com.prawalsharma.inkleaf.ui.theme.LocalAppFont
import com.prawalsharma.inkleaf.ui.theme.NoteColorGroups
import com.prawalsharma.inkleaf.ui.theme.NoteColors
import com.prawalsharma.inkleaf.ui.theme.needsWhiteContent
import com.prawalsharma.inkleaf.utils.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val richTextState = rememberRichTextState()
    val appFont = LocalAppFont.current
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Must be declared before early-return blocks
    var navigateToDrawing by remember { mutableStateOf(false) }

    // Show drawing canvas as a full-screen overlay
    if (navigateToDrawing) {
        com.prawalsharma.inkleaf.ui.drawing.DrawingScreen(
            existingPath = uiState.drawingPath,
            onSave = { path ->
                viewModel.onDrawingPathChanged(path)
                navigateToDrawing = false
            },
            onBack = { navigateToDrawing = false }
        )
        return
    }

    // Show lock screen if note is locked and not yet authenticated this session
    if (uiState.requiresUnlock) {
        NoteLockScreen(
            hasPinSet = uiState.hasPinSet,
            verifyPin = { candidate -> viewModel.verifyPin(candidate) },
            onUnlocked = viewModel::onUnlocked,
            onSetPin = { pin -> viewModel.onPinSet(uiState.noteId, pin) },
            onCancel = onBack
        )
        return
    }

    // Load content into RichTextState once, when data arrives
    var contentLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !contentLoaded) {
            viewModel.populateRichTextState(richTextState)
            contentLoaded = true
        }
    }

    var showBgColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showHighlightPicker by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showUnlockConfirmDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Hoist string resources so they can be referenced inside onClick lambdas
    val strMsgPdfSaved = stringResource(R.string.msg_pdf_saved)
    val strMsgPdfFailed = stringResource(R.string.msg_pdf_save_failed)
    val strMsgSaveBeforeLocking = stringResource(R.string.msg_save_before_locking)
    val strMsgNoteLocked = stringResource(R.string.msg_note_locked)
    val strMsgNoteUnlocked = stringResource(R.string.msg_note_unlocked)
    val strSharePdfChooser = stringResource(R.string.share_pdf_chooser)
    val strShareNoteChooser = stringResource(R.string.share_note_chooser)
    val strShareTxtChooser = stringResource(R.string.share_txt_chooser)

    val bgColor = Color(uiState.color)
    val isDark = needsWhiteContent(uiState.color)
    val contentColor = if (isDark) Color.White else Color.Black

    // Word / char counts from rich text state
    val wordCount = remember(richTextState.plainText) {
        if (richTextState.plainText.isBlank()) 0
        else richTextState.plainText.trim().split(Regex("\\s+")).size
    }
    val charCount = richTextState.plainText.length

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (wordCount > 0) {
                        Text(
                            "$wordCount words · $charCount chars",
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.saveFromRichText(uiState.title, richTextState)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = contentColor)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePin) {
                        Icon(
                            Icons.Default.PushPin, stringResource(R.string.cd_pin),
                            tint = if (uiState.isPinned) MaterialTheme.colorScheme.primary
                            else contentColor.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { showBgColorPicker = !showBgColorPicker }) {
                        Icon(Icons.Default.Palette, stringResource(R.string.cd_background_color), tint = contentColor)
                    }
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.cd_more), tint = contentColor)
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_save)) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.saveFromRichText(uiState.title, richTextState)
                                showOverflowMenu = false
                            },
                            leadingIcon = { Icon(Icons.Default.Save, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_export_pdf)) },
                            onClick = {
                                showOverflowMenu = false
                                viewModel.saveFromRichText(uiState.title, richTextState)
                                val note = viewModel.currentNoteSnapshot(uiState.title, richTextState)
                                val uri = PdfExporter.exportNote(context, note)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, strSharePdfChooser))
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_save_pdf_downloads)) },
                            onClick = {
                                showOverflowMenu = false
                                viewModel.saveFromRichText(uiState.title, richTextState)
                                val note = viewModel.currentNoteSnapshot(uiState.title, richTextState)
                                val uri = PdfExporter.saveToDownloads(context, note)
                                statusMessage = if (uri != null) strMsgPdfSaved
                                else strMsgPdfFailed
                            },
                            leadingIcon = { Icon(Icons.Default.Download, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_share_as_text)) },
                            onClick = {
                                showOverflowMenu = false
                                viewModel.saveFromRichText(uiState.title, richTextState)
                                val note = viewModel.currentNoteSnapshot(uiState.title, richTextState)
                                val intent = com.prawalsharma.inkleaf.utils.TextExporter.buildShareTextIntent(note)
                                context.startActivity(Intent.createChooser(intent, strShareNoteChooser))
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_export_txt)) },
                            onClick = {
                                showOverflowMenu = false
                                viewModel.saveFromRichText(uiState.title, richTextState)
                                val note = viewModel.currentNoteSnapshot(uiState.title, richTextState)
                                val uri = com.prawalsharma.inkleaf.utils.TextExporter.exportToTxtFile(context, note)
                                val intent = com.prawalsharma.inkleaf.utils.TextExporter.buildShareTxtFileIntent(uri)
                                context.startActivity(Intent.createChooser(intent, strShareTxtChooser))
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.TextSnippet, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (uiState.isLocked) R.string.menu_unlock_note else R.string.menu_lock_note)) },
                            onClick = {
                                showOverflowMenu = false
                                if (uiState.noteId == -1L) {
                                    statusMessage = strMsgSaveBeforeLocking
                                } else if (uiState.isLocked) {
                                    showUnlockConfirmDialog = true
                                } else {
                                    showSetPinDialog = true
                                }
                            },
                            leadingIcon = {
                                Icon(if (uiState.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_move_to_folder)) },
                            onClick = { showFolderPicker = true; showOverflowMenu = false },
                            leadingIcon = { Icon(Icons.Default.Folder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_drawing_canvas)) },
                            onClick = { navigateToDrawing = true; showOverflowMenu = false },
                            leadingIcon = { Icon(Icons.Default.Draw, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = { showDeleteDialog = true; showOverflowMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor,
                    titleContentColor = contentColor
                )
            )
        },
        bottomBar = {
            FormattingToolbar(
                state = richTextState,
                onShowTextColorPicker = { showTextColorPicker = true },
                onShowHighlightColorPicker = { showHighlightPicker = true },
                modifier = Modifier.imePadding().navigationBarsPadding()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(padding)
        ) {
            // Note background color picker
            if (showBgColorPicker) {
                BgColorPickerBar(
                    selectedColor = uiState.color,
                    onColorSelected = { color ->
                        viewModel.onColorChange(color)
                        showBgColorPicker = false
                    }
                )
            }

            // Scrollable note body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Title
                RichTitleField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    contentColor = contentColor,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = contentColor.copy(alpha = 0.1f)
                )

                // Tag chips row
                TagChipsRow(
                    tags = uiState.tags,
                    contentColor = contentColor,
                    onTagRemove = viewModel::onTagRemove,
                    onAddTag = { showAddTagDialog = true }
                )

                // Rich text body
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (richTextState.plainText.isEmpty()) {
                        Text(
                            stringResource(R.string.editor_body_hint),
                            style = TextStyle(
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = contentColor.copy(alpha = 0.4f),
                                fontFamily = appFont
                            )
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = richTextState.textFieldValue,
                        onValueChange = richTextState::onValueChange,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = contentColor,
                            fontFamily = appFont
                        ),
                        cursorBrush = SolidColor(contentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                    )
                }

                // Checklist section
                ChecklistSection(
                    state = richTextState,
                    contentColor = contentColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Image attachments
                ImageAttachmentSection(
                    imageUris = uiState.imageUris,
                    contentColor = contentColor,
                    onImageAdded = viewModel::onImageUriAdded,
                    onImageRemoved = viewModel::onImageUriRemoved,
                    modifier = Modifier.fillMaxWidth()
                )

                // Drawing thumbnail
                if (uiState.drawingPath.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        coil.compose.AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(java.io.File(uiState.drawingPath)).build(),
                            contentDescription = stringResource(R.string.cd_drawing),
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { navigateToDrawing = true },
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { navigateToDrawing = true }) { Text(stringResource(R.string.btn_edit_drawing)) }
                    }
                }
            }
        }
    }

    // Color picker dialogs
    if (showTextColorPicker) {
        ColorPickerDialog(
            title = stringResource(R.string.dialog_text_color_title),
            selectedColor = richTextState.textColor,
            isHighlight = false,
            onColorSelected = richTextState::applyTextColor,
            onClear = richTextState::resetTextColor,
            onDismiss = { showTextColorPicker = false }
        )
    }
    if (showHighlightPicker) {
        ColorPickerDialog(
            title = stringResource(R.string.dialog_highlight_color_title),
            selectedColor = richTextState.highlightColor,
            isHighlight = true,
            onColorSelected = richTextState::applyHighlightColor,
            onClear = richTextState::resetHighlightColor,
            onDismiss = { showHighlightPicker = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_note_title)) },
            text = { Text(stringResource(R.string.dialog_delete_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.trashNote()
                    showDeleteDialog = false
                    onBack()
                }) { Text(stringResource(R.string.btn_move_to_trash), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            currentFolderId = uiState.folderId,
            viewModel = viewModel,
            onDismiss = { showFolderPicker = false }
        )
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onConfirm = { tag -> viewModel.onTagAdd(tag); showAddTagDialog = false },
            onDismiss = { showAddTagDialog = false }
        )
    }

    if (showSetPinDialog) {
        SetPinDialog(
            onConfirm = { pin ->
                viewModel.toggleLock(pin)
                showSetPinDialog = false
                statusMessage = strMsgNoteLocked
            },
            onDismiss = { showSetPinDialog = false }
        )
    }

    if (showUnlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_unlock_note_title)) },
            text = { Text(stringResource(R.string.dialog_unlock_note_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleLock()
                    showUnlockConfirmDialog = false
                    statusMessage = strMsgNoteUnlocked
                }) { Text(stringResource(R.string.btn_unlock)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockConfirmDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Auto-dismissing status message
    statusMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2200)
            statusMessage = null
        }
        androidx.compose.material3.Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(msg, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun SetPinDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val errPinLength = stringResource(R.string.error_pin_length)
    val errPinMismatch = stringResource(R.string.error_pin_mismatch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_set_pin_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.dialog_set_pin_message),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text(stringResource(R.string.pin_label)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                    label = { Text(stringResource(R.string.confirm_pin_label)) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length != 4 -> error = errPinLength
                    pin != confirmPin -> error = errPinMismatch
                    else -> onConfirm(pin)
                }
            }) { Text(stringResource(R.string.btn_lock)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun RichTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                stringResource(R.string.title),
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.4f)
                )
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            ),
            cursorBrush = SolidColor(contentColor),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BgColorPickerBar(
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.animateContentSize()
    ) {
        Column {
            // Group tabs
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(NoteColorGroups, key = { it.name }) { group ->
                    FilterChip(
                        selected = expandedGroup == group.name,
                        onClick = {
                            expandedGroup = if (expandedGroup == group.name) null else group.name
                        },
                        label = { Text(group.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            // Color swatches — show all if no group selected, else the selected group
            val colors = if (expandedGroup == null) NoteColors
            else NoteColorGroups.firstOrNull { it.name == expandedGroup }?.colors ?: NoteColors
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colors) { color ->
                    val checkTint = if (needsWhiteContent(color)) Color.White else Color.Black
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .clickable { onColorSelected(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == selectedColor) {
                            Icon(Icons.Default.Check, null, tint = checkTint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TagChipsRow(
    tags: List<String>,
    contentColor: Color,
    onTagRemove: (String) -> Unit,
    onAddTag: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        items(tags) { tag ->
            InputChip(
                selected = false,
                onClick = {},
                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close, stringResource(R.string.cd_remove_tag),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onTagRemove(tag) }
                    )
                }
            )
        }
        item {
            AssistChip(
                onClick = onAddTag,
                label = { Text(stringResource(R.string.btn_add_tag), style = MaterialTheme.typography.labelSmall) },
                leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) }
            )
        }
    }
}

@Composable
fun AddTagDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var tagText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_tag_title)) },
        text = {
            OutlinedTextField(
                value = tagText,
                onValueChange = { tagText = it },
                label = { Text(stringResource(R.string.tag_name_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (tagText.isNotBlank()) onConfirm(tagText.trim()) },
                enabled = tagText.isNotBlank()
            ) { Text(stringResource(R.string.btn_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun FolderPickerDialog(
    currentFolderId: Long?,
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val folders by viewModel.foldersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_move_folder_title)) },
        text = {
            Column {
                // "No folder" option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onFolderChange(null); onDismiss() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOff, null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.folder_picker_no_folder))
                    if (currentFolderId == null) {
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                HorizontalDivider()
                folders.forEach { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onFolderChange(folder.id); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null, tint = Color(folder.color))
                        Spacer(Modifier.width(12.dp))
                        Text(folder.name, modifier = Modifier.weight(1f))
                        if (currentFolderId == folder.id) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_done)) } }
    )
}
