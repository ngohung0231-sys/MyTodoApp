package com.hungday.mytodoapp.Models

import com.hungday.mytodoapp.R

data class OnboardingItem(
    val backgroundColor: Int,
    val image: Int,
    val title: String,
    val desc: String
)

val onboardingList = listOf(
    OnboardingItem(
        backgroundColor = R.color.blue_light,
        image = R.drawable.first,
        title = "Welcome to ToDo",
        desc = "The simplest and most powerful way to manage your tasks and projects"
    ),
    OnboardingItem(
        backgroundColor = R.color.green_light,
        image = R.drawable.second,
        title = "There is so much ToDo",
        desc = "Create lists, add tasks, set deadlines, assign priorities, and track your progress with ease"
    ),
    OnboardingItem(
        backgroundColor = R.color.red_light,
        image = R.drawable.third,
        title = "Ready for local organization",
        desc = "All your data is securely stored and managed on this single device, Offline-first. Private and under your control."
    )
)


