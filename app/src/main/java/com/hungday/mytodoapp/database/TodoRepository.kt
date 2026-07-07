package com.hungday.mytodoapp.database

import com.google.gson.Gson
import com.hungday.mytodoapp.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao,
    private val trashDao: TrashDao? = null
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allFolders: Flow<List<Folder>> = todoDao.getAllFolders()
    val allTasks: Flow<List<Task>> = todoDao.getAllTasks()
    val allLists: Flow<List<TodoList>> = todoDao.getAllLists()

    fun getListsByFolder(folderId: Int): Flow<List<TodoList>> {
        return todoDao.getListsByFolder(folderId)
    }

    suspend fun insertFolder(folder: Folder){
        todoDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        todoDao.updateFolder(folder)
    }

    suspend fun getFolderById(folderId: Int): Folder? {
        return todoDao.getFolderById(folderId)
    }

    suspend fun deleteFolder(folder: Folder) {
        todoDao.deleteFolder(folder)
    }

    suspend fun deleteFolderWithTasks(folder: Folder) {
        todoDao.deleteTasksByFolderId(folder.folderId)
        todoDao.deleteListsByFolderId(folder.folderId)
        todoDao.deleteFolder(folder)
    }

    // --- Trash Logic ---

    suspend fun moveFolderToTrash(folder: Folder) {
        trashDao?.let { tDao ->
            // Bước 1: Query trọn gói thông tin Folder cùng toàn bộ các List và Task thuộc về Folder đó
            val lists = todoDao.getListsByFolderSync(folder.folderId)
            val tasks = todoDao.getTasksByFolderSync(folder.folderId)

            // Bước 2: Sử dụng thư viện Gson để biến toàn bộ cục Object phân cấp thành chuỗi String JSON
            val backup = FolderBackup(folder, lists, tasks)
            val folderDataJson = Gson().toJson(backup)

            // Bước 3: Lưu chuỗi JSON đó vào bảng TrashItem kèm theo timestamp hiện tại
            val trashItem = TrashItem(
                originalId = folder.folderId,
                itemType = "FOLDER",
                title = folder.folderName,
                folderDataJson = folderDataJson
            )
            tDao.insertTrashItem(trashItem)

            // Bước 4: Gọi lệnh Delete Folder ở bảng chính (CASCADE sẽ tự xóa List và Task con)
            todoDao.deleteFolder(folder)
        }
    }

    suspend fun restoreFolderFromTrash(trashItem: TrashItem) {
        trashDao?.let { tDao ->
            val backup = Gson().fromJson(trashItem.folderDataJson, FolderBackup::class.java)
            
            // 1. Insert Folder lại
            todoDao.insertFolder(backup.folder)
            
            // 2. Insert Lists lại
            backup.lists.forEach { list ->
                todoDao.insertTodoList(list)
            }
            
            // 3. Insert Tasks lại
            backup.tasks.forEach { task ->
                todoDao.insertTask(task)
            }
            
            // 4. Xóa khỏi thùng rác
            tDao.deleteTrashItem(trashItem.trashId)
        }
    }

    suspend fun moveTaskToTrash(task: Task) {
        trashDao?.let { tDao ->
            val taskDataJson = Gson().toJson(task)
            val trashItem = TrashItem(
                originalId = task.id,
                itemType = "TASK",
                title = task.title,
                folderDataJson = taskDataJson
            )
            tDao.insertTrashItem(trashItem)
            todoDao.deleteTask(task)
        }
    }

    suspend fun restoreTaskFromTrash(trashItem: TrashItem) {
        trashDao?.let { tDao ->
            val task = Gson().fromJson(trashItem.folderDataJson, Task::class.java)
            todoDao.insertTask(task)
            tDao.deleteTrashItem(trashItem.trashId)
        }
    }

    suspend fun moveListToTrash(todoList: TodoList) {
        trashDao?.let { tDao ->
            val listDataJson = Gson().toJson(todoList)
            val trashItem = TrashItem(
                originalId = todoList.id,
                itemType = "LIST",
                title = todoList.title,
                folderDataJson = listDataJson
            )
            tDao.insertTrashItem(trashItem)
            todoDao.deleteTodoList(todoList)
        }
    }

    suspend fun restoreListFromTrash(trashItem: TrashItem) {
        trashDao?.let { tDao ->
            val list = Gson().fromJson(trashItem.folderDataJson, TodoList::class.java)
            todoDao.insertTodoList(list)
            tDao.deleteTrashItem(trashItem.trashId)
        }
    }

    /**
     * Tự động chuyển các task đã hoàn thành quá 3 ngày vào thùng rác
     */
    suspend fun autoMoveCompletedTasksToTrash() {
        trashDao?.let { tDao ->
            val threeDaysInMillis = 3 * 24 * 60 * 60 * 1000L
            val expiryTime = System.currentTimeMillis() - threeDaysInMillis
            val expiredTasks = todoDao.getExpiredCompletedTasks(expiryTime)

            expiredTasks.forEach { task ->
                val taskDataJson = Gson().toJson(task)
                val trashItem = TrashItem(
                    originalId = task.id,
                    itemType = "TASK",
                    title = task.title,
                    folderDataJson = taskDataJson
                )
                tDao.insertTrashItem(trashItem)
                todoDao.deleteTask(task)
            }
        }
    }

    fun getAllTrashItems(): Flow<List<TrashItem>>? {
        return trashDao?.getAllTrashItems()
    }

    suspend fun deleteTrashItem(trashId: Int) {
        trashDao?.deleteTrashItem(trashId)
    }

    suspend fun clearExpiredTrash() {
        // Xóa các bản ghi vượt quá 15 ngày
        val fifteenDaysInMillis = 15 * 24 * 60 * 60 * 1000L
        val expiryTime = System.currentTimeMillis() - fifteenDaysInMillis
        trashDao?.deleteExpiredTrash(expiryTime)
    }

    // --- Other Methods ---

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
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        todoDao.updateTaskStatus(taskId, isCompleted, completedAt)

        if (isCompleted) {
            repositoryScope.launch {
                delay(3000)
                val task = todoDao.getTaskById(taskId)
                if (task != null && task.isCompleted) {
                    moveTaskToTrash(task)
                }
            }
        }
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
