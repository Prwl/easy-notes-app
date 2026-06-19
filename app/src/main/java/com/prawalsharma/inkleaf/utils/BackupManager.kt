package com.prawalsharma.inkleaf.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.prawalsharma.inkleaf.data.db.FolderDao
import com.prawalsharma.inkleaf.data.db.NoteDao
import com.prawalsharma.inkleaf.data.model.Folder
import com.prawalsharma.inkleaf.data.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val folderDao: FolderDao
) {

    /**
     * Serializes all notes and folders to a JSON file in internal storage
     * and returns a sharable [Uri] via FileProvider.
     */
    suspend fun exportBackup(): Uri {
        val notes = noteDao.getAllNotesDirect()
        val folders = folderDao.getAllFoldersDirect()

        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("folders", JSONArray().also { arr -> folders.forEach { arr.put(it.toJson()) } })
            put("notes", JSONArray().also { arr -> notes.forEach { arr.put(it.toJson()) } })
        }

        val backupsDir = File(context.filesDir, "backups").also { it.mkdirs() }
        val file = File(backupsDir, "inkleaf_backup_${System.currentTimeMillis()}.json")
        file.writeText(root.toString(2))

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Reads a backup file from [uri], clears existing data, and inserts the backup contents.
     * Returns the number of notes restored.
     */
    suspend fun importBackup(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            ?: throw IOException("Cannot read backup file")

        val root = JSONObject(text)
        val foldersArray = root.optJSONArray("folders") ?: JSONArray()
        val notesArray = root.optJSONArray("notes") ?: JSONArray()

        val folders = (0 until foldersArray.length()).map { foldersArray.getJSONObject(it).toFolder() }
        val notes = (0 until notesArray.length()).map { notesArray.getJSONObject(it).toNote() }

        // Full restore: wipe then re-insert
        noteDao.deleteAllNotes()
        folderDao.deleteAllFolders()
        if (folders.isNotEmpty()) folderDao.insertAllFolders(folders)
        if (notes.isNotEmpty()) noteDao.insertAllNotes(notes)

        return notes.size
    }

    // ---------- Serialization helpers ----------

    private fun Folder.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("color", color)
        put("createdAt", createdAt)
    }

    private fun Note.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("content", content)
        put("color", color)
        put("isPinned", isPinned)
        put("isArchived", isArchived)
        put("isDeleted", isDeleted)
        put("isHidden", isHidden)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("folderId", folderId ?: JSONObject.NULL)
        put("isLocked", isLocked)
        put("tags", tags)
        put("imageUris", imageUris)
        put("drawingPath", drawingPath)
    }

    private fun JSONObject.toFolder() = Folder(
        id = getLong("id"),
        name = getString("name"),
        color = getInt("color"),
        createdAt = getLong("createdAt")
    )

    private fun JSONObject.toNote() = Note(
        id = getLong("id"),
        title = optString("title", ""),
        content = optString("content", ""),
        color = getInt("color"),
        isPinned = optBoolean("isPinned", false),
        isArchived = optBoolean("isArchived", false),
        isDeleted = optBoolean("isDeleted", false),
        isHidden = optBoolean("isHidden", false),
        createdAt = getLong("createdAt"),
        updatedAt = getLong("updatedAt"),
        folderId = if (isNull("folderId")) null else getLong("folderId"),
        isLocked = optBoolean("isLocked", false),
        tags = optString("tags", ""),
        imageUris = optString("imageUris", ""),
        drawingPath = optString("drawingPath", "")
    )
}
