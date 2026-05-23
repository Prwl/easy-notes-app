package com.prawalsharma.inkleaf.data.repository

import com.prawalsharma.inkleaf.data.db.FolderDao
import com.prawalsharma.inkleaf.data.model.Folder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(private val folderDao: FolderDao) {

    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun getFolderById(id: Long): Folder? = folderDao.getFolderById(id)

    fun getNoteCountForFolder(folderId: Long): Flow<Int> =
        folderDao.getNoteCountForFolder(folderId)

    suspend fun createFolder(name: String, color: Int = 0xFFFF6B35.toInt()): Long =
        folderDao.insertFolder(Folder(name = name.trim(), color = color))

    suspend fun renameFolder(folder: Folder, newName: String) =
        folderDao.updateFolder(folder.copy(name = newName.trim()))

    suspend fun deleteFolder(folder: Folder) {
        folderDao.unassignNotesFromFolder(folder.id)
        folderDao.deleteFolder(folder)
    }
}
