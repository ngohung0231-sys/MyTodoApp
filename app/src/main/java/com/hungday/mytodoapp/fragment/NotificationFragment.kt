package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.NotificationAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Task
import kotlinx.coroutines.launch

class NotificationFragment : Fragment(R.layout.fragment_task_trash) {
    private lateinit var repository: TodoRepository
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var btnBack: ImageView
    private lateinit var rvNotification: RecyclerView
    private lateinit var blank: View

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
        repository = TodoRepository(database.todoDao(), database.trashDao(), requireContext())
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        rvNotification = view.findViewById(R.id.rvTaskTrash)
        blank = view.findViewById(R.id.blank)
        
        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvTitle)
        tvTitle?.text = getString(R.string.notification)

        view.findViewById<View>(R.id.btnClearAll)?.visibility = View.GONE
    }

    private fun setupAdapter() {
        notificationAdapter = NotificationAdapter(emptyList()) { task ->
            showTurnOffConfirmDialog(task)
        }
        rvNotification.layoutManager = LinearLayoutManager(requireContext())
        rvNotification.adapter = notificationAdapter
    }

    private fun showTurnOffConfirmDialog(task: Task) {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_confirm_delete_folder, null)
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDelete)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirmDelete)

        // Customize text for notification turn off
        tvTitle.text = getString(R.string.off) + " " + getString(R.string.notification) + "?"
        tvMessage.text = "Are you sure you want to turn off notifications and repeat for this task?"

        btnCancel.setOnClickListener { alertDialog.dismiss() }
        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            viewLifecycleOwner.lifecycleScope.launch {
                repository.turnOffTaskNotification(task.id)
            }
        }
        alertDialog.show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getTasksWithNotifications().collect { tasks ->
                notificationAdapter.updateData(tasks)
                if (tasks.isEmpty()) {
                    blank.visibility = View.VISIBLE
                    rvNotification.visibility = View.GONE
                } else {
                    blank.visibility = View.GONE
                    rvNotification.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}