package com.hungday.mytodoapp.view

import android.content.Context
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class RichEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    interface OnSelectionChangedListener {
        fun onSelectionChanged(selStart: Int, selEnd: Int)
    }

    var selectionChangedListener: OnSelectionChangedListener? = null
    private var ignoreTextChanges = false

    var isStrikingThrough: Boolean = false
        set(value) {
            field = value
            updateStrikeThrough()
        }

    private fun updateStrikeThrough() {
        val currentFlags = paintFlags
        if (isStrikingThrough) {
            paintFlags = currentFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            setTextColor(android.graphics.Color.GRAY)
            alpha = 0.6f
        } else {
            paintFlags = currentFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            setTextColor(android.graphics.Color.BLACK)
            alpha = 1.0f
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!ignoreTextChanges) {
            selectionChangedListener?.onSelectionChanged(selStart, selEnd)
        }
    }

    fun setTextWithoutTriggeringSelection(text: CharSequence?) {
        ignoreTextChanges = true
        val currentText = this.text?.toString() ?: ""
        val newText = text?.toString() ?: ""
        
        if (currentText != newText) {
            setText(text)
        }
        updateStrikeThrough()
        ignoreTextChanges = false
    }

    fun toggleStyle(style: Int) {
        val start = selectionStart
        val end = selectionEnd
        if (start == -1 || end == -1) return

        val editable = text ?: return
        
        if (start == end) {
            // Typing Style: Toggle for next characters
            val spans = editable.getSpans(start, start, StyleSpan::class.java)
            val existingSpan = spans.find { it.style == style }
            if (existingSpan != null) {
                editable.removeSpan(existingSpan)
            } else {
                editable.setSpan(StyleSpan(style), start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
        } else {
            // Range Selection: Toggle for selection
            val spans = editable.getSpans(start, end, StyleSpan::class.java)
            var found = false
            for (span in spans) {
                if (span.style == style) {
                    editable.removeSpan(span)
                    found = true
                }
            }
            if (!found) {
                editable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun toggleUnderline() {
        val start = selectionStart
        val end = selectionEnd
        if (start == -1 || end == -1) return

        val editable = text ?: return
        val spans = editable.getSpans(start, if (start == end) start else end, UnderlineSpan::class.java)
        
        if (start == end) {
            if (spans.isNotEmpty()) {
                editable.removeSpan(spans[0])
            } else {
                editable.setSpan(UnderlineSpan(), start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
        } else {
            if (spans.isNotEmpty()) {
                for (span in spans) editable.removeSpan(span)
            } else {
                editable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    fun toggleColor(color: Int) {
        val start = selectionStart
        val end = selectionEnd
        if (start == -1 || end == -1) return

        val editable = text ?: return
        
        if (start == end) {
            // Typing Style
            val spans = editable.getSpans(start, start, ForegroundColorSpan::class.java)
            for (span in spans) editable.removeSpan(span)
            
            if (color != android.graphics.Color.BLACK) {
                editable.setSpan(ForegroundColorSpan(color), start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
            }
        } else {
            // Range Selection
            val spans = editable.getSpans(start, end, ForegroundColorSpan::class.java)
            for (span in spans) editable.removeSpan(span)
            
            if (color != android.graphics.Color.BLACK) {
                editable.setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}
