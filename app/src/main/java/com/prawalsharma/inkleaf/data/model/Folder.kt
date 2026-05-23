package com.prawalsharma.inkleaf.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFFFF6B35.toInt(),
    val createdAt: Long = System.currentTimeMillis()
)
