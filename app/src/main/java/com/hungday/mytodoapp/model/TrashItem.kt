package com.hungday.mytodoapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashItem(
    @PrimaryKey(autoGenerate = true) val trashId: Int = 0,
    val originalId: Int,
    val itemType: String, // "FOLDER", "LIST" hoặc "TASK"
    val title: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val folderDataJson: String // Chuỗi JSON chứa dữ liệu backup
)
