package com.hungday.mytodoapp.model

data class SubTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String = "",
    var isCompleted: Boolean = false,
    var isTask: Boolean = true
)
