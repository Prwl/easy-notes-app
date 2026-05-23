package com.prawalsharma.inkleaf.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prawalsharma.inkleaf.R
import com.prawalsharma.inkleaf.ui.editor.richtext.RichTextState

@Composable
fun ChecklistSection(
    state: RichTextState,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val items = state.checklistItems
    if (items.isEmpty()) return

    Column(modifier = modifier) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = contentColor.copy(alpha = 0.15f)
        )
        Text(
            stringResource(R.string.checklist_title),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        items.forEachIndexed { index, item ->
            key(item.id) {
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    ChecklistItemRow(
                        text = item.text,
                        checked = item.checked,
                        contentColor = contentColor,
                        onTextChange = { text -> state.updateChecklistItem(index, text = text) },
                        onCheckedChange = { checked -> state.updateChecklistItem(index, checked = checked) },
                        onDelete = { state.removeChecklistItem(index) }
                    )
                }
            }
        }

        TextButton(
            onClick = state::addChecklistItem,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.btn_add_item), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ChecklistItemRow(
    text: String,
    checked: Boolean,
    contentColor: Color,
    onTextChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(36.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = contentColor.copy(alpha = 0.6f)
            )
        )

        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    stringResource(R.string.checklist_item_hint),
                    style = TextStyle(
                        fontSize = 15.sp,
                        color = contentColor.copy(alpha = 0.35f)
                    )
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = if (checked) contentColor.copy(alpha = 0.5f) else contentColor,
                    textDecoration = if (checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                ),
                cursorBrush = SolidColor(contentColor),
                modifier = Modifier.fillMaxWidth()
            )
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_remove_item),
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
