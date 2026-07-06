package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Task
import com.hungday.mytodoapp.model.TrashItem
import java.util.*

class TaskTrashAdapter(
    private var trashList: List<TrashItem>,
    private val onRestoreClick: (TrashItem) -> Unit
) : RecyclerView.Adapter<TaskTrashAdapter.TaskTrashViewHolder>() {

    class TaskTrashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        val tvTaskDateTime: TextView = itemView.findViewById(R.id.tvTaskDateTime)
        val tvPriorityText: TextView = itemView.findViewById(R.id.tvPriorityText)
        val imgPriorityIcon: ImageView = itemView.findViewById(R.id.imgPriorityIcon)
        val btnRestore: View = itemView.findViewById(R.id.btnRestore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskTrashViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task_trash, parent, false)
        return TaskTrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskTrashViewHolder, position: Int) {
        val trashItem = trashList[position]
        val task = Gson().fromJson(trashItem.folderDataJson, Task::class.java)

        holder.tvTaskTitle.text = task.title
        
        val dateTime = "${task.timeStr ?: ""}, ${task.dateStr ?: ""}"
        holder.tvTaskDateTime.text = dateTime
        
        holder.tvPriorityText.text = task.priority
        val priorityColor = when (task.priority.lowercase(Locale.ROOT)) {
            "high" -> R.color.red
            "medium" -> R.color.blue
            else -> R.color.green
        }
        val color = ContextCompat.getColor(holder.itemView.context, priorityColor)
        holder.tvPriorityText.setTextColor(color)
        holder.imgPriorityIcon.setColorFilter(color)

        holder.btnRestore.setOnClickListener {
            onRestoreClick(trashItem)
        }
    }

    override fun getItemCount(): Int = trashList.size

    fun updateData(newList: List<TrashItem>) {
        this.trashList = newList
        notifyDataSetChanged()
    }
}