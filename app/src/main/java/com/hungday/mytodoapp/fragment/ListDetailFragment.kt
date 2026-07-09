package com.hungday.mytodoapp.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
    private lateinit var lnlEmptyState: LinearLayout
    
    private lateinit var layoutFormatting: LinearLayout
    private lateinit var btnTextColor: View
    private lateinit var viewCurrentColor: View
    private lateinit var btnBold: TextView
    private lateinit var btnItalic: TextView
    private lateinit var btnUnderline: TextView

    private lateinit var taskAdapter: SubTaskAdapter
    private var subTasks = mutableListOf<SubTask>()
    private var activeEditText: EditText? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupAdapter()
        setupListeners()
        loadListData()
    }

    private fun initDatabase() {
        context?.let { ctx ->
            val database = TodoDatabase.getDatabase(ctx)
            repository = TodoRepository(database.todoDao(), database.trashDao())
            listId = arguments?.getInt("listId") ?: -1
        }
    }

    private fun initViews(view: View) {
        etListTitle = view.findViewById(R.id.etListTitle)
        imgListIcon = view.findViewById(R.id.imgListIcon)
        imgListIconBg = view.findViewById(R.id.imgListIconBg)
        btnBack = view.findViewById(R.id.btnBack)
        btnSettings = view.findViewById(R.id.btnSettings)
        rvTasks = view.findViewById(R.id.rvTasks)
        btnAddTaskToggle = view.findViewById(R.id.btnAddTaskToggle)
        lnlEmptyState = view.findViewById(R.id.lnlEmptyState)
        
        layoutFormatting = view.findViewById(R.id.layoutFormatting)
        btnTextColor = view.findViewById(R.id.btnTextColor)
        viewCurrentColor = view.findViewById(R.id.viewCurrentColor)
        btnBold = view.findViewById(R.id.btnBold)
        btnItalic = view.findViewById(R.id.btnItalic)
        btnUnderline = view.findViewById(R.id.btnUnderline)
    }

    private fun setupAdapter() {
        val ctx = context ?: return
        taskAdapter = SubTaskAdapter(subTasks, {
            saveListToDatabase()
        }, {
            saveListToDatabase()
            updateUIState()
        }, { editText, _ ->
            activeEditText = editText
            layoutFormatting.visibility = if (editText != null) View.VISIBLE else View.INVISIBLE
            editText?.let { updateFormattingButtonsState(it) }
        }, { editText ->
            activeEditText = editText
            layoutFormatting.visibility = View.VISIBLE
            updateFormattingButtonsState(editText)
        })

        rvTasks.layoutManager = LinearLayoutManager(ctx)
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
            updateUIState()
        }

        lnlEmptyState.setOnClickListener {
            subTasks.add(SubTask(title = "", isTask = false))
            taskAdapter.updateData(subTasks)
            updateUIState()
            
            rvTasks.post {
                val holder = rvTasks.findViewHolderForAdapterPosition(0)
                holder?.itemView?.findViewById<View>(R.id.etNoteText)?.requestFocus()
            }
        }

        // Formatting Buttons
        btnBold.setOnClickListener { taskAdapter.toggleBold() }
        btnItalic.setOnClickListener { taskAdapter.toggleItalic() }
        btnUnderline.setOnClickListener { taskAdapter.toggleUnderline() }
        btnTextColor.setOnClickListener { showColorPopup() }
    }

    private fun showColorPopup() {
        val start = activeEditText?.selectionStart ?: 0
        val text = activeEditText?.text as? Spanned
        val spans = text?.getSpans(start, start, ForegroundColorSpan::class.java)
        val currentColor = spans?.lastOrNull()?.foregroundColor ?: android.graphics.Color.BLACK

        val bottomSheet = TextColorBottomSheetFragment(currentColor) { selectedColor ->
            taskAdapter.toggleTextColor(selectedColor)
        }
        bottomSheet.show(childFragmentManager, "TextColorBottomSheet")
    }

    private fun updateFormattingButtonsState(editText: EditText) {
        val start = editText.selectionStart
        val end = editText.selectionEnd
        val text = editText.text as? Spannable ?: return
        
        val targetEnd = if (start == end) start else end
        
        // 1. StyleSpan (Bold/Italic)
        val styleSpans = text.getSpans(start, targetEnd, StyleSpan::class.java)
        var isBold = false
        var isItalic = false
        for (span in styleSpans) {
            if (span.style == Typeface.BOLD) isBold = true
            if (span.style == Typeface.ITALIC) isItalic = true
        }
        
        // 2. UnderlineSpan
        val underlineSpans = text.getSpans(start, targetEnd, UnderlineSpan::class.java)
        var isUnderline = false
        for (span in underlineSpans) {
            val flag = text.getSpanFlags(span)
            if ((flag and android.text.Spanned.SPAN_COMPOSING) == 0) {
                isUnderline = true
                break
            }
        }
        
        // 3. Color
        val colorSpans = text.getSpans(start, targetEnd, ForegroundColorSpan::class.java)
        val currentColor = colorSpans.lastOrNull()?.foregroundColor ?: android.graphics.Color.BLACK
        
        val drawable = viewCurrentColor.background.mutate()
        if (drawable is android.graphics.drawable.GradientDrawable) {
            drawable.setColor(currentColor)
            // Stroke logic for light colors
            if (currentColor == android.graphics.Color.WHITE || currentColor == android.graphics.Color.parseColor("#F3F5F9")) {
                drawable.setStroke(2, android.graphics.Color.LTGRAY)
            } else {
                drawable.setStroke(0, android.graphics.Color.TRANSPARENT)
            }
        } else {
            // If it's not a GradientDrawable, set a simple oval background
            val newDrawable = android.graphics.drawable.GradientDrawable()
            newDrawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            newDrawable.setColor(currentColor)
            viewCurrentColor.background = newDrawable
        }
        
        btnBold.isSelected = isBold
        btnItalic.isSelected = isItalic
        btnUnderline.isSelected = isUnderline
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
        val isEmpty = taskAdapter.getSubTasks().isEmpty()
        
        TransitionManager.beginDelayedTransition(view as ViewGroup, ChangeBounds())
        lnlEmptyState.isVisible = isEmpty
        rvTasks.isVisible = !isEmpty
    }
}
