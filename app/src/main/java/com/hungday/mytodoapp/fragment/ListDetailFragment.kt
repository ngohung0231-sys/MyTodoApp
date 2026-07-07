package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.SubTaskAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.SubTask
import com.hungday.mytodoapp.model.TodoList
import kotlinx.coroutines.launch

class ListDetailFragment : Fragment(R.layout.fragment_list_detail) {

    private lateinit var repository: TodoRepository
    private var currentList: TodoList? = null
    private var listId: Int = -1

    private lateinit var etListTitle: TextView
    private lateinit var imgListIcon: ImageView
    private lateinit var imgListIconBg: com.google.android.material.imageview.ShapeableImageView
    private lateinit var btnBack: ImageView
    private lateinit var btnSettings: ImageView
    private lateinit var rvTasks: RecyclerView
    private lateinit var btnAddTaskToggle: ImageView
    private lateinit var tvEmptyState: TextView

    private lateinit var taskAdapter: SubTaskAdapter
    private var subTasks = mutableListOf<SubTask>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupAdapter()
        setupListeners()
        loadListData()
    }

    private fun initDatabase() {
        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao())
        listId = arguments?.getInt("listId") ?: -1
    }

    private fun initViews(view: View) {
        etListTitle = view.findViewById(R.id.etListTitle)
        imgListIcon = view.findViewById(R.id.imgListIcon)
        imgListIconBg = view.findViewById(R.id.imgListIconBg)
        btnBack = view.findViewById(R.id.btnBack)
        btnSettings = view.findViewById(R.id.btnSettings)
        rvTasks = view.findViewById(R.id.rvTasks)
        btnAddTaskToggle = view.findViewById(R.id.btnAddTaskToggle)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
    }

    private fun setupAdapter() {
        taskAdapter = SubTaskAdapter(subTasks, {
            saveListToDatabase()
        }, {
            saveListToDatabase()
            updateUIState()
        })

        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = taskAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnSettings.setOnClickListener {
            val bundle = Bundle().apply {
                putInt("listId", listId)
            }
            findNavController().navigate(R.id.action_listDetailFragment_to_addListFragment, bundle)
        }

        btnAddTaskToggle.setOnClickListener {
            taskAdapter.toggleTodoModeForCurrentFocus()
        }

        tvEmptyState.setOnClickListener {
            // Khi nhấn vào màn hình trống, focus vào dòng đầu tiên nếu có
            if (subTasks.isNotEmpty()) {
                rvTasks.scrollToPosition(0)
                rvTasks.post {
                    val holder = rvTasks.findViewHolderForAdapterPosition(0)
                    val editText = holder?.itemView?.findViewById<View>(R.id.etNoteText)
                                ?: holder?.itemView?.findViewById<View>(R.id.etNoteTodoText)
                    editText?.requestFocus()
                }
            }
        }
    }

    private fun loadListData() {
        if (listId != -1) {
            lifecycleScope.launch {
                currentList = repository.getListById(listId)
                currentList?.let { list ->
                    etListTitle.text = list.title
                    imgListIcon.setImageResource(list.icon)
                    imgListIcon.setColorFilter(android.graphics.Color.BLACK)
                    imgListIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(list.color)

                    subTasks = list.subTasks.toMutableList()

                    // Nếu danh sách hoàn toàn trống, bắt đầu bằng 1 dòng text trắng
                    if (subTasks.isEmpty()) {
                        subTasks.add(SubTask(title = "", isTask = false))
                    }

                    taskAdapter.updateData(subTasks)
                    updateUIState()
                }
            }
        }
    }

    private fun saveListToDatabase() {
        val list = currentList ?: return
        list.subTasks = taskAdapter.getSubTasks()
        lifecycleScope.launch {
            repository.updateTodoList(list)
        }
    }

    private fun updateUIState() {
        // Chỉ hiện empty state nếu thực sự không có gì (hiếm khi xảy ra vì ta luôn giữ 1 dòng trống)
        val isEmpty = subTasks.isEmpty()
        
        TransitionManager.beginDelayedTransition(view as ViewGroup, ChangeBounds())
        tvEmptyState.isVisible = isEmpty
        rvTasks.isVisible = !isEmpty
    }
}
