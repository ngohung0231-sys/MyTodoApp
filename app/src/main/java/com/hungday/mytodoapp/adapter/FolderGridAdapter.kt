package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Folder
import androidx.core.graphics.toColorInt

class FolderGridAdapter(
    private var folderList: List<Folder>,
    private val onNewFolderClick: () -> Unit,
    private val onFolderClick: (Folder) -> Unit,
    private val onDeleteFolderClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderGridAdapter.FolderGridViewHolder>() {

    private var isEditMode: Boolean = false

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
        val ivDeleteFolder: ImageView = itemView.findViewById(R.id.ivDeleteFolder)
        val cardView: CardView = itemView.findViewById(R.id.cardFolder)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_NEW_FOLDER else TYPE_FOLDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderGridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_grid, parent, false)
        return FolderGridViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderGridViewHolder, position: Int) {
        val context = holder.itemView.context
        if (getItemViewType(position) == TYPE_NEW_FOLDER) {
            holder.containerFolderData.visibility = View.GONE
            holder.containerNewFolder.visibility = View.VISIBLE
            holder.cardView.setCardBackgroundColor("#dbe9f5".toColorInt())
            holder.ivDeleteFolder.visibility = View.GONE
            holder.itemView.clearAnimation()
            
            holder.cardView.setOnClickListener { 
                if (!isEditMode) onNewFolderClick() 
            }
        } else {
            val folder = folderList[position - 1]
            holder.containerFolderData.visibility = View.VISIBLE
            holder.containerNewFolder.visibility = View.GONE
            holder.cardView.setCardBackgroundColor("#f3f5f9".toColorInt())
            
            val folderName = when (folder.folderName) {
                "Others" -> context.getString(R.string.others)
                "Personal" -> context.getString(R.string.personal)
                "Exercise" -> context.getString(R.string.exercise)
                "Travel" -> context.getString(R.string.travel)
                "Study" -> context.getString(R.string.study)
                "Groceries" -> context.getString(R.string.shopping)
                else -> folder.folderName
            }
            holder.tvFolderName.text = folderName
            holder.tvFolderName.setTextColor(folder.folderColor)
            holder.imgArrow.setColorFilter(folder.folderColor)
            
            holder.tvListCount.text = if (folder.listCount < 2) context.getString(R.string.list_format, folder.listCount) else context.getString(R.string.lists_format, folder.listCount)
            holder.tvTaskCount.text = if (folder.taskCount < 2) context.getString(R.string.task_format_val, folder.taskCount) else context.getString(R.string.tasks_format_val, folder.taskCount)
            
            holder.imgFolderIllustration.setImageResource(folder.folderImg)
            holder.imgFolderIllustration.setColorFilter(folder.folderColor)

            // Edit Mode handling
            if (isEditMode) {
                holder.ivDeleteFolder.visibility = View.VISIBLE
                val shakeAnim = AnimationUtils.loadAnimation(context, R.anim.folder_shake)
                holder.itemView.startAnimation(shakeAnim)
            } else {
                holder.ivDeleteFolder.visibility = View.GONE
                holder.itemView.clearAnimation()
            }

            holder.ivDeleteFolder.setOnClickListener {
                onDeleteFolderClick(folder)
            }

            holder.cardView.setOnClickListener {
                if (!isEditMode) onFolderClick(folder) 
            }
        }
    }

    override fun getItemCount(): Int = folderList.size + 1

    fun updateData(newList: List<Folder>) {
        this.folderList = newList
        notifyDataSetChanged()
    }

    fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        notifyDataSetChanged()
    }
}