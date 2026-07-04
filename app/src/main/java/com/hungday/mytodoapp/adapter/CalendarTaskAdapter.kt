package com.hungday.mytodoapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Task
import androidx.core.graphics.toColorInt

class CalendarTaskAdapter(
    private val tasks: List<Task>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<CalendarTaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.tvTaskTitle.text = task.title

        if (task.timeStr.isNullOrEmpty()) {
            holder.tvTaskTime.visibility = View.GONE
        } else {
            holder.tvTaskTime.visibility = View.VISIBLE
            holder.tvTaskTime.text = task.timeStr
        }

        // Đổi màu thanh dọc Priority của RIÊNG task này
        when (task.priority) {
            "High" -> holder.viewPriorityIndicator.setBackgroundColor("#EE4D5E".toColorInt()) // Red
            "Medium" -> holder.viewPriorityIndicator.setBackgroundColor("#4997CF".toColorInt()) // Blue
            else -> holder.viewPriorityIndicator.setBackgroundColor("#44BE65".toColorInt()) // Green (Low)
        }

        holder.itemView.setOnClickListener {
            onTaskClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewPriorityIndicator: View = view.findViewById(R.id.viewTaskPriorityIndicator)
        val tvTaskTitle: TextView = view.findViewById(R.id.tvCalendarTaskTitle)
        val tvTaskTime: TextView = view.findViewById(R.id.tvCalendarTaskTime)
    }
}