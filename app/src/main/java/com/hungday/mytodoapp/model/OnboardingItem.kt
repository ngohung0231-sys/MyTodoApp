package com.hungday.mytodoapp.model

import android.content.Context
import com.hungday.mytodoapp.R

data class OnboardingItem(
    val backgroundColor: Int,
    val image: Int,
    val titleRes: Int,
    val descRes: Int
)

fun getOnboardingList(context: Context): List<OnboardingItem> {
    return listOf(
        OnboardingItem(
            backgroundColor = R.color.blue_light,
            image = R.drawable.first,
            titleRes = R.string.onboarding_title_1,
            descRes = R.string.onboarding_desc_1
        ),
        OnboardingItem(
            backgroundColor = R.color.green_light,
            image = R.drawable.second,
            titleRes = R.string.onboarding_title_2,
            descRes = R.string.onboarding_desc_2
        ),
        OnboardingItem(
            backgroundColor = R.color.red_light,
            image = R.drawable.third,
            titleRes = R.string.onboarding_title_3,
            descRes = R.string.onboarding_desc_3
        )
    )
}
