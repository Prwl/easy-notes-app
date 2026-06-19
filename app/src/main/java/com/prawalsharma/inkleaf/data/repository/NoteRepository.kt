package com.prawalsharma.inkleaf.data.repository

import com.prawalsharma.inkleaf.data.db.NoteDao
import com.prawalsharma.inkleaf.data.model.Note
import com.prawalsharma.inkleaf.data.model.SortOrder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val noteDao: NoteDao) {

    fun getActiveNotes(sortOrder: SortOrder = SortOrder.DATE_MODIFIED): Flow<List<Note>> {
        return when (sortOrder) {
            SortOrder.DATE_MODIFIED -> noteDao.getActiveNotes()
            SortOrder.DATE_CREATED -> noteDao.getActiveNotesByCreatedAt()
            SortOrder.TITLE -> noteDao.getActiveNotesByTitle()
            SortOrder.COLOR -> noteDao.getActiveNotesByColor()
        }
    }

    fun getArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    fun getTrashedNotes(): Flow<List<Note>> = noteDao.getTrashedNotes()

    fun getHiddenNotes(): Flow<List<Note>> = noteDao.getHiddenNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun getNotesByFolder(folderId: Long): Flow<List<Note>> = noteDao.getNotesByFolder(folderId)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun saveNote(note: Note): Long {
        return if (note.id == 0L) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
            note.id
        }
    }

    suspend fun moveToTrash(id: Long) = noteDao.moveToTrash(id)

    suspend fun restoreFromTrash(id: Long) = noteDao.restoreFromTrash(id)

    suspend fun deleteNotePermanently(note: Note) = noteDao.deleteNote(note)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    suspend fun archiveNote(id: Long) = noteDao.setArchived(id, true)

    suspend fun unarchiveNote(id: Long) = noteDao.setArchived(id, false)

    suspend fun pinNote(id: Long) = noteDao.setPin(id, true)

    suspend fun unpinNote(id: Long) = noteDao.setPin(id, false)

    suspend fun togglePin(note: Note) {
        if (note.isPinned) noteDao.setPin(note.id, false)
        else noteDao.setPin(note.id, true)
    }

    suspend fun setLocked(id: Long, locked: Boolean) = noteDao.setLocked(id, locked)

    suspend fun hideNote(id: Long) = noteDao.setHidden(id, true)

    suspend fun unhideNote(id: Long) = noteDao.setHidden(id, false)
}
