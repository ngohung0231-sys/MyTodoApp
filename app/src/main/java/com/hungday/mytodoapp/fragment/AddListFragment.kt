package com.hungday.mytodoapp.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderAddTaskAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.TodoList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddListFragment : Fragment(R.layout.fragment_add_list) {

    private lateinit var repository: TodoRepository
    private lateinit var folderAdapter: FolderAddTaskAdapter
    
    private var selectedFolderId = 1
    private var selectedColor = 0xFF4997CF.toInt()
    private var selectedIcon = R.drawable.ic_book

    private lateinit var btnBack: ImageView
    private lateinit var etListTitle: EditText
    
    private lateinit var rowSelectFolder: LinearLayout
    private lateinit var rowSelectColor: LinearLayout
    private lateinit var rowSelectIcon: LinearLayout
    
    private lateinit var expandableSelectFolder: LinearLayout
    private lateinit var expandableSelectColor: LinearLayout
    private lateinit var expandableSelectIcon: LinearLayout
    
    private lateinit var chevronFolder: ImageView
    private lateinit var chevronColor: ImageView
    private lateinit var chevronIcon: ImageView
    
    private lateinit var tvSelectedFolder: TextView
    private lateinit var viewSelectedColor: View
    private lateinit var imgSelectedIcon: ImageView
    
    private lateinit var rvFolders: RecyclerView
    private lateinit var rvIcons: RecyclerView
    private lateinit var layoutColorPicker: LinearLayout
    private lateinit var btnAddList: Button

    private var isFolderExpanded = false
    private var isColorExpanded = false
    private var isIconExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao())

        selectedFolderId = arguments?.getInt("folderId") ?: 1

        initViews(view)
        setupFolderList()
        setupColorPicker()
        setupIconList()
        setupListeners()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        etListTitle = view.findViewById(R.id.etListTitle)
        
        rowSelectFolder = view.findViewById(R.id.rowSelectFolder)
        rowSelectColor = view.findViewById(R.id.rowSelectColor)
        rowSelectIcon = view.findViewById(R.id.rowSelectIcon)
        
        expandableSelectFolder = view.findViewById(R.id.expandableSelectFolder)
        expandableSelectColor = view.findViewById(R.id.expandableSelectColor)
        expandableSelectIcon = view.findViewById(R.id.expandableSelectIcon)
        
        chevronFolder = view.findViewById(R.id.chevronFolder)
        chevronColor = view.findViewById(R.id.chevronColor)
        chevronIcon = view.findViewById(R.id.chevronIcon)
        
        tvSelectedFolder = view.findViewById(R.id.tvSelectedFolder)
        viewSelectedColor = view.findViewById(R.id.viewSelectedColor)
        imgSelectedIcon = view.findViewById(R.id.imgSelectedIcon)
        imgSelectedIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black))
        
        rvFolders = view.findViewById(R.id.rvFolders)
        rvIcons = view.findViewById(R.id.rvIcons)
        layoutColorPicker = view.findViewById(R.id.layoutColorPicker)
        btnAddList = view.findViewById(R.id.btnAddList)
    }

    private fun setupFolderList() {
        rvFolders.layoutManager = LinearLayoutManager(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allFolders.collect { folders ->
                // Set initial folder name
                folders.find { it.folderId == selectedFolderId }?.let {
                    tvSelectedFolder.text = it.folderName
                }

                folderAdapter = FolderAddTaskAdapter(folders) { folder ->
                    selectedFolderId = folder.folderId
                    tvSelectedFolder.text = folder.folderName
                    isFolderExpanded = false
                    toggleExpandableRow(view as ViewGroup, expandableSelectFolder, chevronFolder, isFolderExpanded)
                }
                rvFolders.adapter = folderAdapter
            }
        }
    }

    private fun setupColorPicker() {
        val colorResIds = listOf(
            R.color.blue,
            R.color.pink,
            R.color.red,
            R.color.green,
            R.color.cyan,
            R.color.purple,
            R.color.yellow,
            R.color.amber,
            R.color.orange,
            R.color.deep_orange,
        )

        colorResIds.forEach { resId ->
            val color = ContextCompat.getColor(requireContext(), resId)
            val colorView = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    setMargins(16, 0, 16, 0)
                }
                background = ContextCompat.getDrawable(requireContext(), R.drawable.ic_circle_filled)
                backgroundTintList = ColorStateList.valueOf(color)
                setOnClickListener {
                    selectedColor = color
                    viewSelectedColor.backgroundTintList = ColorStateList.valueOf(color)
                    isColorExpanded = false
                    toggleExpandableRow(view as ViewGroup, expandableSelectColor, chevronColor, isColorExpanded)
                }
            }
            layoutColorPicker.addView(colorView)
        }
    }

    private fun setupIconList() {
        val icons = listOf(
            R.drawable.ic_book, R.drawable.ic_study, R.drawable.ic_shopping,
            R.drawable.ic_exercise, R.drawable.ic_travel, R.drawable.ic_profile,
            R.drawable.ic_project, R.drawable.ic_pin, R.drawable.ic_calendar
        )

        rvIcons.layoutManager = GridLayoutManager(requireContext(), 4)
        rvIcons.adapter = object : RecyclerView.Adapter<IconViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_icon_picker, parent, false)
                return IconViewHolder(view)
            }

            override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
                val iconRes = icons[position]
                val imageView = holder.itemView.findViewById<ImageView>(R.id.imgIcon)
                imageView.setImageResource(iconRes)
                imageView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black))
                imageView.setOnClickListener {
                    selectedIcon = iconRes
                    imgSelectedIcon.setImageResource(iconRes)
                    imgSelectedIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black))
                    isIconExpanded = false
                    toggleExpandableRow(view as ViewGroup, expandableSelectIcon, chevronIcon, isIconExpanded)
                }
            }

            override fun getItemCount() = icons.size
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { findNavController().popBackStack() }

        rowSelectFolder.setOnClickListener {
            isFolderExpanded = !isFolderExpanded
            toggleExpandableRow(view as ViewGroup, expandableSelectFolder, chevronFolder, isFolderExpanded)
        }

        rowSelectColor.setOnClickListener {
            isColorExpanded = !isColorExpanded
            toggleExpandableRow(view as ViewGroup, expandableSelectColor, chevronColor, isColorExpanded)
        }

        rowSelectIcon.setOnClickListener {
            isIconExpanded = !isIconExpanded
            toggleExpandableRow(view as ViewGroup, expandableSelectIcon, chevronIcon, isIconExpanded)
        }

        btnAddList.setOnClickListener {
            val title = etListTitle.text.toString().trim()
            if (title.isEmpty()) {
                etListTitle.error = "Title is required"
                return@setOnClickListener
            }

            val newList = TodoList(
                title = title,
                color = selectedColor,
                icon = selectedIcon,
                folderId = selectedFolderId
            )

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                repository.insertTodoList(newList)
                withContext(Dispatchers.Main) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun toggleExpandableRow(rootView: ViewGroup, expandableLayout: View, chevron: ImageView, isExpanded: Boolean) {
        val transition = TransitionSet().addTransition(ChangeBounds()).setDuration(300)
        TransitionManager.beginDelayedTransition(rootView, transition)
        expandableLayout.isVisible = isExpanded
        chevron.animate().rotation(if (isExpanded) 180f else 0f).setDuration(250).start()
    }

    class IconViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
