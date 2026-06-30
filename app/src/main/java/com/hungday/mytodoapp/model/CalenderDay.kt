package com.hungday.mytodoapp.model

import java.time.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val dayOfWeek: String,
    val dayOfMonth: String,
    var isSelected: Boolean = false,
    var hasTask: Boolean = false
)