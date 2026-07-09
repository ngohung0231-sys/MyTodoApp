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
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import kotlinx.coroutines.launch

class TrashBinFragment : Fragment(R.layout.fragment_trash_bin) {

    private lateinit var repository: TodoRepository
    private lateinit var btnBack: ImageView
    private lateinit var btnClearAll: ImageView
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
        autoCleanTrash()
    }

    private fun autoCleanTrash() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.clearExpiredTrash()
            repository.autoMoveCompletedTasksToTrash()
        }
    }

    private fun initDatabase() {
        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao(), requireContext())
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnClearAll = view.findViewById(R.id.btnClearAll)
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

                btnClearAll.visibility = if (items.isEmpty()) View.INVISIBLE else View.VISIBLE
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnClearAll.setOnClickListener {
            showClearAllConfirmDialog()
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

    private fun showClearAllConfirmDialog() {
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

        tvTitle.setText(R.string.clear_trash_q)
        tvTitle.setTextColor(resources.getColor(R.color.red, null))
        tvMessage.setText(R.string.clear_trash_msg)

        btnCancel.setText(R.string.cancel)
        btnConfirm.setText(R.string.clear)
        btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.red, null))

        btnCancel.setOnClickListener { alertDialog.dismiss() }
        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            clearAllTrash()
        }

        alertDialog.show()
    }

    private fun clearAllTrash() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.clearAllTrash()
        }
    }
}