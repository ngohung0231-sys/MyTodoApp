package com.hungday.mytodoapp.database

import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.Task
import com.hungday.mytodoapp.model.TodoList
import kotlinx.coroutines.flow.Flow

class TodoRepository(private var todoDao: TodoDao) {
    val allFolders: Flow<List<Folder>> = todoDao.getAllFolders()
    val allTasks: Flow<List<Task>> = todoDao.getAllTasks()
    val allLists: Flow<List<TodoList>> = todoDao.getAllLists()

    fun getListsByFolder(folderId: Int): Flow<List<TodoList>> {
        return todoDao.getListsByFolder(folderId)
    }

    suspend fun insertFolder(folder: Folder){
        todoDao.insertFolder(folder)
    }

    suspend fun insertTodoList(todoList: TodoList): Long {
        return todoDao.insertTodoList(todoList)
    }

    suspend fun updateTodoList(todoList: TodoList) {
        todoDao.updateTodoList(todoList)
    }

    suspend fun getListById(listId: Int): TodoList? {
        return todoDao.getListById(listId)
    }

    suspend fun insertTask(task: Task): Long {
        return todoDao.insertTask(task)
    }

    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean) {
        todoDao.updateTaskStatus(taskId, isCompleted)
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return todoDao.getTaskById(taskId)
    }

    suspend fun updateTask(task: Task) {
        todoDao.updateTask(task)
    }

    suspend fun deleteCompletedTasks() {
        todoDao.deleteCompletedTasks()
    }

    suspend fun updateFolderColor(folderId: Int, newColor: Int) {
        todoDao.updateFolderColor(folderId, newColor)
    }

}