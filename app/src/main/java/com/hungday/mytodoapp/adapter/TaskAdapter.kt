package com.hungday.mytodoapp.adapter

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Task
import androidx.core.graphics.toColorInt
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

class TaskAdapter(
    private var taskList: List<Task>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskStatusChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cbTaskStatus: CheckBox = itemView.findViewById(R.id.cbTaskStatus)
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskTime: TextView = itemView.findViewById(R.id.tvTaskTime)
        val imgPriorityIcon: ImageView = itemView.findViewById(R.id.imgPriorityIcon)
        val tvPriorityText: TextView = itemView.findViewById(R.id.tvPriorityText)
        val imgNotifyBell: ImageView = itemView.findViewById(R.id.imgNotifyBell)
        val imgClock: ImageView = itemView.findViewById(R.id.imgClock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int {
        return taskList.size
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = taskList[position]

        holder.tvTaskTitle.text = currentTask.title
        val hasTime = !currentTask.timeStr.isNullOrEmpty()
        val hasDate = !currentTask.dateStr.isNullOrEmpty()
        val displayTime = when {
            hasTime && hasDate -> "${currentTask.timeStr}, ${currentTask.dateStr}"
            hasTime -> currentTask.timeStr
            else -> currentTask.dateStr
        }
        holder.tvTaskTime.text = displayTime
        val timeVisibility = if (hasDate || hasTime) View.VISIBLE else View.GONE
        holder.tvTaskTime.visibility = timeVisibility
        holder.imgClock.visibility = timeVisibility
        holder.imgNotifyBell.visibility = if (currentTask.isNotify != null) View.VISIBLE else View.GONE

        currentTask.time?.let {
            val taskDate = currentTask.date ?: LocalDate.MAX
            val isOverdue = (taskDate < LocalDate.now()) ||
                            (taskDate == LocalDate.now() && it < LocalTime.now())
            if (isOverdue && !currentTask.isCompleted) {
                holder.tvTaskTime.setTextColor(Color.RED)
                holder.imgClock.setColorFilter(Color.RED)
            } else {
                holder.tvTaskTime.setTextColor(Color.GRAY)
                holder.imgClock.setColorFilter(Color.GRAY)
            }
        } ?: run {
            val taskDate = currentTask.date ?: LocalDate.MAX
            val isOverdue = taskDate < LocalDate.now()
            if (isOverdue && !currentTask.isCompleted) {
                holder.tvTaskTime.setTextColor(Color.RED)
                holder.imgClock.setColorFilter(Color.RED)
            } else {
                holder.tvTaskTime.setTextColor(Color.GRAY)
                holder.imgClock.setColorFilter(Color.GRAY)
            }
        }
        holder.cbTaskStatus.setOnCheckedChangeListener(null)
        holder.cbTaskStatus.isChecked = currentTask.isCompleted

        holder.cbTaskStatus.setOnCheckedChangeListener { _, isChecked ->
            if (currentTask.isCompleted != isChecked) {
                currentTask.isCompleted = isChecked
                if (isChecked) {
                    holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    holder.tvTaskTitle.setTextColor(Color.GRAY)
                } else {
                    holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    holder.tvTaskTitle.setTextColor(Color.BLACK)
                }
                onTaskStatusChanged(currentTask, isChecked)
            }
        }

        holder.itemView.setOnClickListener {
            onTaskClick(currentTask)
        }

        if (currentTask.isCompleted) {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.tvTaskTitle.setTextColor(Color.GRAY)
        } else {
            holder.tvTaskTitle.paintFlags = holder.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.tvTaskTitle.setTextColor(Color.BLACK)
        }

        holder.tvPriorityText.text = currentTask.priority
        when (currentTask.priority) {
            "High" -> {
                holder.tvPriorityText.setTextColor("#ee4d5e".toColorInt())
                holder.imgPriorityIcon.setColorFilter("#ee4d5e".toColorInt())
            }
            "Medium" -> {
                val themeColor = getColorFromAttr(holder.itemView.context, R.attr.mainThemeColor)
                holder.tvPriorityText.setTextColor(themeColor)
                holder.imgPriorityIcon.setColorFilter(themeColor)
            }
            "Low" -> {
                holder.tvPriorityText.setTextColor("#44be65".toColorInt())
                holder.imgPriorityIcon.setColorFilter("#44be65".toColorInt())
            }
        }
    }

    @ColorInt
    private fun getColorFromAttr(context: android.content.Context, @AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrColor, typedValue, true)
        return typedValue.data
    }

    fun updateData(newList: List<Task>) {
        taskList = newList
        notifyDataSetChanged()
    }
}