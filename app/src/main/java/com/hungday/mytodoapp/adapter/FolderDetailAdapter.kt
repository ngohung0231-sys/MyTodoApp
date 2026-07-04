package com.hungday.mytodoapp.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.TodoList
import com.hungday.mytodoapp.utils.ColorHelper

class FolderDetailAdapter(
    private var listList: List<TodoList>,
    private val onListItemClick: (TodoList) -> Unit
) : RecyclerView.Adapter<FolderDetailAdapter.FolderDetailViewHolder>() {
    
    class FolderDetailViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val imgFolderIcon = itemView.findViewById<ImageView>(R.id.imgFolderIcon)
        val imgFolderIconBg = itemView.findViewById<ShapeableImageView>(R.id.imgFolderIconBg)
        val progressList = itemView.findViewById<ProgressBar>(R.id.progressList)
        val tvListName = itemView.findViewById<TextView>(R.id.tvListName)
        val tvProgressList = itemView.findViewById<TextView>(R.id.tvProgressList)
        val clListContent = itemView.findViewById<ConstraintLayout>(R.id.clListContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderDetailViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return FolderDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderDetailViewHolder, position: Int) {
        val currentList = listList[position]

        holder.itemView.setOnClickListener {
            onListItemClick(currentList)
        }

        // 1. Hiển thị tên List
        holder.tvListName.text = currentList.title

        // 2. Thiết lập Icon và Màu sắc
        holder.imgFolderIcon.setImageResource(currentList.icon)
        holder.imgFolderIcon.setColorFilter(android.graphics.Color.BLACK)
        val mainColor = currentList.color

        // Cập nhật màu background cho icon tròn
        holder.imgFolderIconBg.backgroundTintList = ColorStateList.valueOf(mainColor)
        
        val lightColor = ColorHelper.lightenColor(mainColor)
        holder.clListContent.setBackgroundColor(lightColor)
        holder.clListContent.setOnClickListener { onListItemClick(currentList) }

        // 3. Xử lý tính toán Tiến độ (Progress)
        // Dựa trên danh sách subTasks (nhiệm vụ lẻ) bên trong TodoList
        // Chỉ tính các task thực sự (không tính header)
        val tasksOnly = currentList.subTasks.filter { it.isTask }
        val totalTasks = tasksOnly.size
        val completedTasks = tasksOnly.count { it.isCompleted }
        
        // Tính % hoàn thành
        val progressPercent = if (totalTasks > 0) {
            (completedTasks * 100) / totalTasks
        } else {
            0
        }
        
        // Cập nhật ProgressBar
        holder.progressList.progress = progressPercent
        holder.progressList.progressTintList = ColorStateList.valueOf(mainColor)
        
        // Cập nhật text hiển thị % (ví dụ: 20%)
        holder.tvProgressList.text = "$progressPercent%"
    }

    override fun getItemCount() = listList.size

    fun updateData(newList: List<TodoList>) {
        this.listList = newList
        notifyDataSetChanged()
    }
}
