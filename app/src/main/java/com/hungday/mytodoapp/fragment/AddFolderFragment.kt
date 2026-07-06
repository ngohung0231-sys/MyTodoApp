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
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddFolderFragment : Fragment(R.layout.fragment_add_folder) {

    private lateinit var repository: TodoRepository
    
    private var folderId: Int = -1
    private var selectedColor = 0xFF4997CF.toInt()
    private var selectedIcon = R.drawable.ic_folder

    private lateinit var btnBack: ImageView
    private lateinit var tvHeaderTitle: TextView
    private lateinit var etFolderName: EditText
    
    private lateinit var rowSelectColor: LinearLayout
    private lateinit var rowSelectIcon: LinearLayout
    
    private lateinit var expandableSelectColor: LinearLayout
    private lateinit var expandableSelectIcon: LinearLayout
    
    private lateinit var chevronColor: ImageView
    private lateinit var chevronIcon: ImageView
    
    private lateinit var viewSelectedColor: View
    private lateinit var imgSelectedIcon: ImageView
    
    private lateinit var rvIcons: RecyclerView
    private lateinit var layoutColorPicker: LinearLayout
    private lateinit var btnAddFolder: Button

    private var isColorExpanded = false
    private var isIconExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao())

        folderId = arguments?.getInt("folderId", -1) ?: -1

        initViews(view)
        setupColorPicker()
        setupIconList()
        setupListeners()

        if (folderId != -1) {
            loadFolderData()
        }
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle)
        etFolderName = view.findViewById(R.id.etFolderName)
        
        rowSelectColor = view.findViewById(R.id.rowSelectColor)
        rowSelectIcon = view.findViewById(R.id.rowSelectIcon)
        
        expandableSelectColor = view.findViewById(R.id.expandableSelectColor)
        expandableSelectIcon = view.findViewById(R.id.expandableSelectIcon)
        
        chevronColor = view.findViewById(R.id.chevronColor)
        chevronIcon = view.findViewById(R.id.chevronIcon)
        
        viewSelectedColor = view.findViewById(R.id.viewSelectedColor)
        imgSelectedIcon = view.findViewById(R.id.imgSelectedIcon)
        imgSelectedIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.black))
        
        rvIcons = view.findViewById(R.id.rvIcons)
        layoutColorPicker = view.findViewById(R.id.layoutColorPicker)
        btnAddFolder = view.findViewById(R.id.btnAddFolder)
    }

    private fun loadFolderData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getFolderById(folderId)?.let { folder ->
                tvHeaderTitle.text = "Edit Folder"
                btnAddFolder.text = "Update Folder"
                etFolderName.setText(folder.folderName)
                selectedColor = folder.folderColor
                selectedIcon = folder.folderImg
                
                viewSelectedColor.backgroundTintList = ColorStateList.valueOf(selectedColor)
                imgSelectedIcon.setImageResource(selectedIcon)
            }
        }
    }

    private fun setupColorPicker() {
        val colorResIds = listOf(
            R.color.blue, R.color.pink, R.color.red, R.color.green,
            R.color.cyan, R.color.purple, R.color.yellow, R.color.amber,
            R.color.orange, R.color.deep_orange,
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
            R.drawable.ic_folder, R.drawable.ic_book, R.drawable.ic_study, 
            R.drawable.ic_shopping, R.drawable.ic_exercise, R.drawable.ic_travel, 
            R.drawable.ic_profile, R.drawable.ic_project, R.drawable.ic_calendar
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
                    isIconExpanded = false
                    toggleExpandableRow(view as ViewGroup, expandableSelectIcon, chevronIcon, isIconExpanded)
                }
            }

            override fun getItemCount() = icons.size
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { findNavController().popBackStack() }

        rowSelectColor.setOnClickListener {
            isColorExpanded = !isColorExpanded
            toggleExpandableRow(view as ViewGroup, expandableSelectColor, chevronColor, isColorExpanded)
        }

        rowSelectIcon.setOnClickListener {
            isIconExpanded = !isIconExpanded
            toggleExpandableRow(view as ViewGroup, expandableSelectIcon, chevronIcon, isIconExpanded)
        }

        btnAddFolder.setOnClickListener {
            val name = etFolderName.text.toString().trim()
            if (name.isEmpty()) {
                etFolderName.error = "Name is required"
                return@setOnClickListener
            }

            val folder = Folder(
                folderId = if (folderId == -1) 0 else folderId,
                folderName = name,
                folderColor = selectedColor,
                folderImg = selectedIcon
            )

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                if (folderId == -1) {
                    repository.insertFolder(folder)
                } else {
                    repository.updateFolder(folder)
                }
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
