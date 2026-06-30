package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.CalendarDay

class CalendarAdapter(private val daysList: List<CalendarDay>,
                      private val onDaySelected: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalenderViewHolder>() {

    private var selectedPosition = 0
    class CalenderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutCalendarDay = itemView.findViewById<CardView>(R.id.layoutCalendarDay)
        val tvDayOfWeek = itemView.findViewById<TextView>(R.id.tvDayOfWeek)
        val tvDayOfMonth = itemView.findViewById<TextView>(R.id.tvDayOfMonth)
        val viewDot = itemView.findViewById<View>(R.id.viewDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalenderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
        return CalenderViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalenderViewHolder, position: Int) {
        val day = daysList[position]
        holder.tvDayOfWeek.text = day.dayOfWeek
        holder.tvDayOfMonth.text = day.dayOfMonth

        holder.viewDot.visibility = if (day.hasTask) View.VISIBLE else View.GONE

        if(position == selectedPosition) {
            holder.layoutCalendarDay.setBackgroundResource(R.drawable.filter_calendar_day_selected)
            holder.tvDayOfWeek.setTextColor("#ffffff".toColorInt())
            holder.tvDayOfMonth.setTextColor("#ffffff".toColorInt())
        } else {
            holder.layoutCalendarDay.setBackgroundResource(android.R.color.transparent)
            holder.tvDayOfWeek.setTextColor("#A0A0A0".toColorInt())
            holder.tvDayOfMonth.setTextColor("#A0A0A0".toColorInt())
        }

        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition

            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)

            onDaySelected(day)
        }
    }

    override fun getItemCount(): Int = daysList.size

    fun selectToday() {
        val today = java.time.LocalDate.now()
        val index = daysList.indexOfFirst { it.date == today }
        if (index != -1 && index != selectedPosition) {
            val oldPos = selectedPosition
            selectedPosition = index
            notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
        }
    }
}