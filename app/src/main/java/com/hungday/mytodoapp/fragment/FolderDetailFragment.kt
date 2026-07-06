package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderDetailAdapter
import com.hungday.mytodoapp.adapter.FolderGroupAdapter
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.FolderWithTasks
import com.hungday.mytodoapp.model.SubTask
import com.hungday.mytodoapp.model.TodoList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FolderDetailFragment : Fragment(R.layout.fragment_folder_detail) {

    private lateinit var repository: TodoRepository
    private lateinit var folderDetailAdapter: FolderDetailAdapter
    private lateinit var folderGroupAdapter: FolderGroupAdapter

    private lateinit var btnBack: ImageView
    private lateinit var btnSetting: ImageView
    private lateinit var lnlAddTask: LinearLayout
    private lateinit var rvList: RecyclerView
    private lateinit var rvFolderGroup: RecyclerView
    private lateinit var listBlank: FrameLayout
    private lateinit var taskBlank: FrameLayout
    private lateinit var tvFolderTitle: TextView

    private var folderId: Int = 1 // Default

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupAdapters()
        setupListeners()
        observeData()
    }

    private fun initDatabase() {
        val database = com.hungday.mytodoapp.database.TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao())
        folderId = arguments?.getInt("folderId") ?: 1
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnSetting = view.findViewById(R.id.btnNotification)
        lnlAddTask = view.findViewById(R.id.lnlAddTask)
        rvList = view.findViewById(R.id.rvList)
        rvFolderGroup = view.findViewById(R.id.rvFolderGroup)
        listBlank = view.findViewById(R.id.listBlank)
        taskBlank = view.findViewById(R.id.taskBlank)
        tvFolderTitle = view.findViewById(R.id.tvFolderTitle)
    }

    private fun setupAdapters() {
        // RV for TodoLists (Image 1)
        folderDetailAdapter = FolderDetailAdapter(emptyList()) { todoList ->
            val bundle = Bundle().apply {
                putInt("listId", todoList.id)
            }
            findNavController().navigate(R.id.action_folderDetailFragment_to_listDetailFragment, bundle)
        }
        rvList.layoutManager = LinearLayoutManager(requireContext())
        rvList.adapter = folderDetailAdapter

        // RV for Tasks (FolderGroupAdapter used in Home)
        folderGroupAdapter = FolderGroupAdapter(emptyList(), { folder ->
            // Handle folder setting click
        }, { task ->
            val bundle = Bundle().apply {
                putInt("taskId", task.id)
            }
            findNavController().navigate(R.id.editTaskFragment, bundle)
        }, { task, isCompleted ->
            lifecycleScope.launch {
                repository.updateTaskStatus(task.id, isCompleted)
            }
        })
        rvFolderGroup.layoutManager = LinearLayoutManager(requireContext())
        rvFolderGroup.adapter = folderGroupAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSetting.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("folderId", folderId)
            }
            findNavController().navigate(R.id.action_folderDetailFragment_to_addFolderFragment, bundle)
        }

        lnlAddTask.setOnClickListener {
            // Navigate to Add List Fragment
            val bundle = Bundle().apply {
                putInt("folderId", folderId)
            }
            findNavController().navigate(R.id.action_folderDetailFragment_to_addListFragment, bundle)
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            // Quan sát cả Folders, TodoLists và Tasks để cập nhật UI
            combine(
                repository.allFolders,
                repository.getListsByFolder(folderId),
                repository.allTasks
            ) { folders, lists, tasks ->
                Triple(folders, lists, tasks)
            }.collect { (folders, lists, tasks) ->
                // Lọc Folder hiện tại
                val currentFolder = folders.find { it.folderId == folderId }
                
                folderDetailAdapter.updateData(lists)
                
                // Cập nhật Task group bên dưới
                val folderTasks = tasks.filter { it.folderId == folderId }
                if (currentFolder != null) {
                    tvFolderTitle.text = currentFolder.folderName
                    val folderGroup = FolderWithTasks(currentFolder, folderTasks)
                    folderGroupAdapter.updateData(listOf(folderGroup))
                }

                rvList.isVisible = lists.isNotEmpty()
                listBlank.isVisible = lists.isEmpty()
                rvFolderGroup.isVisible = folderTasks.isNotEmpty()
                taskBlank.isVisible = folderTasks.isEmpty()
            }
        }
    }
}
