package com.hungday.mytodoapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.model.OnboardingItem
import com.hungday.mytodoapp.R
class OnboardingAdapter(private val onboardingItems: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnBoardingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnBoardingViewHolder(view)
    }

    class OnBoardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img = itemView.findViewById<ImageView>(R.id.imgOnboard)
        val tvTitle = itemView.findViewById<TextView>(R.id.tvTitleOnboard)
        val tvDesc = itemView.findViewById<TextView>(R.id.tvDescOnboard)
    }

    override fun onBindViewHolder(holder: OnBoardingViewHolder, position: Int) {
        val items = onboardingItems[position]
        holder.img.setImageResource(items.image)
        holder.tvTitle.setText(items.titleRes)
        holder.tvDesc.setText(items.descRes)

        holder.itemView.setBackgroundResource(items.backgroundColor)
    }

    override fun getItemCount(): Int = onboardingItems.size
}