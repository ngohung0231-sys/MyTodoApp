package com.hungday.mytodoapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Button
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.hungday.mytodoapp.model.onboardingList
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.OnboardingAdapter

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {
    // Adapters
    private lateinit var onboardingAdapter: OnboardingAdapter

    // UI Components
    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapters()
        setupListeners()
    }

    private fun initViews(view: View) {
        viewPager = view.findViewById(R.id.viewPager)
        btnNext = view.findViewById(R.id.btnNext)
        btnSkip = view.findViewById(R.id.btnSkip)
    }

    private fun setupAdapters() {
        onboardingAdapter = OnboardingAdapter(onboardingList)
        viewPager.adapter = onboardingAdapter
    }

    private fun setupListeners() {
        // Theo dõi chuyển trang
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentItem = onboardingList[position]
                viewPager.setBackgroundColor(currentItem.backgroundColor)
                btnNext.text = if (position == onboardingList.size - 1) "Get Started" else "Next"
            }
        })

        // Nút Next
        btnNext.setOnClickListener {
            val currentPos = viewPager.currentItem
            if (currentPos < onboardingList.size - 1) {
                viewPager.currentItem = currentPos + 1
            } else {
                navigateToSetupProfile()
            }
        }

        // Nút Skip
        btnSkip.setOnClickListener {
            navigateToSetupProfile()
        }
    }

    //-------------------- Các hàm chức năng bổ trợ (Helper Functions) --------------------//

    private fun navigateToSetupProfile() {
        findNavController().navigate(R.id.action_onboarding_to_setupProfile)
    }
}
