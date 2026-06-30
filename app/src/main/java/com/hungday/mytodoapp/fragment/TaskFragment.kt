package com.hungday.mytodoapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderGroupAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.FolderWithTasks
import com.hungday.mytodoapp.model.Task
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskFragment : Fragment(R.layout.fragment_task) {
    // Database & Repository
    private lateinit var database: TodoDatabase
    private lateinit var repository: TodoRepository

    // Adapter
    private lateinit var folderGroupAdapter: FolderGroupAdapter

    // Filter & dataList
    private enum class FilterMode { TODAY, UPCOMING }
    private var currentFilterMode = FilterMode.TODAY
    private var allTasks = mutableListOf<Task>()
    private var allFolders = mutableListOf<Folder>()

    // Init UI
    private lateinit var btnBack: ImageView
    private lateinit var lnlAddTask: LinearLayout
    private lateinit var tvTabToday: TextView
    private lateinit var tvTabUpcoming: TextView
    private lateinit var rvFolderGroup: RecyclerView
    private lateinit var blank: FrameLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //------------------------------------ Init Database ------------------------------------//
        database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao())
        //---------------------------------------------------------------------------------------//

        //------------------------------------ Init Views ------------------------------------//
        btnBack = view.findViewById(R.id.btnBack)
        lnlAddTask = view.findViewById(R.id.lnlAddTask)
        tvTabToday = view.findViewById(R.id.tvTabToday)
        tvTabUpcoming = view.findViewById(R.id.tvTabUpcoming)
        rvFolderGroup = view.findViewById(R.id.rvFolderGroup)
        blank = view.findViewById(R.id.blank)
        //------------------------------------------------------------------------------------//

        //----------------------------------- Setup Adapter -----------------------------------//
        folderGroupAdapter = FolderGroupAdapter(emptyList() , {folder ->
            // TODO: navigate to Folder
        }, { task, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                repository.updateTaskStatus(task.id, isChecked)
            }
        })
        rvFolderGroup.layoutManager = LinearLayoutManager(requireContext())
        rvFolderGroup.adapter = folderGroupAdapter
        //-------------------------------------------------------------------------------------//

        //------------------------------------- Setup data loading -------------------------------------//
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allTasks.collect { taskFromRoom ->
                allTasks = taskFromRoom.toMutableList()
                refreshTasks()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allFolders.collect { foldersFromRoom ->
                allFolders = foldersFromRoom.toMutableList()
                refreshTasks()
            }
        }
        //----------------------------------------------------------------------------------------------//

        //------------------------------------ Setup Listeners ------------------------------------//
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        lnlAddTask.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(findNavController().graph.startDestinationId, false, true)
                .build()
            findNavController().navigate(R.id.addTaskFragment, null, navOptions)
        }

        tvTabToday.setOnClickListener {
            updateTabUI(isToday = true)
            currentFilterMode = FilterMode.TODAY
            refreshTasks()
        }

        tvTabUpcoming.setOnClickListener {
            updateTabUI(isToday = false)
            currentFilterMode = FilterMode.UPCOMING
            refreshTasks()
        }
        //-----------------------------------------------------------------------------------------//
    }

    //------------------------------------------ Helper ------------------------------------------//
    private fun refreshTasks() {
        val today = LocalDate.now()
        val filteredTasks = when (currentFilterMode) {
            FilterMode.TODAY -> allTasks.filter { it.date == today || (it.dateStr.isNullOrEmpty() && it.timeStr.isNullOrEmpty()) }
            FilterMode.UPCOMING -> allTasks.filter { it.date.isAfter(today) || (it.dateStr.isNullOrEmpty() && it.timeStr.isNullOrEmpty()) }
        }
        updateTaskDisplay(filteredTasks)
    }

    private fun updateTaskDisplay(tasks: List<Task>) {
        val groups = getFolderGroups(tasks)
        folderGroupAdapter.updateData(groups)
        rvFolderGroup.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
        blank.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getFolderGroups(tasks: List<Task>): List<FolderWithTasks> {
        return allFolders.mapNotNull { folder ->
            val tasksInFolder = tasks.filter { it.folderId == folder.folderId }
            if (tasksInFolder.isNotEmpty()) FolderWithTasks(folder, tasksInFolder) else null
        }
    }

    private fun updateTabUI(isToday: Boolean) {
        val activeTab = if (isToday) tvTabToday else tvTabUpcoming
        val inactiveTab = if (isToday) tvTabUpcoming else tvTabToday

        activeTab.setTextColor("#4a93ce".toColorInt())
        activeTab.setBackgroundResource(R.drawable.filter_task_bg)

        inactiveTab.setTextColor("#A0A0A0".toColorInt())
        inactiveTab.setBackgroundResource(android.R.color.transparent)
    }
    //--------------------------------------------------------------------------------------------//
}
