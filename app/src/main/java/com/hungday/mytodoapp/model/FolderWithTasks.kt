package com.hungday.mytodoapp.model

data class FolderWithTasks(
    val folder: Folder,
    val taskList: List<Task>
)
