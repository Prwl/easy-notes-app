package com.prawalsharma.inkleaf.ui.editor.richtext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.prawalsharma.inkleaf.R

private val textColorPalette = listOf(
    Color.Black, Color(0xFF212121), Color(0xFF424242),
    Color(0xFF757575), Color(0xFF9E9E9E), Color.White,
    Color(0xFFD32F2F), Color(0xFFF44336), Color(0xFFFF5722),
    Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4),
    Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50),
    Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B),
    Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFF795548),
    Color(0xFF607D8B), Color(0xFF37474F), Color(0xFF1A237E),
    Color(0xFF0D47A1), Color(0xFF004D40), Color(0xFF1B5E20),
)

private val highlightPalette = listOf(
    Color(0xFFFFF59D), Color(0xFFFFE082), Color(0xFFFFCC80),
    Color(0xFFFFAB91), Color(0xFFF48FB1), Color(0xFFCE93D8),
    Color(0xFF9FA8DA), Color(0xFF90CAF9), Color(0xFF80DEEA),
    Color(0xFFA5D6A7), Color(0xFFE6EE9C), Color(0xFFF5F5F5),
    Color(0xFFFFD54F), Color(0xFFFF8A65), Color(0xFFE57373),
    Color(0xFF80CBC4), Color(0xFF81D4FA), Color(0xFFC5E1A5),
    Color(0xFFB39DDB), Color(0xFFFF8A80), Color(0xFF69F0AE),
    Color(0xFF40C4FF), Color(0xFFE040FB), Color(0xFFFFD180),
    Color(0xFFCCFF90), Color(0xFFEA80FC), Color(0xFF82B1FF),
    Color.Transparent, Color.White, Color.Unspecified,
)

@Composable
fun ColorPickerDialog(
    title: String,
    selectedColor: Color,
    isHighlight: Boolean = false,
    onColorSelected: (Color) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = if (isHighlight) highlightPalette else textColorPalette

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                    }
                }

                Spacer(Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(palette) { color ->
                        ColorSwatch(
                            color = color,
                            isSelected = color == selectedColor,
                            onClick = { onColorSelected(color); onDismiss() }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { onClear(); onDismiss() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.btn_clear))
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (color == Color.Unspecified) Color.LightGray else color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
