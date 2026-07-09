package com.hungday.mytodoapp.database

import com.google.gson.Gson
import com.hungday.mytodoapp.model.*
import com.hungday.mytodoapp.widget.TodoWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

class TodoRepository(
    private val todoDao: TodoDao,
    private val trashDao: TrashDao? = null,
    private val context: Context? = null
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun notifyWidgetDataChanged() {
        context?.let { ctx ->
            val appWidgetManager = AppWidgetManager.getInstance(ctx)
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(ctx, TodoWidgetProvider::class.java))
            appWidgetManager.notifyAppWidgetViewDataChanged(ids, com.hungday.mytodoapp.R.id.lv_tasks)
            
            // Also trigger a full update to refresh the header state if needed, 
            // though notifyAppWidgetViewDataChanged is enough for the list.
            val intent = android.content.Intent(ctx, TodoWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            ctx.sendBroadcast(intent)
        }
    }

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
            
            // Cancel alarm when moved to trash
            context?.let { ctx ->
                com.hungday.mytodoapp.receiver.NotificationReceiver().cancelTaskAlarm(ctx, task.id)
            }
        }
    }

    suspend fun restoreTaskFromTrash(trashItem: TrashItem): Boolean {
        return trashDao?.let { tDao ->
            val task = Gson().fromJson(trashItem.folderDataJson, Task::class.java)
            
            // Kiểm tra xem folder chứa task còn tồn tại không
            val folder = todoDao.getFolderById(task.folderId)
            if (folder == null && task.folderId != 1) { // 1 thường là folder "Others" mặc định
                return false
            }

            todoDao.insertTask(task)
            tDao.deleteTrashItem(trashItem.trashId)
            true
        } ?: false
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

    suspend fun restoreListFromTrash(trashItem: TrashItem): Boolean {
        return trashDao?.let { tDao ->
            val list = Gson().fromJson(trashItem.folderDataJson, TodoList::class.java)
            
            // Kiểm tra xem folder chứa list còn tồn tại không
            val folder = todoDao.getFolderById(list.folderId)
            if (folder == null && list.folderId != 1) {
                return false
            }

            todoDao.insertTodoList(list)
            tDao.deleteTrashItem(trashItem.trashId)
            true
        } ?: false
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

    suspend fun clearAllTrash() {
        trashDao?.deleteAllTrash()
    }

    suspend fun clearTrashByType(type: String) {
        trashDao?.deleteAllTrashByType(type)
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
        val result = todoDao.insertTask(task)
        notifyWidgetDataChanged()
        return result
    }

    suspend fun updateTaskStatus(taskId: Int, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        todoDao.updateTaskStatus(taskId, isCompleted, completedAt)
        notifyWidgetDataChanged()

        if (isCompleted) {
            repositoryScope.launch {
                delay(3000)
                val task = todoDao.getTaskById(taskId)
                if (task != null && task.isCompleted) {
                    if (task.repeatType != "NONE") {
                        // Reschedule recurring task instead of moving to trash
                        val nextDate = when (task.repeatType) {
                            "DAILY" -> task.date?.plusDays(1)
                            "WEEKLY" -> task.date?.plusWeeks(1)
                            "MONTHLY" -> task.date?.plusMonths(1)
                            "YEARLY" -> task.date?.plusYears(1)
                            else -> task.date?.plusDays(1)
                        }
                        val updatedTask = task.copy(
                            date = nextDate,
                            isCompleted = false,
                            completedAt = null
                        )
                        todoDao.updateTask(updatedTask)
                    } else {
                        moveTaskToTrash(task)
                    }
                    notifyWidgetDataChanged()
                }
            }
        }
    }

    suspend fun getTaskById(taskId: Int): Task? {
        return todoDao.getTaskById(taskId)
    }

    suspend fun updateTask(task: Task) {
        todoDao.updateTask(task)
        notifyWidgetDataChanged()
    }

    suspend fun deleteTask(task: Task) {
        todoDao.deleteTask(task)
        notifyWidgetDataChanged()
    }

    suspend fun deleteCompletedTasks() {
        todoDao.deleteCompletedTasks()
    }

    suspend fun updateFolderColor(folderId: Int, newColor: Int) {
        todoDao.updateFolderColor(folderId, newColor)
    }

    fun getTasksWithNotifications(): Flow<List<Task>> {
        return todoDao.getTasksWithNotifications()
    }

    suspend fun turnOffTaskNotification(taskId: Int) {
        todoDao.updateTaskNotification(taskId, 0, "NONE")
        context?.let { ctx ->
            com.hungday.mytodoapp.receiver.NotificationReceiver().cancelTaskAlarm(ctx, taskId)
        }
        notifyWidgetDataChanged()
    }
}
