package com.prawalsharma.inkleaf.data.db

import androidx.room.*
import com.prawalsharma.inkleaf.data.model.Note
import com.prawalsharma.inkleaf.data.model.SortOrder
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun getActiveNotes(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0
        ORDER BY isPinned DESC, createdAt DESC
    """)
    fun getActiveNotesByCreatedAt(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0
        ORDER BY isPinned DESC, title ASC
    """)
    fun getActiveNotesByTitle(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0
        ORDER BY isPinned DESC, color ASC
    """)
    fun getActiveNotesByColor(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 AND isDeleted = 0 AND isHidden = 0 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isHidden = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getHiddenNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND isHidden = 0
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId AND isDeleted = 0 AND isArchived = 0 AND isHidden = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET isDeleted = 1, isHidden = 0, updatedAt = :time WHERE id = :id")
    suspend fun moveToTrash(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, updatedAt = :time WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isArchived = :archived, updatedAt = :time WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, time: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :time WHERE id = :id")
    suspend fun setPin(id: Long, pinned: Boolean, time: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isLocked = :locked WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean)

    @Query("UPDATE notes SET isHidden = :hidden, updatedAt = :time WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean, time: Long = System.currentTimeMillis())

    /** One-shot suspend query for widget (no Flow needed). */
    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 AND isHidden = 0 ORDER BY isPinned DESC, updatedAt DESC LIMIT 5")
    suspend fun getActiveNotesDirect(): List<Note>

    /** Full snapshot for backup export. */
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesDirect(): List<Note>

    /** Bulk insert for backup restore. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<Note>)

    /** Wipe all notes (used before restoring from backup). */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
