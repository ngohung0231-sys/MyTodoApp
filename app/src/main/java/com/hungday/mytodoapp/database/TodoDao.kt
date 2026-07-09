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

    @Update
    suspend fun updateFolder(folder: Folder)

    @Query("SELECT * FROM folders WHERE folderId = :folderId")
    suspend fun getFolderById(folderId: Int): Folder?

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

    @Query("SELECT * FROM lists WHERE folderId = :folderId")
    suspend fun getListsByFolderSync(folderId: Int): List<TodoList>

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("DELETE FROM tasks WHERE folderId = :folderId")
    suspend fun deleteTasksByFolderId(folderId: Int)

    @Query("DELETE FROM lists WHERE folderId = :folderId")
    suspend fun deleteListsByFolderId(folderId: Int)

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

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<Task>

    @Query("SELECT * FROM tasks WHERE folderId = :folderId")
    suspend fun getTasksByFolderSync(folderId: Int): List<Task>

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean, completedAt: Long?)

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt <= :expiryTime")
    suspend fun getExpiredCompletedTasks(expiryTime: Long): List<Task>

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    @Query("UPDATE folders SET folderColor = :newColor WHERE folderId = :folderId")
    suspend fun updateFolderColor(folderId: Int, newColor: Int)

    @Query("SELECT * FROM tasks WHERE isNotify IS NOT NULL AND isNotify >= 0 AND (isNotify > 0 OR repeatType != 'NONE') AND isCompleted = 0")
    fun getTasksWithNotifications(): Flow<List<Task>>

    @Query("UPDATE tasks SET isNotify = :isNotify, repeatType = :repeatType WHERE id = :taskId")
    suspend fun updateTaskNotification(taskId: Int, isNotify: Int, repeatType: String)
}