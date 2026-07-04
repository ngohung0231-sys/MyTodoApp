package com.hungday.mytodoapp.adapter

import android.graphics.Color
import android.graphics.Paint
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.SubTask

class SubTaskAdapter(
    private var subTasks: MutableList<SubTask>,
    private val onSubTaskChanged: (SubTask) -> Unit,
    private val onDataUpdated: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_TODO = 1
    }

    private var currentFocusedPosition: Int = -1
    private var pendingFocusPosition: Int = -1
    private var pendingSelectionStart: Int = -1

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etNoteText: EditText = itemView.findViewById(R.id.etNoteText)
    }

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbNoteTodo: CheckBox = itemView.findViewById(R.id.cbNoteTodo)
        val etNoteTodoText: EditText = itemView.findViewById(R.id.etNoteTodoText)
    }

    override fun getItemViewType(position: Int): Int {
        return if (subTasks[position].isTask) TYPE_TODO else TYPE_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TODO) {
            val view = inflater.inflate(R.layout.item_note_todo, parent, false)
            TodoViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_note_text, parent, false)
            TextViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val subTask = subTasks[position]

        if (holder is TodoViewHolder) {
            setupTodoView(holder, subTask, position)
        } else if (holder is TextViewHolder) {
            setupTextView(holder, subTask, position)
        }

        // Focus handling
        val editText = if (holder is TodoViewHolder) holder.etNoteTodoText else (holder as TextViewHolder).etNoteText
        if (position == pendingFocusPosition) {
            editText.requestFocus()
            if (pendingSelectionStart != -1) {
                val safeSelection = pendingSelectionStart.coerceIn(0, editText.text.length)
                editText.setSelection(safeSelection)
                pendingSelectionStart = -1
            } else {
                editText.setSelection(editText.text.length)
            }
            pendingFocusPosition = -1
        }
    }

    private fun setupTextView(holder: TextViewHolder, subTask: SubTask, position: Int) {
        val editText = holder.etNoteText

        // Remove old watcher
        (editText.tag as? TextWatcher)?.let { editText.removeTextChangedListener(it) }

        editText.setText(subTask.title)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                subTask.title = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {
                onSubTaskChanged(subTask)
            }
        }
        editText.addTextChangedListener(textWatcher)
        editText.tag = textWatcher

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) currentFocusedPosition = holder.bindingAdapterPosition
        }

        setupKeyboardActions(editText, subTask, position)
    }

    private fun setupTodoView(holder: TodoViewHolder, subTask: SubTask, position: Int) {
        val editText = holder.etNoteTodoText
        val checkBox = holder.cbNoteTodo

        // Remove old watcher
        (editText.tag as? TextWatcher)?.let { editText.removeTextChangedListener(it) }

        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = subTask.isCompleted
        editText.setText(subTask.title)
        updateStrikeThrough(editText, subTask.isCompleted)

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                subTask.title = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {
                onSubTaskChanged(subTask)
            }
        }
        editText.addTextChangedListener(textWatcher)
        editText.tag = textWatcher

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            subTask.isCompleted = isChecked
            updateStrikeThrough(editText, isChecked)
            onSubTaskChanged(subTask)
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) currentFocusedPosition = holder.bindingAdapterPosition
        }

        setupKeyboardActions(editText, subTask, position)
    }

    private fun setupKeyboardActions(editText: EditText, subTask: SubTask, position: Int) {
        // Smart Enter
        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                
                val currentPos = currentFocusedPosition
                if (currentPos != -1) {
                    val selectionStart = editText.selectionStart
                    val fullText = subTask.title
                    
                    val prefix = fullText.substring(0, selectionStart)
                    val suffix = fullText.substring(selectionStart)
                    
                    subTask.title = prefix
                    notifyItemChanged(currentPos)
                    
                    val newLine = SubTask(
                        title = suffix,
                        isTask = subTask.isTask,
                        isCompleted = false
                    )
                    subTasks.add(currentPos + 1, newLine)
                    pendingFocusPosition = currentPos + 1
                    pendingSelectionStart = 0
                    notifyItemInserted(currentPos + 1)
                    onDataUpdated()
                }
                true
            } else false
        }

        // Smart Backspace
        editText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                val currentPos = currentFocusedPosition
                val selectionStart = editText.selectionStart
                
                if (selectionStart == 0 && currentPos != -1) {
                    if (subTask.isTask && subTask.title.isEmpty()) {
                        // Case 1: Empty Task -> Text
                        subTask.isTask = false
                        notifyItemChanged(currentPos)
                        onDataUpdated()
                        return@setOnKeyListener true
                    } else if (currentPos > 0) {
                        // Case 2: Merge into above
                        val aboveItem = subTasks[currentPos - 1]
                        pendingSelectionStart = aboveItem.title.length
                        aboveItem.title += subTask.title
                        
                        subTasks.removeAt(currentPos)
                        pendingFocusPosition = currentPos - 1
                        notifyItemRemoved(currentPos)
                        notifyItemChanged(currentPos - 1)
                        onDataUpdated()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun updateStrikeThrough(editText: EditText, isCompleted: Boolean) {
        if (isCompleted) {
            editText.paintFlags = editText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            editText.setTextColor(Color.GRAY)
            editText.alpha = 0.6f
        } else {
            editText.paintFlags = editText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            editText.setTextColor(Color.BLACK)
            editText.alpha = 1.0f
        }
    }

    override fun getItemCount() = subTasks.size

    fun toggleTodoModeForCurrentFocus() {
        if (currentFocusedPosition != -1) {
            val subTask = subTasks[currentFocusedPosition]
            subTask.isTask = !subTask.isTask
            pendingFocusPosition = currentFocusedPosition
            notifyItemChanged(currentFocusedPosition)
            onDataUpdated()
        }
    }

    fun updateData(newList: List<SubTask>) {
        this.subTasks = newList.toMutableList()
        notifyDataSetChanged()
    }
}
