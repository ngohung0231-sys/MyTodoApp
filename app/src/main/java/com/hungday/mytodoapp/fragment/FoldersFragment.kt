package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderGridAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Folder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FoldersFragment : Fragment(R.layout.fragment_folders) {
    // Database & Repository
    private lateinit var repository: TodoRepository
    
    // Adapters
    private lateinit var folderGridAdapter: FolderGridAdapter

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var btnEdit: ImageView
    private lateinit var rvFoldersGrid: RecyclerView
    private lateinit var blank: View
    
    private var isInEditMode = false

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
        repository = TodoRepository(database.todoDao(), database.trashDao())
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnEdit = view.findViewById(R.id.btnEdit)
        rvFoldersGrid = view.findViewById(R.id.rvFoldersGrid)
        blank = view.findViewById(R.id.blank)
    }

    private fun setupAdapters() {
        folderGridAdapter = FolderGridAdapter(emptyList(), {
            // Click "New Folder"
            findNavController().navigate(R.id.action_foldersFragment_to_addFolderFragment)
        }, { folder ->
            val bundle = Bundle().apply {
                putInt("folderId", folder.folderId)
            }
            findNavController().navigate(R.id.action_foldersFragment_to_folderDetailFragment, bundle)
        }, { folder ->
            deleteFolder(folder)
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
                
                rvFoldersGrid.visibility = if (foldersWithCounts.isEmpty()) View.GONE else View.VISIBLE
                if (foldersWithCounts.isEmpty()) {
                    blank.visibility = View.VISIBLE
                    blank.findViewById<TextView>(R.id.tvEmptyText).text = getString(R.string.no_folders_here)
                } else {
                    blank.visibility = View.GONE
                }
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        
        btnEdit.setOnClickListener {
            isInEditMode = !isInEditMode
            folderGridAdapter.setEditMode(isInEditMode)
            if (isInEditMode) {
                btnEdit.setImageResource(R.drawable.ic_done)
            } else {
                btnEdit.setImageResource(R.drawable.ic_setting_task)
            }
        }
    }

    private fun deleteFolder(folder: Folder) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete_folder, null)
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDelete)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirmDelete)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)

        tvMessage.setText(R.string.delete_folder_msg)

        btnCancel.setOnClickListener { alertDialog.dismiss() }

        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            viewLifecycleOwner.lifecycleScope.launch {
                repository.moveFolderToTrash(folder)
            }
        }

        alertDialog.show()
    }
}
