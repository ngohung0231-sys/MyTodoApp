package com.hungday.mytodoapp.adapter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.util.TypedValue
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
import com.hungday.mytodoapp.view.RichEditText
import kotlin.math.roundToInt

class SubTaskAdapter(
    private var subTasks: MutableList<SubTask>,
    private val onSubTaskChanged: (SubTask) -> Unit,
    private val onDataUpdated: () -> Unit,
    private val onFocusChanged: (EditText?, Int) -> Unit,
    private val onSelectionChanged: (EditText) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_TODO = 1
    }

    private var currentFocusedPosition: Int = -1
    private var activeEditText: EditText? = null
    private var pendingFocusPosition: Int = -1
    private var pendingSelectionStart: Int = -1
    private var isLayoutChanging: Boolean = false

    class TextViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val etNoteText: RichEditText = itemView.findViewById(R.id.etNoteText)
    }

    class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbNoteTodo: CheckBox = itemView.findViewById(R.id.cbNoteTodo)
        val etNoteTodoText: RichEditText = itemView.findViewById(R.id.etNoteTodoText)
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

        val editText = if (holder is TodoViewHolder) holder.etNoteTodoText else (holder as TextViewHolder).etNoteText
        
        // Remove old watcher to prevent infinite loop or wrong data updates
        (editText.tag as? TextWatcher)?.let { editText.removeTextChangedListener(it) }

        // Load HTML content
        val content: Spannable = if (subTask.title.contains("&") || (subTask.title.contains("<") && subTask.title.contains(">"))) {
            Html.fromHtml(subTask.title, Html.FROM_HTML_MODE_COMPACT) as Spannable
        } else {
            SpannableStringBuilder(subTask.title)
        }
        
        // Apply Strikethrough if completed, without losing original spans
        if (subTask.isCompleted) {
            content.setSpan(StrikethroughSpan(), 0, content.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        editText.isStrikingThrough = subTask.isCompleted
        editText.setTextWithoutTriggeringSelection(content)

        if (holder is TodoViewHolder) {
            setupTodoView(holder, subTask, position)
        } else {
            setupTextView(holder as TextViewHolder, subTask, position)
        }

        // Focus handling
        if (position == pendingFocusPosition) {
            editText.requestFocus()
            if (pendingSelectionStart != -1) {
                val safeSelection = pendingSelectionStart.coerceIn(0, editText.text?.length ?: 0)
                editText.setSelection(safeSelection)
                pendingSelectionStart = -1
            } else {
                editText.setSelection(editText.text?.length ?: 0)
            }
            pendingFocusPosition = -1
        }
    }

    private fun setupTextView(holder: TextViewHolder, subTask: SubTask, position: Int) {
        val editText = holder.etNoteText
        addTextWatcher(editText, subTask)
        
        editText.selectionChangedListener = object : RichEditText.OnSelectionChangedListener {
            override fun onSelectionChanged(selStart: Int, selEnd: Int) {
                if (editText.isFocused) onSelectionChanged(editText)
            }
        }
        
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentFocusedPosition = holder.bindingAdapterPosition
                activeEditText = editText
                onFocusChanged(editText, currentFocusedPosition)
                onSelectionChanged(editText)
            } else {
                if (!isLayoutChanging && activeEditText == editText) {
                    activeEditText = null
                    currentFocusedPosition = -1
                    onFocusChanged(null, -1)
                }
            }
        }

        setupKeyboardActions(editText, subTask, holder)
    }

    private fun setupTodoView(holder: TodoViewHolder, subTask: SubTask, position: Int) {
        val editText = holder.etNoteTodoText
        val checkBox = holder.cbNoteTodo

        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = subTask.isCompleted
        editText.isStrikingThrough = subTask.isCompleted

        addTextWatcher(editText, subTask)

        editText.selectionChangedListener = object : RichEditText.OnSelectionChangedListener {
            override fun onSelectionChanged(selStart: Int, selEnd: Int) {
                if (editText.isFocused) onSelectionChanged(editText)
            }
        }

        checkBox.setOnClickListener {
            if (isLayoutChanging) return@setOnClickListener
            val pos = holder.bindingAdapterPosition
            if (pos != -1) {
                toggleTaskCompletion(pos)
            }
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentFocusedPosition = holder.bindingAdapterPosition
                activeEditText = editText
                onFocusChanged(editText, currentFocusedPosition)
                onSelectionChanged(editText)
            } else {
                if (!isLayoutChanging && activeEditText == editText) {
                    activeEditText = null
                    currentFocusedPosition = -1
                    onFocusChanged(null, -1)
                }
            }
        }

        setupKeyboardActions(editText, subTask, holder)
    }

    private fun addTextWatcher(editText: EditText, subTask: SubTask) {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s == null || isLayoutChanging || !editText.hasFocus()) return
                
                val currentText = s.toString()

                if (currentText == "\u200B" || currentText.isEmpty()) {
                    subTask.title = ""
                    onSubTaskChanged(subTask)
                    return
                }

                if (currentText.length > 1 && currentText.startsWith("\u200B")) {
                    editText.removeTextChangedListener(this)
                    s.delete(0, 1) // Remove anchor
                    editText.addTextChangedListener(this)
                }

                val spannable = SpannableStringBuilder(s)
                val globalStrikethroughs = spannable.getSpans(0, spannable.length, android.text.style.StrikethroughSpan::class.java)
                for (span in globalStrikethroughs) {
                    if (spannable.getSpanStart(span) == 0 && spannable.getSpanEnd(span) == spannable.length) {
                        spannable.removeSpan(span)
                    }
                }

                val html = Html.toHtml(spannable, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                    .replace("<p dir=\"ltr\">", "").replace("</p>", "").replace("\n", "")
                
                subTask.title = html
                onSubTaskChanged(subTask)
            }
        }
        editText.addTextChangedListener(textWatcher)
        editText.tag = textWatcher
    }

    private fun setupKeyboardActions(editText: EditText, subTask: SubTask, holder: RecyclerView.ViewHolder) {
        editText.setOnEditorActionListener { _, actionId, event ->
            val currentPos = holder.bindingAdapterPosition
            if (currentPos == -1) return@setOnEditorActionListener false

            if (actionId == EditorInfo.IME_ACTION_NEXT || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                
                if (!isLayoutChanging) {
                    isLayoutChanging = true
                    
                    val currentEditText = activeEditText ?: editText
                    val text = currentEditText.text ?: ""
                    val selectionStart = currentEditText.selectionStart

                    val spannable = SpannableStringBuilder(text)
                    val globalStrikethroughs = spannable.getSpans(0, spannable.length, android.text.style.StrikethroughSpan::class.java)
                    for (span in globalStrikethroughs) {
                        if (spannable.getSpanStart(span) == 0 && spannable.getSpanEnd(span) == spannable.length) {
                            spannable.removeSpan(span)
                        }
                    }
                    
                    val prefixHtml = if (selectionStart > 0) {
                        Html.toHtml(spannable.subSequence(0, selectionStart) as Spanned, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                            .replace("<p dir=\"ltr\">", "").replace("</p>", "").replace("\n", "")
                    } else {
                        ""
                    }
                    
                    subTasks[currentPos].title = prefixHtml
                    onSubTaskChanged(subTasks[currentPos])
                    
                    val suffixHtml = if (selectionStart < text.length) {
                         Html.toHtml(spannable.subSequence(selectionStart, text.length) as Spanned, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                             .replace("<p dir=\"ltr\">", "").replace("</p>", "").replace("\n", "")
                    } else {
                        ""
                    }

                    val newLine = SubTask(
                        title = suffixHtml,
                        isTask = subTask.isTask,
                        isCompleted = false
                    )
                    
                    subTasks.add(currentPos + 1, newLine)
                    pendingFocusPosition = currentPos + 1
                    pendingSelectionStart = 0
                    
                    notifyItemChanged(currentPos)
                    notifyItemInserted(currentPos + 1)
                    
                    onDataUpdated()
                    
                    editText.postDelayed({
                        isLayoutChanging = false
                    }, 50)
                }
                true
            } else false
        }

        editText.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                if (isLayoutChanging) return@setOnKeyListener true
                
                val currentPos = holder.bindingAdapterPosition
                val selectionStart = editText.selectionStart
                val currentTextStr = editText.text.toString()
                
                // Handle deletion at the start of the line, considering the \u200B anchor
                val isAtStart = selectionStart == 0 || (selectionStart == 1 && currentTextStr.startsWith("\u200B"))
                
                if (isAtStart && currentPos != -1) {
                    if (subTask.isTask && (currentTextStr.replace("\u200B", "").isEmpty())) {
                        isLayoutChanging = true
                        subTask.isTask = false
                        notifyItemChanged(currentPos)
                        onDataUpdated()
                        editText.postDelayed({ isLayoutChanging = false }, 50)
                        return@setOnKeyListener true
                    } else if (currentPos > 0) {
                        isLayoutChanging = true
                        
                        val aboveItem = subTasks[currentPos - 1]
                        val aboveSpanned = Html.fromHtml(aboveItem.title, Html.FROM_HTML_MODE_COMPACT)
                        val aboveTextLength = aboveSpanned.toString().replace("\u200B", "").length
                        
                        val currentText = editText.text ?: ""
                        val combined = SpannableStringBuilder(aboveSpanned)
                        
                        if (combined.isNotEmpty() && combined.toString().endsWith("\u200B") && currentText.toString().replace("\u200B", "").isNotEmpty()) {
                             combined.delete(combined.length - 1, combined.length)
                        }
                        
                        combined.append(currentText)
                        
                        val finalCombined = SpannableStringBuilder()
                        val plainString = combined.toString()
                        val hasRealText = plainString.replace("\u200B", "").isNotEmpty()
                        
                        if (hasRealText) {
                            for (i in combined.indices) {
                                if (combined[i] != '\u200B') {
                                    finalCombined.append(combined[i])
                                    val spans = combined.getSpans(i, i + 1, Any::class.java)
                                    for (span in spans) {
                                        val start = combined.getSpanStart(span)
                                        val end = combined.getSpanEnd(span)
                                        val flags = combined.getSpanFlags(span)
                                        if (start <= i && end > i) {
                                            finalCombined.setSpan(span, finalCombined.length - 1, finalCombined.length, flags)
                                        }
                                    }
                                }
                            }
                        }

                        aboveItem.title = Html.toHtml(finalCombined, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                            .replace("<p dir=\"ltr\">", "").replace("</p>", "").replace("\n", "")
                        onSubTaskChanged(aboveItem)
                        
                        subTasks.removeAt(currentPos)
                        
                        pendingFocusPosition = currentPos - 1
                        pendingSelectionStart = aboveTextLength
                        
                        notifyItemRemoved(currentPos)
                        notifyItemChanged(currentPos - 1)
                        onDataUpdated()
                        
                        v.postDelayed({
                            isLayoutChanging = false
                        }, 50)
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    override fun getItemCount() = subTasks.size

    fun toggleTodoModeForCurrentFocus() {
        if (currentFocusedPosition != -1 && currentFocusedPosition < subTasks.size) {
            val subTask = subTasks[currentFocusedPosition]
            subTask.isTask = !subTask.isTask
            pendingFocusPosition = currentFocusedPosition
            notifyItemChanged(currentFocusedPosition)
            onDataUpdated()
        }
    }

    fun toggleTaskCompletion(position: Int) {
        if (position in subTasks.indices) {
            val subTask = subTasks[position]
            subTask.isCompleted = !subTask.isCompleted
            
            if (position == currentFocusedPosition) {
                val editText = activeEditText ?: return
                val editable = editText.text ?: return
                if (subTask.isCompleted) {
                    editable.setSpan(StrikethroughSpan(), 0, editable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    val spans = editable.getSpans(0, editable.length, StrikethroughSpan::class.java)
                    for (span in spans) {
                        if (editable.getSpanStart(span) == 0 && editable.getSpanEnd(span) == editable.length) {
                            editable.removeSpan(span)
                        }
                    }
                }
                (editText as? RichEditText)?.isStrikingThrough = subTask.isCompleted
            }

            onSubTaskChanged(subTask)
            notifyItemChanged(position)
        }
    }

    fun toggleBold() {
        (activeEditText as? RichEditText)?.toggleStyle(Typeface.BOLD)
        saveActiveContent()
    }

    fun toggleItalic() {
        (activeEditText as? RichEditText)?.toggleStyle(Typeface.ITALIC)
        saveActiveContent()
    }

    fun toggleUnderline() {
        (activeEditText as? RichEditText)?.toggleUnderline()
        saveActiveContent()
    }

    fun toggleTextColor(color: Int) {
        (activeEditText as? RichEditText)?.toggleColor(color)
        saveActiveContent()
    }

    private fun saveActiveContent() {
        val editText = activeEditText ?: return
        val editable = editText.text ?: return
        val currentPos = currentFocusedPosition
        if (currentPos == -1 || currentPos >= subTasks.size) return

        val spannable = SpannableStringBuilder(editable)
        val globalStrikethroughs = spannable.getSpans(0, spannable.length, StrikethroughSpan::class.java)
        for (span in globalStrikethroughs) {
            if (spannable.getSpanStart(span) == 0 && spannable.getSpanEnd(span) == spannable.length) {
                spannable.removeSpan(span)
            }
        }

        val html = Html.toHtml(spannable, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
            .replace("<p dir=\"ltr\">", "").replace("</p>", "").replace("\n", "")
        subTasks[currentPos].title = html
        onSubTaskChanged(subTasks[currentPos])
        onSelectionChanged(editText)
    }

    fun updateData(newList: List<SubTask>) {
        subTasks.clear()
        subTasks.addAll(newList)
        currentFocusedPosition = -1
        activeEditText = null
        notifyDataSetChanged()
    }

    fun getSubTasks(): List<SubTask> = subTasks
}
