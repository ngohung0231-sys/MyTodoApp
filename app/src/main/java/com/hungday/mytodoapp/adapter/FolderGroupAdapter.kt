package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.FolderWithTasks
import com.hungday.mytodoapp.model.Task

class FolderGroupAdapter(
    private var folderGroupList: List<FolderWithTasks>,
    private val onFolderClick: (Folder) -> Unit,
    private val onSettingClick: (Folder) -> Unit,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskStatusChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<FolderGroupAdapter.FolderGroupViewHolder>() {

    class FolderGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFolderName = itemView.findViewById<TextView>(R.id.tvFolderName)
        val rvTasks = itemView.findViewById<RecyclerView>(R.id.rvTasks)
        val btnTaskSetting = itemView.findViewById<ImageView>(R.id.btnTaskSetting)
        val lnlFolderDetail = itemView.findViewById<LinearLayout>(R.id.lnlFolderDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderGroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_group, parent, false)
        return FolderGroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderGroupViewHolder, position: Int) {
        val context = holder.itemView.context
        val currentFolderGroup = folderGroupList[position]
        val folderName = when (currentFolderGroup.folder.folderName) {
            "Others" -> context.getString(R.string.others)
            "Personal" -> context.getString(R.string.personal)
            "Exercise" -> context.getString(R.string.exercise)
            "Travel" -> context.getString(R.string.travel)
            "Study" -> context.getString(R.string.study)
            "Groceries", "Shopping" -> context.getString(R.string.shopping)
            else -> currentFolderGroup.folder.folderName
        }
        holder.tvFolderName.text = folderName
        holder.tvFolderName.setTextColor(currentFolderGroup.folder.folderColor)
        holder.btnTaskSetting.setColorFilter(currentFolderGroup.folder.folderColor)

        holder.btnTaskSetting.setOnClickListener {
            onSettingClick(currentFolderGroup.folder)
        }

        holder.lnlFolderDetail.setOnClickListener {
            onFolderClick(currentFolderGroup.folder)
        }

        val taskAdapter = TaskAdapter(currentFolderGroup.taskList, { task ->
            onTaskClick(task)
        }) { task, isCompleted ->
            onTaskStatusChanged(task, isCompleted)
        }
        holder.rvTasks.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            holder.itemView.context,
            androidx.recyclerview.widget.LinearLayoutManager.VERTICAL,
            false
        )
        holder.rvTasks.adapter = taskAdapter
        holder.rvTasks.isNestedScrollingEnabled = false
    }

    override fun getItemCount(): Int {
        return folderGroupList.size
    }

    fun updateData(newList: List<FolderWithTasks>) {
        this.folderGroupList = newList
        notifyDataSetChanged()
    }
}