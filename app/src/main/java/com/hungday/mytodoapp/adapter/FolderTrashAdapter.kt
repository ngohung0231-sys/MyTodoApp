package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.FolderBackup
import com.hungday.mytodoapp.model.TrashItem
import androidx.core.graphics.toColorInt

class FolderTrashAdapter(
    private var trashList: List<TrashItem>,
    private val onRestoreClick: (TrashItem) -> Unit
) : RecyclerView.Adapter<FolderTrashAdapter.FolderTrashViewHolder>() {

    class FolderTrashViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)
        val tvListCount: TextView = itemView.findViewById(R.id.tvListCount)
        val tvTaskCount: TextView = itemView.findViewById(R.id.tvTaskCount)
        val imgFolderIllustration: ImageView = itemView.findViewById(R.id.imgFolderIllustration)
        val imgArrow: ImageView = itemView.findViewById(R.id.imgArrow)
        val cardView: CardView = itemView.findViewById(R.id.cardFolder)
        val btnRestore: View = itemView.findViewById(R.id.btnRestore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderTrashViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_trash, parent, false)
        return FolderTrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderTrashViewHolder, position: Int) {
        val trashItem = trashList[position]
        val backup = Gson().fromJson(trashItem.folderDataJson, FolderBackup::class.java)
        val folder = backup.folder

        holder.cardView.setCardBackgroundColor("#f3f5f9".toColorInt())
        
        val context = holder.itemView.context
        val folderName = when (folder.folderName) {
            "Others" -> context.getString(R.string.others)
            "Personal" -> context.getString(R.string.personal)
            "Exercise" -> context.getString(R.string.exercise)
            "Travel" -> context.getString(R.string.travel)
            "Study" -> context.getString(R.string.study)
            "Groceries", "Shopping" -> context.getString(R.string.shopping)
            else -> folder.folderName
        }
        holder.tvFolderName.text = folderName
        holder.tvFolderName.setTextColor(folder.folderColor)
        holder.imgArrow.setColorFilter(folder.folderColor)
        
        // Use counts from backup if available, otherwise 0
        val listCount = backup.lists.size
        val taskCount = backup.tasks.size
        
        holder.tvListCount.text = if (listCount < 2) context.getString(R.string.list_format, listCount) else context.getString(R.string.lists_format, listCount)
        holder.tvTaskCount.text = if (taskCount < 2) context.getString(R.string.task_format_val, taskCount) else context.getString(R.string.tasks_format_val, taskCount)
        
        holder.imgFolderIllustration.setImageResource(folder.folderImg)
        holder.imgFolderIllustration.setColorFilter(folder.folderColor)

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