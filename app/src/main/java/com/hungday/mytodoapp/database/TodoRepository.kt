package com.hungday.mytodoapp.database

import com.google.gson.Gson
import com.hungday.mytodoapp.model.*
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao,
    private val trashDao: TrashDao? = null
) {
    val allFolders: Flow<List<Folder>> = todoDao.getAllFolders()
    val allTasks: Flow<List<Task>> = todoDao.getAllTasks()
    val allLists: Flow<List<TodoList>> = todoDao.getAllLists()

    fun getListsByFolder(folderId: Int): Flow<List<TodoList>> {
        return todoDao.getListsByFolder(folderId)
    }

    suspend fun insertFolder(folder: Folder){
        todoDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
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
