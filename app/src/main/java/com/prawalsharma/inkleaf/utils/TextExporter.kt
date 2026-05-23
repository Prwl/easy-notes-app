package com.prawalsharma.inkleaf.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.prawalsharma.inkleaf.data.model.Note
import java.io.File

object TextExporter {

    /** Returns a plain-text rendering of the note suitable for share/clipboard/export. */
    fun renderNote(note: Note): String = buildString {
        if (note.title.isNotBlank()) {
            appendLine(note.title)
            appendLine("=".repeat(note.title.length.coerceAtMost(60)))
            appendLine()
        }
        append(RichTextSerializer.toPlainText(note.content))
    }

    /** Writes the note as a `.txt` file in cache and returns a sharable [Uri]. */
    fun exportToTxtFile(context: Context, note: Note): Uri {
        val text = renderNote(note)
        val safeName = note.title.ifBlank { "note_${note.id}" }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_")
            .take(40)
        val file = File(context.cacheDir, "${safeName}_${System.currentTimeMillis()}.txt")
        file.writeText(text)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Build a "share as plain text" Intent (no file attachment, just EXTRA_TEXT). */
    fun buildShareTextIntent(note: Note): Intent {
        val body = renderNote(note)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            if (note.title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }

    /** Build an Intent that shares the TXT file via FileProvider (for "Export as TXT"). */
    fun buildShareTxtFileIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
}
