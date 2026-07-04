package com.hungday.mytodoapp.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "lists")
data class TodoList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var title: String,
    var color: Int,
    var icon: Int,
    var folderId: Int = 1,
    var content: String = "",
    var subTasks: List<SubTask> = emptyList(),
    var extraContent: String = ""
)
