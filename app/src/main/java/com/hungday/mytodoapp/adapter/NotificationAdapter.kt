package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Task
import java.util.Locale

class NotificationAdapter(
    private var taskList: List<Task>,
    private val onTurnOffClick: (Task) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskDateTime: TextView = itemView.findViewById(R.id.tvTaskDateTime)
        val imgClock: ImageView = itemView.findViewById(R.id.imgClock)
        val tvPriorityText: TextView = itemView.findViewById(R.id.tvPriorityText)
        val imgPriorityIcon: ImageView = itemView.findViewById(R.id.imgPriorityIcon)
        val btnTurnOff: ViewGroup = itemView.findViewById(R.id.btnRestore) 
        val imgAction: ImageView = (itemView.findViewById<ViewGroup>(R.id.btnRestore)).getChildAt(0) as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_trash, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val task = taskList[position]

        holder.tvTaskTitle.text = task.title
        
        val hasTime = !task.timeStr.isNullOrEmpty()
        val hasDate = !task.dateStr.isNullOrEmpty()
        
        if (hasTime || hasDate) {
            holder.imgClock.visibility = View.VISIBLE
            holder.tvTaskDateTime.visibility = View.VISIBLE
            holder.tvTaskDateTime.text = if (hasDate) "${task.dateStr}, ${task.timeStr ?: ""}" else task.timeStr
        } else {
            holder.imgClock.visibility = View.GONE
            holder.tvTaskDateTime.visibility = View.GONE
        }

        val priorityColor = when (task.priority.lowercase(Locale.ROOT)) {
            "high" -> "#EE4D5E"
            "medium" -> "#4997CF"
            else -> "#44BE65"
        }
        holder.tvPriorityText.text = task.priority
        holder.tvPriorityText.setTextColor(priorityColor.toColorInt())
        holder.imgPriorityIcon.setColorFilter(priorityColor.toColorInt())

        // UI customization for notification list
        holder.imgAction.setImageResource(R.drawable.bell)
        holder.imgAction.setColorFilter(android.graphics.Color.parseColor("#EE4D5E"))
        
        holder.btnTurnOff.setOnClickListener {
            onTurnOffClick(task)
        }
        
        // Hide checkbox as these are active tasks
        holder.itemView.findViewById<View>(R.id.cbTaskStatus)?.visibility = View.GONE
    }

    override fun getItemCount(): Int = taskList.size

    fun updateData(newList: List<Task>) {
        this.taskList = newList
        notifyDataSetChanged()
    }
}