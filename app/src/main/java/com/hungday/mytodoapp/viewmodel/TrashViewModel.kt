package com.hungday.mytodoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.TodoList
import com.hungday.mytodoapp.model.TrashItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TrashViewModel(private val repository: TodoRepository) : ViewModel() {

    val trashItems: Flow<List<TrashItem>>? = repository.getAllTrashItems()

    /**
     * Xóa Folder và chuyển vào Thùng rác (đóng gói JSON)
     */
    fun deleteFolderToTrash(folder: Folder) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveFolderToTrash(folder)
        }
    }

    /**
     * Xóa List và chuyển vào Thùng rác
     */
    fun deleteListToTrash(todoList: TodoList) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveListToTrash(todoList)
        }
    }

    /**
     * Xóa vĩnh viễn một mục trong thùng rác
     */
    fun deleteTrashItemPermanently(trashId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrashItem(trashId)
        }
    }

    /**
     * Tự động dọn dẹp các mục trong thùng rác đã quá 15 ngày
     * và chuyển task hoàn thành quá 3 ngày vào thùng rác
     */
    fun autoCleanTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.autoMoveCompletedTasksToTrash()
            repository.clearExpiredTrash()
        }
    }
}
