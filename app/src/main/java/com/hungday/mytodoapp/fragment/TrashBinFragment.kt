package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import kotlinx.coroutines.launch

class TrashBinFragment : Fragment(R.layout.fragment_trash_bin) {

    private lateinit var repository: TodoRepository
    private lateinit var btnBack: ImageView
    private lateinit var tvTrashFoldersCount: TextView
    private lateinit var tvTrashTasksCount: TextView
    private lateinit var tvTrashListsCount: TextView
    private lateinit var cardTrashFolders: View
    private lateinit var cardTrashTasks: View
    private lateinit var cardTrashLists: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        observeTrashData()
        setupListeners()
    }

    private fun initDatabase() {
        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao())
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        tvTrashFoldersCount = view.findViewById(R.id.tvTrashFoldersCount)
        tvTrashTasksCount = view.findViewById(R.id.tvTrashTasksCount)
        tvTrashListsCount = view.findViewById(R.id.tvTrashListsCount)
        cardTrashFolders = view.findViewById(R.id.cardTrashFolders)
        cardTrashTasks = view.findViewById(R.id.cardTrashTasks)
        cardTrashLists = view.findViewById(R.id.cardTrashLists)
    }

    private fun observeTrashData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllTrashItems()?.collect { items ->
                val folderCount = items.count { it.itemType == "FOLDER" }
                val taskCount = items.count { it.itemType == "TASK" }
                val listCount = items.count { it.itemType == "LIST" }

                tvTrashFoldersCount.text = if (folderCount < 2) getString(R.string.item_format, folderCount) else getString(R.string.items_format, folderCount)
                tvTrashTasksCount.text = if (taskCount < 2) getString(R.string.item_format, taskCount) else getString(R.string.items_format, taskCount)
                tvTrashListsCount.text = if (listCount < 2) getString(R.string.item_format, listCount) else getString(R.string.items_format, listCount)
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        cardTrashFolders.setOnClickListener {
            findNavController().navigate(R.id.action_trashBinFragment_to_folderTrashFragment)
        }

        cardTrashTasks.setOnClickListener {
            findNavController().navigate(R.id.action_trashBinFragment_to_taskTrashFragment)
        }

        cardTrashLists.setOnClickListener {
            findNavController().navigate(R.id.action_trashBinFragment_to_listTrashFragment)
        }
    }
}