package com.hungday.mytodoapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hungday.mytodoapp.R

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val folderId: Int = 0,
    var folderName: String,
    val folderImg: Int = R.drawable.ic_folder,
    val folderColor: Int = R.color.blue,
    var taskCount: Int = 0,
    var listCount: Int = 0
)