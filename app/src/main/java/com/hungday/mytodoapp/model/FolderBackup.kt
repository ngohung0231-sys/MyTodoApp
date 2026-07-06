package com.hungday.mytodoapp.model

data class FolderBackup(
    val folder: Folder,
    val lists: List<TodoList>,
    val tasks: List<Task>
)
