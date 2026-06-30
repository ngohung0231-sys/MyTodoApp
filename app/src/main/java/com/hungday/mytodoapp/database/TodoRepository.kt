package com.hungday.mytodoapp.database

import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.Task
import kotlinx.coroutines.flow.Flow

class TodoRepository(private var todoDao: TodoDao) {
    val allFolders: Flow<List<Folder>> = todoDao.getAllFolders()
    val allTasks: Flow<List<Task>> = todoDao.getAllTasks()

    suspend fun insertFolder(folder: Folder){
        todoDao.insertFolder(folder)
    }
    suspend fun insertTask(task: Task): Long {
        return todoDao.insertTask(task)
    }

    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean) {
        todoDao.updateTaskStatus(taskId, isCompleted)
    }

    suspend fun deleteCompletedTasks() {
        todoDao.deleteCompletedTasks()
    }

    suspend fun updateFolderColor(folderId: Int, newColor: Int) {
        todoDao.updateFolderColor(folderId, newColor)
    }

//    suspend fun updateFolder(folder: Folder) {
//        todoDao.updateFolder(folder)
//    }
//
//    suspend fun updateTask(task: Task) {
//        todoDao.updateTask(task)
//    }
//
//    suspend fun deleteFolder(folder: Folder) {
//        todoDao.deleteFolder(folder)
//    }
//
//    suspend fun deleteTask(task: Task) {
//        todoDao.deleteTask(task)
//    }
}