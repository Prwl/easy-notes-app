package com.prawalsharma.inkleaf.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.prawalsharma.inkleaf.MainActivity
import com.prawalsharma.inkleaf.data.db.NoteDatabase
import com.prawalsharma.inkleaf.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val WIDGET_EXTRA_NOTE_ID = "widgetNoteId"

class NotesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load recent 5 notes directly from DB on IO dispatcher (avoid ANR)
        val db = NoteDatabase.getInstance(context)
        val notes = withContext(Dispatchers.IO) {
            db.noteDao().getActiveNotesDirect().take(5)
        }

        provideContent {
            WidgetContent(notes = notes, context = context)
        }
    }
}

private fun openNoteIntent(context: Context, noteId: Long): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        // Unique data per note ensures distinct PendingIntents (otherwise all
        // rows would share the same PendingIntent and the same extras).
        data = Uri.parse("inkleaf://note/$noteId")
        putExtra(WIDGET_EXTRA_NOTE_ID, noteId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

private fun newNoteIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("inkleaf://new")
        putExtra(WIDGET_EXTRA_NOTE_ID, -1L)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
}

@Composable
private fun WidgetContent(notes: List<Note>, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFFFFFDE7)))
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📝 Inkleaf",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ColorProvider(Color(0xFFFF6B35))
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            // New note button → open editor for new note
            Text(
                " + ",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ColorProvider(Color(0xFFFF6B35))
                ),
                modifier = GlanceModifier.clickable(
                    actionStartActivity(newNoteIntent(context))
                )
            )
        }

        if (notes.isEmpty()) {
            Text(
                "No notes yet. Tap + to create one.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = ColorProvider(Color.Gray)
                )
            )
        } else {
            notes.forEach { note ->
                WidgetNoteRow(note = note, context = context)
            }
        }
    }
}

@Composable
private fun WidgetNoteRow(note: Note, context: Context) {
    val bgColor = Color(note.color)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(ColorProvider(bgColor.copy(alpha = 0.7f)))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(actionStartActivity(openNoteIntent(context, note.id))),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (note.title.isNotBlank()) {
                Text(
                    note.title,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ColorProvider(Color.Black)
                    ),
                    maxLines = 1
                )
            }
            if (note.isLocked) {
                Text(
                    "🔒 Locked",
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.DarkGray)),
                    maxLines = 1
                )
            } else if (note.content.isNotBlank()) {
                val preview = com.prawalsharma.inkleaf.utils.RichTextSerializer.toPlainText(note.content)
                    .take(60).replace("\n", " ")
                Text(
                    preview,
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.DarkGray)),
                    maxLines = 1
                )
            }
        }
    }
}

class NotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NotesWidget()
}
