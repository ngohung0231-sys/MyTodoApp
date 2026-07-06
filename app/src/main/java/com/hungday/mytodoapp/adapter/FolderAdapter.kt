package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Folder

class FolderAdapter(
    private var folderList: List<Folder>,
    private val onFolderClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFolderIc: ImageView = itemView.findViewById(R.id.imgFolderIc)
        val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)
        val tvListCount: TextView = itemView.findViewById(R.id.tvListCount)
        val tvTaskCount: TextView = itemView.findViewById(R.id.tvTaskCount)
    }

    fun updateData(newList: List<Folder>) {
        this.folderList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_home, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val currentFolder = folderList[position]

        holder.imgFolderIc.setImageResource(currentFolder.folderImg)
        holder.imgFolderIc.setColorFilter(currentFolder.folderColor)

        holder.tvFolderName.text = currentFolder.folderName
        holder.tvFolderName.setTextColor(currentFolder.folderColor)
        
        holder.tvListCount.text = if (currentFolder.listCount < 2) "${currentFolder.listCount} List" else "${currentFolder.listCount} Lists"
        holder.tvTaskCount.text = if (currentFolder.taskCount < 2) "${currentFolder.taskCount} Task" else "${currentFolder.taskCount} Tasks"

        holder.itemView.setOnClickListener {
            onFolderClick(currentFolder)
        }
    }

    override fun getItemCount(): Int {
        return folderList.size
    }
}