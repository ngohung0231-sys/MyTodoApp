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

        holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#f3f5f9"))
        
        holder.tvFolderName.text = folder.folderName
        holder.tvFolderName.setTextColor(folder.folderColor)
        holder.imgArrow.setColorFilter(folder.folderColor)
        
        // Use counts from backup if available, otherwise 0
        val listCount = backup.lists.size
        val taskCount = backup.tasks.size
        
        holder.tvListCount.text = if (listCount < 2) "$listCount List" else "$listCount Lists"
        holder.tvTaskCount.text = if (taskCount < 2) "$taskCount Task" else "$taskCount Tasks"
        
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