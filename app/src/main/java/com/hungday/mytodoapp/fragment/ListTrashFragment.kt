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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.ListTrashAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.TrashItem
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ListTrashFragment : Fragment(R.layout.fragment_list_trash) {

    private lateinit var repository: TodoRepository
    private lateinit var listTrashAdapter: ListTrashAdapter
    private lateinit var btnBack: ImageView
    private lateinit var rvListTrash: RecyclerView

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
        rvListTrash = view.findViewById(R.id.rvListTrash)
    }

    private fun setupAdapter() {
        listTrashAdapter = ListTrashAdapter(emptyList()) { trashItem ->
            showRestoreConfirmDialog(trashItem)
        }
        rvListTrash.layoutManager = LinearLayoutManager(requireContext())
        rvListTrash.adapter = listTrashAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllTrashItems()?.map { items ->
                items.filter { it.itemType == "LIST" }
            }?.collect { listTrashItems ->
                listTrashAdapter.updateData(listTrashItems)
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

        tvTitle.text = "Restore List?"
        tvTitle.setTextColor(resources.getColor(R.color.blue, null))
        tvMessage.text = "Are you sure you want to restore '${trashItem.title}'?"
        
        btnCancel.text = "Cancel"
        btnCancel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(resources.getColor(R.color.red, null)))
        btnConfirm.text = "Restore"
        btnConfirm.setBackgroundTintList(android.content.res.ColorStateList.valueOf(resources.getColor(R.color.blue, null)))

        btnCancel.setOnClickListener { alertDialog.dismiss() }
        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            restoreList(trashItem)
        }

        alertDialog.show()
    }

    private fun restoreList(trashItem: TrashItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.restoreListFromTrash(trashItem)
        }
    }
}