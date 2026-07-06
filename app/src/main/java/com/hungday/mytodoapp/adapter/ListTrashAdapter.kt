package com.hungday.mytodoapp.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.TodoList
import com.hungday.mytodoapp.model.TrashItem

class ListTrashAdapter(
    private var trashList: List<TrashItem>,
    private val onRestoreClick: (TrashItem) -> Unit
) : RecyclerView.Adapter<ListTrashAdapter.ListTrashViewHolder>() {

    class ListTrashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvListTitle: TextView = itemView.findViewById(R.id.tvListTitle)
        val imgListIcon: ImageView = itemView.findViewById(R.id.imgListIcon)
        val imgFolderIconBg: ImageView = itemView.findViewById(R.id.imgFolderIconBg)
        val clListContent: View = itemView.findViewById(R.id.clListContent)
        val pbListProgress: ProgressBar = itemView.findViewById(R.id.pbListProgress)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val imgRightArrow: TextView = itemView.findViewById(R.id.imgRightArrow)
        val btnRestore: View = itemView.findViewById(R.id.btnRestore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListTrashViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_trash, parent, false)
        return ListTrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListTrashViewHolder, position: Int) {
        val trashItem = trashList[position]
        val todoList = Gson().fromJson(trashItem.folderDataJson, TodoList::class.java)

        holder.tvListTitle.text = todoList.title
        holder.imgListIcon.setImageResource(todoList.icon)
        
        val listColor = todoList.color
        holder.imgFolderIconBg.backgroundTintList = ColorStateList.valueOf(listColor)
        
        // Use a lighter version of the color for background, same as item_list style
        val lightColor = ColorUtils.setAlphaComponent(listColor, 30) // ~12% alpha
        holder.clListContent.setBackgroundColor(lightColor)

        // Calculate progress
        val total = todoList.subTasks.size
        val completed = todoList.subTasks.count { it.isCompleted }
        val progress = if (total > 0) (completed * 100 / total) else 0
        
        holder.pbListProgress.progress = progress
        holder.tvPercentage.text = "$progress%"
        holder.tvPercentage.setTextColor(Color.BLACK)
        holder.imgRightArrow.setTextColor(Color.BLACK)

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