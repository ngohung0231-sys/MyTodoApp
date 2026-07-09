package com.hungday.mytodoapp.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hungday.mytodoapp.R
import androidx.core.graphics.toColorInt

class TextColorBottomSheetFragment(
    private val currentColor: Int,
    private val onColorSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    private val colors = listOf(
        Color.BLACK,
        "#4997cf".toColorInt(),
        "#ee4d5e".toColorInt(),
        "#44be65".toColorInt(),
        "#f89520".toColorInt(),
        "#fe6e9a".toColorInt(),
        "#a792ec".toColorInt(),
        "#00BCD4".toColorInt(),
        "#FF5722".toColorInt(),
        "#795548".toColorInt()
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_text_color_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvColors = view.findViewById<RecyclerView>(R.id.rvTextColors)
        rvColors.adapter = ColorAdapter(colors, currentColor) {
            onColorSelected(it)
            dismiss()
        }
    }

    class ColorAdapter(
        private val colors: List<Int>,
        private val currentColor: Int,
        private val onColorSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val viewColorCircle: View = view.findViewById(R.id.viewColorCircle)
            val viewSelectionOverlay: View = view.findViewById(R.id.viewSelectionOverlay)
            val imgTick: ImageView = view.findViewById(R.id.imgTick)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_text_color_circle, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val color = colors[position]
            
            // Set background color of the circle
            val drawable = holder.viewColorCircle.background.mutate()
            if (drawable is android.graphics.drawable.GradientDrawable) {
                drawable.setColor(color)
                if (color == Color.WHITE || color == Color.parseColor("#F3F5F9")) {
                    drawable.setStroke(2, Color.LTGRAY)
                } else {
                    drawable.setStroke(0, Color.TRANSPARENT)
                }
            }
            
            // Selection state: 70% white overlay + checkmark icon
            val isSelected = (color == currentColor)
            holder.viewSelectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.imgTick.visibility = if (isSelected) View.VISIBLE else View.GONE
            
            holder.itemView.setOnClickListener { onColorSelected(color) }
        }

        override fun getItemCount() = colors.size
    }
}
