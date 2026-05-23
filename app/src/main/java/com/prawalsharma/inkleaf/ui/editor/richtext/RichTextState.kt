package com.prawalsharma.inkleaf.ui.editor.richtext

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.prawalsharma.inkleaf.utils.ChecklistItem

@Stable
class RichTextState {

    var textFieldValue by mutableStateOf(TextFieldValue(AnnotatedString("")))
        private set

    // Active formatting for newly typed characters
    var isBold by mutableStateOf(false)
    var isItalic by mutableStateOf(false)
    var isUnderline by mutableStateOf(false)
    var isStrikethrough by mutableStateOf(false)
    var fontSize by mutableStateOf(16f)
    var textColor by mutableStateOf(Color.Unspecified)
    var highlightColor by mutableStateOf(Color.Unspecified)
    var textAlign by mutableStateOf(TextAlign.Start)

    var checklistItems by mutableStateOf<List<ChecklistItem>>(emptyList())

    // Character-level parallel arrays
    private val charStyles = mutableListOf<SpanStyle>()
    private val charAligns = mutableListOf<TextAlign>()

    private data class Snapshot(
        val styles: List<SpanStyle>,
        val aligns: List<TextAlign>,
        val text: String,
        val selection: TextRange
    )

    private val undoStack = ArrayDeque<Snapshot>(50)
    private val redoStack = ArrayDeque<Snapshot>(50)

    private val maxHistory = 50

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()
    val hasSelection get() = !textFieldValue.selection.collapsed

    // ─── Text change handler ───────────────────────────────────────────────

    fun onValueChange(newValue: TextFieldValue) {
        val oldText = textFieldValue.annotatedString.text
        val newText = newValue.annotatedString.text

        if (oldText == newText) {
            textFieldValue = newValue
            syncActiveStyleFromCursor(newValue)
            return
        }

        pushUndo()
        redoStack.clear()

        val (prefixLen, deletedCount, insertedCount) = findDiff(oldText, newText)

        repeat(deletedCount) {
            if (prefixLen < charStyles.size) {
                charStyles.removeAt(prefixLen)
                charAligns.removeAt(prefixLen)
            }
        }

        val activeStyle = buildActiveSpanStyle()
        repeat(insertedCount) { i ->
            charStyles.add(prefixLen + i, activeStyle)
            charAligns.add(prefixLen + i, textAlign)
        }

        // Ensure arrays stay in sync with text length
        while (charStyles.size > newText.length) charStyles.removeAt(charStyles.size - 1)
        while (charStyles.size < newText.length) {
            charStyles.add(SpanStyle())
            charAligns.add(TextAlign.Start)
        }

        textFieldValue = newValue.copy(annotatedString = buildAnnotatedString(newText))
    }

    // ─── Format toggles ────────────────────────────────────────────────────

    fun toggleBold() {
        val wasActive = isSelectionFullyStyled { it.fontWeight == FontWeight.Bold }
        isBold = !wasActive
        applyToSelection { it.copy(fontWeight = if (!wasActive) FontWeight.Bold else FontWeight.Normal) }
    }

    fun toggleItalic() {
        val wasActive = isSelectionFullyStyled { it.fontStyle == FontStyle.Italic }
        isItalic = !wasActive
        applyToSelection { it.copy(fontStyle = if (!wasActive) FontStyle.Italic else FontStyle.Normal) }
    }

    fun toggleUnderline() {
        val wasActive = isSelectionFullyStyled {
            it.textDecoration?.contains(TextDecoration.Underline) == true
        }
        isUnderline = !wasActive
        applyToSelection { style ->
            val hasStrike = style.textDecoration?.contains(TextDecoration.LineThrough) == true
            style.copy(textDecoration = buildDecoration(!wasActive, hasStrike))
        }
    }

    fun toggleStrikethrough() {
        val wasActive = isSelectionFullyStyled {
            it.textDecoration?.contains(TextDecoration.LineThrough) == true
        }
        isStrikethrough = !wasActive
        applyToSelection { style ->
            val hasUnderline = style.textDecoration?.contains(TextDecoration.Underline) == true
            style.copy(textDecoration = buildDecoration(hasUnderline, !wasActive))
        }
    }

    fun applyFontSize(size: Float) {
        fontSize = size
        applyToSelection { it.copy(fontSize = size.sp) }
    }

    fun applyTextColor(color: Color) {
        textColor = color
        applyToSelection { it.copy(color = color) }
    }

    fun resetTextColor() {
        textColor = Color.Unspecified
        applyToSelection { it.copy(color = Color.Unspecified) }
    }

    fun applyHighlightColor(color: Color) {
        highlightColor = color
        applyToSelection { it.copy(background = color) }
    }

    fun resetHighlightColor() {
        highlightColor = Color.Unspecified
        applyToSelection { it.copy(background = Color.Unspecified) }
    }

    fun applyAlignment(align: TextAlign) {
        textAlign = align
        if (charAligns.isEmpty()) return
        val text = textFieldValue.annotatedString.text
        val sel = textFieldValue.selection
        // Find paragraph bounds for current cursor/selection
        val selStart = minOf(sel.start, sel.end).coerceIn(0, text.length)
        val selEnd = maxOf(sel.start, sel.end).coerceIn(0, text.length)

        val paraStart = if (selStart > 0) {
            val idx = text.lastIndexOf('\n', selStart - 1)
            if (idx == -1) 0 else idx + 1
        } else 0

        val paraEnd = run {
            val idx = text.indexOf('\n', selEnd)
            if (idx == -1) text.length else idx + 1
        }

        pushUndo()
        redoStack.clear()
        for (i in paraStart until paraEnd.coerceAtMost(charAligns.size)) {
            charAligns[i] = align
        }
        rebuildAnnotatedString()
    }

    // ─── Undo / Redo ──────────────────────────────────────────────────────

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addFirst(captureSnapshot())
        if (redoStack.size > maxHistory) redoStack.removeLast()
        restoreSnapshot(undoStack.removeFirst())
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addFirst(captureSnapshot())
        if (undoStack.size > maxHistory) undoStack.removeLast()
        restoreSnapshot(redoStack.removeFirst())
    }

    // ─── Load / Export ────────────────────────────────────────────────────

    fun loadContent(annotatedString: AnnotatedString, items: List<ChecklistItem>) {
        charStyles.clear()
        charAligns.clear()
        val text = annotatedString.text

        val flatStyles = Array(text.length) { SpanStyle() }
        for (range in annotatedString.spanStyles) {
            for (i in range.start until range.end.coerceAtMost(text.length)) {
                flatStyles[i] = flatStyles[i].merge(range.item)
            }
        }
        charStyles.addAll(flatStyles)

        val flatAligns = Array(text.length) { TextAlign.Start }
        for (range in annotatedString.paragraphStyles) {
            for (i in range.start until range.end.coerceAtMost(text.length)) {
                flatAligns[i] = range.item.textAlign ?: TextAlign.Start
            }
        }
        charAligns.addAll(flatAligns)

        textFieldValue = TextFieldValue(buildAnnotatedString(text))
        checklistItems = items
        undoStack.clear()
        redoStack.clear()
    }

    fun loadPlainText(text: String) {
        charStyles.clear()
        charAligns.clear()
        repeat(text.length) { charStyles.add(SpanStyle()); charAligns.add(TextAlign.Start) }
        textFieldValue = TextFieldValue(AnnotatedString(text))
        undoStack.clear()
        redoStack.clear()
    }

    val annotatedString: AnnotatedString get() = textFieldValue.annotatedString
    val plainText: String get() = textFieldValue.annotatedString.text

    // ─── Checklist ────────────────────────────────────────────────────────

    fun addChecklistItem() {
        checklistItems = checklistItems + ChecklistItem("", false)
    }

    fun updateChecklistItem(index: Int, text: String? = null, checked: Boolean? = null) {
        checklistItems = checklistItems.mapIndexed { i, item ->
            if (i == index) item.copy(
                text = text ?: item.text,
                checked = checked ?: item.checked
            )
            else item
        }
    }

    fun removeChecklistItem(index: Int) {
        checklistItems = checklistItems.filterIndexed { i, _ -> i != index }
    }

    // ─── Private helpers ──────────────────────────────────────────────────

    private fun applyToSelection(transform: (SpanStyle) -> SpanStyle) {
        val sel = textFieldValue.selection
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)
        if (start == end) return

        pushUndo()
        redoStack.clear()
        for (i in start until end.coerceAtMost(charStyles.size)) {
            charStyles[i] = transform(charStyles[i])
        }
        rebuildAnnotatedString()
    }

    private fun isSelectionFullyStyled(predicate: (SpanStyle) -> Boolean): Boolean {
        val sel = textFieldValue.selection
        val start = minOf(sel.start, sel.end)
        val end = maxOf(sel.start, sel.end)
        if (start == end || charStyles.isEmpty()) return false
        return (start until end.coerceAtMost(charStyles.size)).all { predicate(charStyles[it]) }
    }

    private fun rebuildAnnotatedString() {
        val text = textFieldValue.annotatedString.text
        textFieldValue = textFieldValue.copy(annotatedString = buildAnnotatedString(text))
    }

    private fun buildAnnotatedString(text: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val n = minOf(text.length, charStyles.size, charAligns.size)
        if (n == 0) return AnnotatedString(text)

        val builder = AnnotatedString.Builder(text)

        // Run-length encode span styles
        var spanStart = 0
        var cur = charStyles[0]
        for (i in 1..n) {
            val next = if (i < n) charStyles[i] else null
            if (next != cur || i == n) {
                if (cur != SpanStyle()) builder.addStyle(cur, spanStart, i)
                spanStart = i
                if (next != null) cur = next
            }
        }

        // Run-length encode paragraph aligns (per line)
        var paraStart = 0
        var curAlign = charAligns[0]
        for (i in 1..n) {
            val next = if (i < n) charAligns[i] else null
            val atNewline = i > 0 && text.getOrNull(i - 1) == '\n'
            if (next != curAlign || i == n || atNewline) {
                if (curAlign != TextAlign.Start) {
                    builder.addStyle(ParagraphStyle(textAlign = curAlign), paraStart, i)
                }
                paraStart = i
                if (next != null) curAlign = next
            }
        }

        return builder.toAnnotatedString()
    }

    private fun syncActiveStyleFromCursor(value: TextFieldValue) {
        val pos = (value.selection.start - 1).coerceAtLeast(0)
        if (pos < charStyles.size) {
            val s = charStyles[pos]
            isBold = s.fontWeight == FontWeight.Bold
            isItalic = s.fontStyle == FontStyle.Italic
            isUnderline = s.textDecoration?.contains(TextDecoration.Underline) == true
            isStrikethrough = s.textDecoration?.contains(TextDecoration.LineThrough) == true
            if (s.fontSize.isSp) fontSize = s.fontSize.value
            textColor = s.color
            highlightColor = s.background
        }
        if (pos < charAligns.size) textAlign = charAligns[pos]
    }

    private fun buildActiveSpanStyle(): SpanStyle = SpanStyle(
        fontWeight = if (isBold) FontWeight.Bold else null,
        fontStyle = if (isItalic) FontStyle.Italic else null,
        textDecoration = buildDecoration(isUnderline, isStrikethrough),
        color = textColor,
        background = highlightColor,
        fontSize = if (fontSize != 16f) fontSize.sp else TextUnit.Unspecified
    )

    private fun buildDecoration(underline: Boolean, strike: Boolean): TextDecoration? = when {
        underline && strike -> TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
        )
        underline -> TextDecoration.Underline
        strike -> TextDecoration.LineThrough
        else -> null
    }

    private fun captureSnapshot() = Snapshot(
        charStyles.toList(), charAligns.toList(),
        textFieldValue.annotatedString.text, textFieldValue.selection
    )

    private fun restoreSnapshot(snap: Snapshot) {
        charStyles.clear(); charStyles.addAll(snap.styles)
        charAligns.clear(); charAligns.addAll(snap.aligns)
        textFieldValue = TextFieldValue(
            annotatedString = buildAnnotatedString(snap.text),
            selection = snap.selection
        )
    }

    private fun pushUndo() {
        undoStack.addFirst(captureSnapshot())
        if (undoStack.size > maxHistory) undoStack.removeLast()
    }

    companion object {
        private fun findDiff(old: String, new: String): Triple<Int, Int, Int> {
            var prefix = 0
            while (prefix < old.length && prefix < new.length && old[prefix] == new[prefix]) prefix++
            var oldEnd = old.length
            var newEnd = new.length
            while (oldEnd > prefix && newEnd > prefix && old[oldEnd - 1] == new[newEnd - 1]) {
                oldEnd--; newEnd--
            }
            return Triple(prefix, oldEnd - prefix, newEnd - prefix)
        }
    }
}

@Composable
fun rememberRichTextState(): RichTextState = remember { RichTextState() }
