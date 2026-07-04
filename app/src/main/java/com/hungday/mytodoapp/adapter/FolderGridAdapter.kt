package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Folder

class FolderGridAdapter(
    private var folderList: List<Folder>,
    private val onNewFolderClick: () -> Unit,
    private val onFolderClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderGridAdapter.FolderGridViewHolder>() {

    companion object {
        private const val TYPE_NEW_FOLDER = 0
        private const val TYPE_FOLDER = 1
    }

    class FolderGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val containerFolderData: LinearLayout = itemView.findViewById(R.id.containerFolderData)
        val containerNewFolder: LinearLayout = itemView.findViewById(R.id.containerNewFolder)
        val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)
        val tvListCount: TextView = itemView.findViewById(R.id.tvListCount)
        val tvTaskCount: TextView = itemView.findViewById(R.id.tvTaskCount)
        val imgFolderIllustration: ImageView = itemView.findViewById(R.id.imgFolderIllustration)
        val imgArrow: ImageView = itemView.findViewById(R.id.imgArrow)
        val cardView: CardView = itemView as CardView
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_NEW_FOLDER else TYPE_FOLDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderGridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_grid, parent, false)
        return FolderGridViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderGridViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_NEW_FOLDER) {
            holder.containerFolderData.visibility = View.GONE
            holder.containerNewFolder.visibility = View.VISIBLE
            holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#dbe9f5"))
            holder.itemView.setOnClickListener { onNewFolderClick() }
        } else {
            val folder = folderList[position - 1]
            holder.containerFolderData.visibility = View.VISIBLE
            holder.containerNewFolder.visibility = View.GONE
            holder.cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#f3f5f9"))
            
            holder.tvFolderName.text = folder.folderName
            holder.tvFolderName.setTextColor(folder.folderColor)
            holder.imgArrow.setColorFilter(folder.folderColor)
            
            holder.tvListCount.text = if (folder.listCount < 2) "${folder.listCount} List" else "${folder.listCount} Lists"
            holder.tvTaskCount.text = if (folder.taskCount < 2) "${folder.taskCount} Task" else "${folder.taskCount} Tasks"
            
            holder.imgFolderIllustration.setImageResource(folder.folderImg)
            holder.imgFolderIllustration.setColorFilter(folder.folderColor)

            holder.itemView.setOnClickListener { onFolderClick(folder) }
        }
    }

    override fun getItemCount(): Int = folderList.size + 1

    fun updateData(newList: List<Folder>) {
        this.folderList = newList
        notifyDataSetChanged()
    }
}