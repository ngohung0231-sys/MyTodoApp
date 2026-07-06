package com.hungday.mytodoapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: LocalTime? = null,
    val timeStr: String? = null,
    val priority: String,
    var isCompleted: Boolean = false,
    val isUpcoming: Boolean = false,
    val folderId: Int = 1,
    val isNotify: Int? = null,
    val date: LocalDate,
    val dateStr: String? = null,
    var completedAt: Long? = null,
    val repeatType: String = "NONE", // NONE, WEEKLY, MONTHLY
    val repeatValues: String? = null // "1,3,5" hoặc "15"
)