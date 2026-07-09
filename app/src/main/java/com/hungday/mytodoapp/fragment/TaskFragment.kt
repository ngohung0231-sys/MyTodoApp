package com.hungday.mytodoapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import java.time.LocalTime

class TaskFragment : Fragment(R.layout.fragment_task) {
    // Database & Repository
    private lateinit var database: TodoDatabase
    private lateinit var repository: TodoRepository

    // Adapter
    private lateinit var folderGroupAdapter: FolderGroupAdapter

    // dataList
    private var allTasks = mutableListOf<Task>()
    private var allFolders = mutableListOf<Folder>()

    // Init UI
    private lateinit var btnBack: ImageView
    private lateinit var lnlAddTask: LinearLayout
    private lateinit var rvFolderGroup: RecyclerView
    private lateinit var blank: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupAdapter()
        observeData()
        setupListeners()
    }

    private fun initDatabase() {
        context?.let { ctx ->
            database = TodoDatabase.getDatabase(ctx)
            repository = TodoRepository(database.todoDao(), database.trashDao(), requireContext())
        }
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        lnlAddTask = view.findViewById(R.id.lnlAddTask)
        rvFolderGroup = view.findViewById(R.id.rvFolderGroup)
        blank = view.findViewById(R.id.blank)
    }

    private fun setupAdapter() {
        val ctx = context ?: return
        folderGroupAdapter = FolderGroupAdapter(emptyList() , { folder ->
            if (isAdded) findNavController().navigate(R.id.folderDetailFragment)
        }, {folder ->
            val bundle = Bundle().apply {
                putInt("folderId", folder.folderId)
            }
            if (isAdded) findNavController().navigate(R.id.addFolderFragment, bundle)
        }, { task ->
            val bundle = Bundle().apply {
                putInt("taskId", task.id)
            }
            if (isAdded) findNavController().navigate(R.id.editTaskFragment, bundle)
        }, { task, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                if (::repository.isInitialized) {
                    repository.updateTaskStatus(task.id, isChecked)
                }
            }
        })
        rvFolderGroup.layoutManager = LinearLayoutManager(ctx)
        rvFolderGroup.adapter = folderGroupAdapter
    }

    private fun observeData() {
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
    }

    private fun setupListeners() {
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
    }

    //------------------------------------------ Helper ------------------------------------------//
    private fun refreshTasks() {
        updateTaskDisplay(allTasks)
    }

    private fun updateTaskDisplay(tasks: List<Task>) {
        val ctx = context ?: return
        val groups = getFolderGroups(tasks)
        folderGroupAdapter.updateData(groups)
        rvFolderGroup.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
        
        if (groups.isEmpty()) {
            blank.visibility = View.VISIBLE
            blank.findViewById<ImageView>(R.id.ivEmptyImg).setImageResource(R.drawable.empty_img)
            val tvEmptyText = blank.findViewById<TextView>(R.id.tvEmptyText)
            tvEmptyText.text = ctx.getString(R.string.no_tasks_here)
        } else {
            blank.visibility = View.GONE
        }
    }

    private fun getFolderGroups(tasks: List<Task>): List<FolderWithTasks> {
        return allFolders.mapNotNull { folder ->
            val tasksInFolder = tasks.filter { it.folderId == folder.folderId }
            if (tasksInFolder.isNotEmpty()) {
                val sortedTasks = tasksInFolder.sortedWith(
                    compareBy<Task> {
                        when (it.priority) {
                            "High" -> 1
                            "Medium" -> 2
                            "Low" -> 3
                            else -> 4
                        }
                    }.thenBy { it.date ?: java.time.LocalDate.MAX }
                        .thenBy { it.time ?: LocalTime.MAX }
                )
                FolderWithTasks(folder, sortedTasks)
            } else null
        }
    }
    //--------------------------------------------------------------------------------------------//
}
