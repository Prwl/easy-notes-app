package com.prawalsharma.inkleaf.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.prawalsharma.inkleaf.data.model.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private const val PAGE_WIDTH = 595   // A4 width in points (72dpi)
    private const val PAGE_HEIGHT = 842  // A4 height in points
    private const val MARGIN = 56f
    private const val LINE_HEIGHT = 22f
    private const val TITLE_SIZE = 22f
    private const val BODY_SIZE = 14f
    private const val DATE_SIZE = 10f

    /** Exports note as PDF into the cache dir; returns a sharable FileProvider [Uri]. */
    fun exportNote(context: Context, note: Note): Uri {
        val file = File(context.cacheDir, "note_${note.id}_${System.currentTimeMillis()}.pdf")
        writePdf(note, file)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Saves the PDF to the public Downloads/Inkleaf/ folder.
     * - API 29+: uses MediaStore (no permission needed).
     * - API 26–28: writes directly to /Downloads/Inkleaf/ (legacy storage).
     * Returns the destination [Uri] (content:// on Q+, file:// fallback) or null on failure.
     */
    fun saveToDownloads(context: Context, note: Note): Uri? {
        val fileName = "${note.title.ifBlank { "note_${note.id}" }
            .replace(Regex("[^A-Za-z0-9_.-]"), "_").take(40)}_${System.currentTimeMillis()}.pdf"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Inkleaf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: return null

            // Render PDF directly into the stream
            val doc = renderDocument(note)
            try {
                val out = resolver.openOutputStream(uri)
                if (out == null) {
                    // Roll back the pending insert so we don't leak a 0-byte placeholder
                    resolver.delete(uri, null, null)
                    return null
                }
                out.use { doc.writeTo(it) }
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            } finally {
                doc.close()
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "Inkleaf"
            ).also { it.mkdirs() }
            val file = File(downloadsDir, fileName)
            writePdf(note, file)
            Uri.fromFile(file)
        }
    }

    private fun writePdf(note: Note, file: File) {
        val document = renderDocument(note)
        try {
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private fun renderDocument(note: Note): PdfDocument {
        val document = PdfDocument()
        var pageNumber = 1
        var page = startPage(document, pageNumber)
        var canvas = page.canvas
        var y = MARGIN

        // Date
        val datePaint = Paint().apply {
            textSize = DATE_SIZE
            color = android.graphics.Color.GRAY
        }
        val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            .format(Date(note.updatedAt))
        canvas.drawText(dateStr, MARGIN, y, datePaint)
        y += LINE_HEIGHT * 1.5f

        // Title
        if (note.title.isNotBlank()) {
            val titlePaint = Paint().apply {
                textSize = TITLE_SIZE
                color = android.graphics.Color.BLACK
                isFakeBoldText = true
            }
            y = drawWrappedText(canvas, note.title, MARGIN, y, PAGE_WIDTH - MARGIN * 2, titlePaint, LINE_HEIGHT * 1.4f)
            y += LINE_HEIGHT
        }

        // Divider
        val linePaint = Paint().apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += LINE_HEIGHT

        // Body — strip rich text JSON to plain text
        val bodyText = RichTextSerializer.toPlainText(note.content)
            .ifBlank { "(empty note)" }
        val bodyPaint = Paint().apply {
            textSize = BODY_SIZE
            color = android.graphics.Color.BLACK
        }

        for (line in bodyText.split("\n")) {
            y = drawWrappedText(canvas, line.ifBlank { " " }, MARGIN, y,
                PAGE_WIDTH - MARGIN * 2, bodyPaint, LINE_HEIGHT)
            if (y > PAGE_HEIGHT - MARGIN * 2) {
                document.finishPage(page)
                pageNumber++
                page = startPage(document, pageNumber)
                canvas = page.canvas
                y = MARGIN
            }
        }
        document.finishPage(page)
        return document
    }

    private fun startPage(doc: PdfDocument, num: Int): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, num).create()
        return doc.startPage(info)
    }

    private fun drawWrappedText(
        canvas: Canvas, text: String, x: Float, startY: Float,
        maxWidth: Float, paint: Paint, lineH: Float
    ): Float {
        var y = startY
        val words = text.split(" ")
        var currentLine = ""
        for (word in words) {
            // A single word that itself exceeds maxWidth would overflow the margin
            // — hard-break it into chunks before normal greedy wrapping.
            val chunks = if (paint.measureText(word) > maxWidth) hardBreak(word, maxWidth, paint)
                         else listOf(word)
            for (chunk in chunks) {
                val test = if (currentLine.isEmpty()) chunk else "$currentLine $chunk"
                if (paint.measureText(test) <= maxWidth) {
                    currentLine = test
                } else {
                    if (currentLine.isNotEmpty()) {
                        canvas.drawText(currentLine, x, y, paint)
                        y += lineH
                    }
                    currentLine = chunk
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, y, paint)
            y += lineH
        }
        return y
    }

    /** Splits a too-long word into substrings that each fit within [maxWidth]. */
    private fun hardBreak(word: String, maxWidth: Float, paint: Paint): List<String> {
        val out = mutableListOf<String>()
        var current = StringBuilder()
        for (ch in word) {
            current.append(ch)
            if (paint.measureText(current.toString()) > maxWidth) {
                current.deleteCharAt(current.length - 1)
                if (current.isNotEmpty()) out += current.toString()
                current = StringBuilder().append(ch)
            }
        }
        if (current.isNotEmpty()) out += current.toString()
        return out
    }
}
