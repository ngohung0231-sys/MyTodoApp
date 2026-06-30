package com.hungday.mytodoapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.CalendarDay
import java.time.LocalDate

class CalendarGridAdapter(
    private var days: List<CalendarDay>,
    private var currentMonth: LocalDate,
    private val onDateClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarGridAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: FrameLayout = view.findViewById(R.id.rootView)
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val viewSelected: View = view.findViewById(R.id.viewSelected)
        val viewHasTask: View = view.findViewById(R.id.viewHasTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_date, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = days[position]
        holder.tvDay.text = day.dayOfMonth
        
        // Check if day is from current month
        val isCurrentMonth = day.date.month == currentMonth.month && day.date.year == currentMonth.year
        if (!isCurrentMonth) {
            holder.tvDay.setTextColor(Color.LTGRAY)
        } else {
            holder.tvDay.setTextColor(Color.BLACK)
        }

        // Selection state
        if (day.isSelected && isCurrentMonth) {
            holder.viewSelected.visibility = View.VISIBLE
            holder.tvDay.setTextColor(Color.WHITE)
        } else {
            holder.viewSelected.visibility = View.GONE
        }

        // Task indicator
        holder.viewHasTask.visibility = if (day.hasTask) View.VISIBLE else View.GONE

        holder.root.setOnClickListener {
            onDateClick(day)
        }
    }

    override fun getItemCount(): Int = days.size

    fun updateData(newDays: List<CalendarDay>, newMonth: LocalDate) {
        days = newDays
        currentMonth = newMonth
        notifyDataSetChanged()
    }
}