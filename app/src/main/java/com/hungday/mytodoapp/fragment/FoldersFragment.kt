package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderGridAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FoldersFragment : Fragment(R.layout.fragment_folders) {
    // Database & Repository
    private lateinit var repository: TodoRepository
    
    // Adapters
    private lateinit var folderGridAdapter: FolderGridAdapter

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var rvFoldersGrid: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupAdapters()
        observeData()
        setupListeners()
    }

    private fun initDatabase() {
        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao())
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        rvFoldersGrid = view.findViewById(R.id.rvFoldersGrid)
    }

    private fun setupAdapters() {
        folderGridAdapter = FolderGridAdapter(emptyList(), {
            // Click "New Folder"
            // TODO: Mở Dialog hoặc Fragment thêm folder
        }, { folder ->
            val bundle = Bundle().apply {
                putInt("folderId", folder.folderId)
            }
            findNavController().navigate(R.id.action_foldersFragment_to_folderDetailFragment, bundle)
        })

        rvFoldersGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        rvFoldersGrid.adapter = folderGridAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                repository.allFolders,
                repository.allTasks,
                repository.allLists
            ) { folders, tasks, lists ->
                folders.map { folder ->
                    folder.copy(
                        taskCount = tasks.count { it.folderId == folder.folderId },
                        listCount = lists.count { it.folderId == folder.folderId }
                    )
                }
            }.collect { foldersWithCounts ->
                folderGridAdapter.updateData(foldersWithCounts)
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
