package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderTrashAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.TrashItem
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class FolderTrashFragment : Fragment(R.layout.fragment_folder_trash) {

    private lateinit var repository: TodoRepository
    private lateinit var folderTrashAdapter: FolderTrashAdapter
    private lateinit var btnBack: ImageView
    private lateinit var rvFolderTrash: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupAdapter()
        observeData()
        setupListeners()
    }

    private fun initDatabase() {
        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao())
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        rvFolderTrash = view.findViewById(R.id.rvFolderTrash)
    }

    private fun setupAdapter() {
        folderTrashAdapter = FolderTrashAdapter(emptyList()) { trashItem ->
            showRestoreConfirmDialog(trashItem)
        }
        rvFolderTrash.layoutManager = GridLayoutManager(requireContext(), 2)
        rvFolderTrash.adapter = folderTrashAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllTrashItems()?.map { items ->
                items.filter { it.itemType == "FOLDER" }
            }?.collect { folderTrashItems ->
                folderTrashAdapter.updateData(folderTrashItems)
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showRestoreConfirmDialog(trashItem: TrashItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete_folder, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDelete)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirmDelete)

        tvTitle.text = "Restore Folder?"
        tvTitle.setTextColor(resources.getColor(R.color.blue, null))
        tvMessage.text = "Are you sure you want to restore '${trashItem.title}' and all its tasks?"
        
        btnCancel.text = "Cancel"
        btnCancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(resources.getColor(R.color.red, null)))
        btnConfirm.text = "Restore"
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(resources.getColor(R.color.blue, null)))

        btnCancel.setOnClickListener { alertDialog.dismiss() }
        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            restoreFolder(trashItem)
        }

        alertDialog.show()
    }

    private fun restoreFolder(trashItem: TrashItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.restoreFolderFromTrash(trashItem)
        }
    }
}