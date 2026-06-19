package com.hungday.mytodoapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.hungday.mytodoapp.Models.OnboardingItem
import com.hungday.mytodoapp.Models.onboardingList
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.OnboardingAdapter

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val adapter = OnboardingAdapter(onboardingList)
        viewPager.adapter = adapter

        val btnNext = view.findViewById<Button>(R.id.btnNext)
        val btnSkip = view.findViewById<Button>(R.id.btnSkip)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentItem = onboardingList[position]
                viewPager.setBackgroundColor(currentItem.backgroundColor)

                if(position == onboardingList.size - 1) {

                    btnNext.text = "Get Started"
                } else {
                    btnNext.text = "Next"
                }
            }
        })

        btnNext.setOnClickListener {
            val currentPos = viewPager.currentItem
            if(currentPos < onboardingList.size - 1) {
                viewPager.currentItem = currentPos + 1
            } else {
                findNavController().navigate(R.id.action_onboarding_to_setupProfile)
            }
        }

        btnSkip.setOnClickListener {
            findNavController().navigate(R.id.action_onboarding_to_setupProfile)
        }
    }
}