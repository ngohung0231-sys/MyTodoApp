package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Folder
import androidx.core.widget.ImageViewCompat
import android.content.res.ColorStateList

class FolderAddTaskAdapter(
    private val folderList: List<Folder>,
    private val onFolderClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAddTaskAdapter.FolderViewHolder>() {

    private var selectedPosition = 0

    fun setSelectedFolder(folderId: Int) {
        val index = folderList.indexOfFirst { it.folderId == folderId }
        if (index != -1) {
            val oldPos = selectedPosition
            selectedPosition = index
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
        }
    }

    class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)
        val rbSelected: RadioButton = itemView.findViewById(R.id.rbSelected)
        val imgFolderIcon: ImageView = itemView.findViewById(R.id.imgFolderIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_add_task, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val currentFolder = folderList[position]
        val context = holder.itemView.context
        val folderName = when (currentFolder.folderName) {
            "Others" -> context.getString(R.string.others)
            "Personal" -> context.getString(R.string.personal)
            "Exercise" -> context.getString(R.string.exercise)
            "Travel" -> context.getString(R.string.travel)
            "Study" -> context.getString(R.string.study)
            "Groceries", "Shopping" -> context.getString(R.string.shopping)
            else -> currentFolder.folderName
        }
        holder.tvFolderName.text = folderName
        holder.tvFolderName.setTextColor(currentFolder.folderColor)

        holder.imgFolderIcon.setImageResource(currentFolder.folderImg)
        holder.imgFolderIcon.setColorFilter(currentFolder.folderColor)

        // Chuyển mã màu folderColor thành đối tượng ColorStateList để ép màu (Tint)
        val folderColorStateList = ColorStateList.valueOf(currentFolder.folderColor)

        // Tiến hành nhuộm màu cho icon Folder một cách an toàn
        ImageViewCompat.setImageTintList(holder.imgFolderIcon, folderColorStateList)

        // (Mẹo thêm) Nhuộm luôn màu cho chiếc RadioButton để đồng bộ với màu của Folder đó
        holder.rbSelected.buttonTintList = folderColorStateList
        
        // Cập nhật trạng thái cho RadioButton
        holder.rbSelected.isChecked = (position == selectedPosition)

        val clickListener = View.OnClickListener {
            // Cập nhật vị trí được chọn
            val oldPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            
            // Thông báo thay đổi để cập nhật UI
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            onFolderClick(currentFolder)
        }

        holder.itemView.setOnClickListener(clickListener)
        holder.rbSelected.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int = folderList.size
}