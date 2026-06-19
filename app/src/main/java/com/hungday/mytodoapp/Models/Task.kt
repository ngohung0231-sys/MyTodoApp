package com.hungday.mytodoapp.Models

import java.util.UUID


data class Task(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val desc: String?,
    val importance: String,
    val isCompleted: Boolean = false
)