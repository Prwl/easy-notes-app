package com.prawalsharma.inkleaf.ui.editor.richtext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prawalsharma.inkleaf.R

@Composable
fun FormattingToolbar(
    state: RichTextState,
    onShowTextColorPicker: () -> Unit,
    onShowHighlightColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showAlignMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo
            item {
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    label = stringResource(R.string.cd_undo),
                    active = false,
                    enabled = state.canUndo,
                    onClick = state::undo
                )
            }
            // Redo
            item {
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    label = stringResource(R.string.toolbar_redo),
                    active = false,
                    enabled = state.canRedo,
                    onClick = state::redo
                )
            }

            item { ToolbarDivider() }

            // Bold
            item {
                ToolbarIconButton(
                    icon = Icons.Default.FormatBold,
                    label = stringResource(R.string.toolbar_bold),
                    active = state.isBold,
                    onClick = state::toggleBold
                )
            }
            // Italic
            item {
                ToolbarIconButton(
                    icon = Icons.Default.FormatItalic,
                    label = stringResource(R.string.toolbar_italic),
                    active = state.isItalic,
                    onClick = state::toggleItalic
                )
            }
            // Underline
            item {
                ToolbarIconButton(
                    icon = Icons.Default.FormatUnderlined,
                    label = stringResource(R.string.toolbar_underline),
                    active = state.isUnderline,
                    onClick = state::toggleUnderline
                )
            }
            // Strikethrough
            item {
                ToolbarIconButton(
                    icon = Icons.Default.FormatStrikethrough,
                    label = stringResource(R.string.toolbar_strikethrough),
                    active = state.isStrikethrough,
                    onClick = state::toggleStrikethrough
                )
            }

            item { ToolbarDivider() }

            // Font size
            item {
                Box {
                    TextButton(
                        onClick = { showFontSizeMenu = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = state.fontSize.toInt().toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showFontSizeMenu, onDismissRequest = { showFontSizeMenu = false }) {
                        listOf(12f, 14f, 16f, 18f, 20f, 24f, 28f, 32f, 36f).forEach { size ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = size.toInt().toString(),
                                        fontSize = size.sp,
                                        fontWeight = if (state.fontSize == size) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = { state.applyFontSize(size); showFontSizeMenu = false }
                            )
                        }
                    }
                }
            }

            item { ToolbarDivider() }

            // Text color
            item {
                ColorDotButton(
                    color = if (state.textColor == Color.Unspecified) Color.Black else state.textColor,
                    icon = Icons.Default.FormatColorText,
                    label = stringResource(R.string.toolbar_text_color),
                    onClick = onShowTextColorPicker
                )
            }

            // Highlight color
            item {
                ColorDotButton(
                    color = if (state.highlightColor == Color.Unspecified) Color.Yellow else state.highlightColor,
                    icon = Icons.Default.BorderColor,
                    label = stringResource(R.string.toolbar_highlight),
                    onClick = onShowHighlightColorPicker
                )
            }

            item { ToolbarDivider() }

            // Alignment
            item {
                Box {
                    ToolbarIconButton(
                        icon = when (state.textAlign) {
                            TextAlign.Center -> Icons.Default.FormatAlignCenter
                            TextAlign.End -> Icons.AutoMirrored.Filled.FormatAlignRight
                            TextAlign.Justify -> Icons.Default.FormatAlignJustify
                            else -> Icons.AutoMirrored.Filled.FormatAlignLeft
                        },
                        label = stringResource(R.string.toolbar_alignment),
                        active = state.textAlign != TextAlign.Start,
                        onClick = { showAlignMenu = true }
                    )
                    DropdownMenu(expanded = showAlignMenu, onDismissRequest = { showAlignMenu = false }) {
                        listOf(
                            Triple(TextAlign.Start, Icons.AutoMirrored.Filled.FormatAlignLeft, stringResource(R.string.toolbar_align_left)),
                            Triple(TextAlign.Center, Icons.Default.FormatAlignCenter, stringResource(R.string.toolbar_align_center)),
                            Triple(TextAlign.End, Icons.AutoMirrored.Filled.FormatAlignRight, stringResource(R.string.toolbar_align_right)),
                            Triple(TextAlign.Justify, Icons.Default.FormatAlignJustify, stringResource(R.string.toolbar_align_justify))
                        ).forEach { (align, icon, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = {
                                    Icon(
                                        icon, null,
                                        tint = if (state.textAlign == align) MaterialTheme.colorScheme.primary
                                        else LocalContentColor.current
                                    )
                                },
                                onClick = { state.applyAlignment(align); showAlignMenu = false }
                            )
                        }
                    }
                }
            }

            item { ToolbarDivider() }

            // Checklist toggle
            item {
                ToolbarIconButton(
                    icon = Icons.Default.CheckBox,
                    label = stringResource(R.string.toolbar_add_checklist),
                    active = false,
                    onClick = state::addChecklistItem
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val tint = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ColorDotButton(
    color: Color,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(14.dp, 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
