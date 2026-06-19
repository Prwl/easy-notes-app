package com.prawalsharma.inkleaf.data.db

import androidx.room.*
import com.prawalsharma.inkleaf.data.model.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    /** Move all notes in a deleted folder back to "no folder". */
    @Query("UPDATE notes SET folderId = NULL WHERE folderId = :folderId")
    suspend fun unassignNotesFromFolder(folderId: Long)

    /** Count of non-deleted, non-archived notes in a folder. */
    @Query("SELECT COUNT(*) FROM notes WHERE folderId = :folderId AND isDeleted = 0 AND isArchived = 0 AND isHidden = 0")
    fun getNoteCountForFolder(folderId: Long): Flow<Int>

    /** Full snapshot for backup export. */
    @Query("SELECT * FROM folders ORDER BY name ASC")
    suspend fun getAllFoldersDirect(): List<Folder>

    /** Bulk insert for backup restore. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFolders(folders: List<Folder>)

    /** Wipe all folders (used before restoring from backup). */
    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()
}
