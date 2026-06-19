package com.prawalsharma.inkleaf.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val color: Int = 0xFFFFF9C4.toInt(), // default light yellow
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isHidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val folderId: Long? = null,
    val isLocked: Boolean = false,
    val tags: String = "",         // comma-separated tag names
    val imageUris: String = "",    // comma-separated content URIs for attached images
    val drawingPath: String = ""   // absolute path to saved drawing PNG (empty = none)
)

enum class SortOrder {
    DATE_MODIFIED, DATE_CREATED, TITLE, COLOR
}
