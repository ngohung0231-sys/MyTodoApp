package com.hungday.mytodoapp.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.HourTimeline
import com.hungday.mytodoapp.model.Task
import androidx.core.graphics.toColorInt

class TimelineHourAdapter(
    private var timelineList: List<HourTimeline>,
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TimelineHourAdapter.HourViewHolder>() {

    fun updateData(newList: List<HourTimeline>) {
        this.timelineList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_timeline_hour, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        val item = timelineList[position]
        holder.tvTimelineHour.text = item.hourText

        // Nếu khung giờ này có task thì bôi đậm chữ giờ lên cho dễ quét UI
        if (item.tasks.isNotEmpty()) {
            holder.tvTimelineHour.setTypeface(null, Typeface.BOLD)
            holder.tvTimelineHour.setTextColor("#111111".toColorInt())
        } else {
            holder.tvTimelineHour.setTypeface(null, Typeface.NORMAL)
            holder.tvTimelineHour.setTextColor("#8E8E93".toColorInt())
        }

        // Nạp danh sách các task con tối giản vào bên phải
        val childAdapter = CalendarTaskAdapter(item.tasks) { task ->
            onTaskClick(task)
        }
        holder.rvHourTasks.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.rvHourTasks.adapter = childAdapter
    }

    override fun getItemCount(): Int = timelineList.size

    class HourViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTimelineHour: TextView = view.findViewById(R.id.tvTimelineHour)
        val rvHourTasks: RecyclerView = view.findViewById(R.id.rvHourTasks)
    }
}