package com.hungday.mytodoapp.database

import androidx.room.*
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.Task
import com.hungday.mytodoapp.model.TodoList
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM lists")
    fun getAllLists(): Flow<List<TodoList>>

    @Query("SELECT * FROM lists WHERE id = :listId")
    suspend fun getListById(listId: Int): TodoList?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoList(todoList: TodoList): Long

    @Update
    suspend fun updateTodoList(todoList: TodoList)

    @Delete
    suspend fun deleteTodoList(todoList: TodoList)

    @Query("SELECT * FROM lists WHERE folderId = :folderId")
    fun getListsByFolder(folderId: Int): Flow<List<TodoList>>

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE folderId = :folderId")
    fun getTasksByFolder(folderId: Int): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :selectedDate")
    fun getTasksByDate(selectedDate: String): Flow<List<Task>>

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    @Query("UPDATE folders SET folderColor = :newColor WHERE folderId = :folderId")
    suspend fun updateFolderColor(folderId: Int, newColor: Int)
}