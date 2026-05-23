package com.prawalsharma.inkleaf.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

data class ChecklistItem(
    val text: String,
    val checked: Boolean,
    /** Stable in-memory key for Compose. Not persisted. */
    val id: Long = nextChecklistId()
)

private val checklistIdCounter = java.util.concurrent.atomic.AtomicLong(1)
internal fun nextChecklistId(): Long = checklistIdCounter.getAndIncrement()

data class RichTextContent(
    val annotatedString: AnnotatedString,
    val checklistItems: List<ChecklistItem> = emptyList()
)

object RichTextSerializer {

    private const val VERSION_KEY = "version"
    private const val CURRENT_VERSION = 1

    fun encode(annotatedString: AnnotatedString, checklists: List<ChecklistItem>): String {
        val root = JSONObject()
        root.put(VERSION_KEY, CURRENT_VERSION)
        root.put("text", annotatedString.text)

        val spansArray = JSONArray()
        for (range in annotatedString.spanStyles) {
            val span = JSONObject()
            span.put("s", range.start)
            span.put("e", range.end)
            val style = range.item
            if (style.fontWeight == FontWeight.Bold) span.put("bold", true)
            if (style.fontWeight == FontWeight.Normal) span.put("bold", false)
            if (style.fontStyle == FontStyle.Italic) span.put("italic", true)
            if (style.fontStyle == FontStyle.Normal) span.put("italic", false)
            if (style.textDecoration?.contains(TextDecoration.Underline) == true) span.put("underline", true)
            if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) span.put("strike", true)
            if (style.color != Color.Unspecified) span.put("color", style.color.value.toLong())
            if (style.background != Color.Unspecified) span.put("bg", style.background.value.toLong())
            if (style.fontSize.isSp) span.put("size", style.fontSize.value)
            spansArray.put(span)
        }
        root.put("spans", spansArray)

        val parasArray = JSONArray()
        for (range in annotatedString.paragraphStyles) {
            val para = JSONObject()
            para.put("s", range.start)
            para.put("e", range.end)
            para.put("align", when (range.item.textAlign) {
                TextAlign.Center -> "Center"
                TextAlign.End -> "End"
                TextAlign.Justify -> "Justify"
                else -> "Start"
            })
            parasArray.put(para)
        }
        root.put("paragraphs", parasArray)

        val checklistArray = JSONArray()
        for (item in checklists) {
            val ci = JSONObject()
            ci.put("text", item.text)
            ci.put("checked", item.checked)
            checklistArray.put(ci)
        }
        root.put("checklist", checklistArray)

        return root.toString()
    }

    fun decode(content: String): RichTextContent {
        if (!isRichText(content)) return RichTextContent(AnnotatedString(content))
        return try {
            val root = JSONObject(content)
            val text = root.getString("text")
            val builder = AnnotatedString.Builder(text)

            val spansArray = root.optJSONArray("spans") ?: JSONArray()
            for (i in 0 until spansArray.length()) {
                val span = spansArray.getJSONObject(i)
                val start = span.getInt("s")
                val end = span.getInt("e")
                if (start < 0 || end > text.length || start >= end) continue

                val bold = span.optBoolean("bold", false)
                val notBold = span.has("bold") && !span.getBoolean("bold")
                val italic = span.optBoolean("italic", false)
                val notItalic = span.has("italic") && !span.getBoolean("italic")
                val underline = span.optBoolean("underline", false)
                val strike = span.optBoolean("strike", false)

                val decoration = buildDecoration(underline, strike)
                val color = if (span.has("color")) Color(span.getLong("color").toULong()) else Color.Unspecified
                val bg = if (span.has("bg")) Color(span.getLong("bg").toULong()) else Color.Unspecified
                val fontSize = if (span.has("size")) span.getDouble("size").toFloat().sp
                else androidx.compose.ui.unit.TextUnit.Unspecified

                val style = SpanStyle(
                    fontWeight = when {
                        bold -> FontWeight.Bold
                        notBold -> FontWeight.Normal
                        else -> null
                    },
                    fontStyle = when {
                        italic -> FontStyle.Italic
                        notItalic -> FontStyle.Normal
                        else -> null
                    },
                    textDecoration = decoration,
                    color = color,
                    background = bg,
                    fontSize = fontSize
                )
                builder.addStyle(style, start, end)
            }

            val parasArray = root.optJSONArray("paragraphs") ?: JSONArray()
            for (i in 0 until parasArray.length()) {
                val para = parasArray.getJSONObject(i)
                val start = para.getInt("s")
                val end = para.getInt("e")
                if (start < 0 || end > text.length || start >= end) continue
                val align = when (para.optString("align", "Start").uppercase()) {
                    "CENTER" -> TextAlign.Center
                    "END", "RIGHT" -> TextAlign.End
                    "JUSTIFY" -> TextAlign.Justify
                    else -> TextAlign.Start
                }
                builder.addStyle(ParagraphStyle(textAlign = align), start, end)
            }

            val checklistArray = root.optJSONArray("checklist") ?: JSONArray()
            val items = (0 until checklistArray.length()).map { i ->
                val ci = checklistArray.getJSONObject(i)
                ChecklistItem(ci.getString("text"), ci.optBoolean("checked"))
            }

            RichTextContent(builder.toAnnotatedString(), items)
        } catch (e: Exception) {
            RichTextContent(AnnotatedString(content))
        }
    }

    fun isRichText(content: String): Boolean = content.startsWith("{\"$VERSION_KEY\":")

    /** Returns plain text for PDF export / search indexing. */
    fun toPlainText(content: String): String {
        if (!isRichText(content)) return content
        return try {
            val root = org.json.JSONObject(content)
            val text = root.optString("text", "")
            val checklists = root.optJSONArray("checklist")
            val checklistText = if (checklists != null) {
                buildString {
                    for (i in 0 until checklists.length()) {
                        val item = checklists.getJSONObject(i)
                        val checked = item.optBoolean("checked", false)
                        append("\n${if (checked) "☑" else "☐"} ${item.optString("text")}")
                    }
                }
            } else ""
            text + checklistText
        } catch (_: Exception) { content }
    }

    private fun buildDecoration(underline: Boolean, strike: Boolean): TextDecoration? = when {
        underline && strike -> TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
        )
        underline -> TextDecoration.Underline
        strike -> TextDecoration.LineThrough
        else -> null
    }
}
