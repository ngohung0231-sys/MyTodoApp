package com.hungday.mytodoapp.model

import java.time.LocalTime

data class HourTimeline(
    val hourText: String,        // Chuỗi hiển thị (Ví dụ: "09:00 AM")
    val startLocalTime: LocalTime?, // Mốc thời gian bắt đầu (Ví dụ: 09:00) - null cho All Day
    val tasks: List<Task>         // Danh sách các task thuộc khung giờ này
)