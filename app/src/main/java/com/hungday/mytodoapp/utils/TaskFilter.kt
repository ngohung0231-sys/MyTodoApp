package com.hungday.mytodoapp.utils

import com.hungday.mytodoapp.model.Task
import java.time.LocalDate

object TaskFilter {

    /**
     * Hàm lọc danh sách Task dựa trên ngày đang xem (targetDate)
     * @param allTasks Danh sách tất cả các task từ database
     * @param targetDate Ngày người dùng đang xem trên lịch hoặc màn hình chính
     * @return Danh sách các task hợp lệ để hiển thị trong ngày đó
     */
    fun filterTasksByDate(allTasks: List<Task>, targetDate: LocalDate): List<Task> {
        return allTasks.filter { task ->
            when (task.repeatType) {
                "NONE" -> {
                    // Task không lặp: chỉ hiển thị nếu ngày tạo/cấu hình trùng với ngày đang xem
                    task.date == targetDate
                }
                "WEEKLY" -> {
                    // Task lặp hàng tuần: kiểm tra Thứ của targetDate có trong repeatValues không
                    // repeatValues lưu dạng "1,3,5" (1=Thứ 2, ..., 7=Chủ nhật)
                    val dayOfWeek = getCustomDayOfWeek(targetDate)
                    val repeatDays = task.repeatValues?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
                    
                    // Task lặp chỉ hiển thị từ ngày bắt đầu trở đi
                    val isAfterStart = !targetDate.isBefore(task.date)
                    isAfterStart && repeatDays.contains(dayOfWeek)
                }
                "MONTHLY" -> {
                    // Task lặp hàng tháng: kiểm tra ngày trong tháng (1-31)
                    val dayOfMonth = targetDate.dayOfMonth
                    val repeatDay = task.repeatValues?.toIntOrNull() ?: 0
                    
                    val isAfterStart = !targetDate.isBefore(task.date)
                    if (!isAfterStart) return@filter false

                    // Xử lý case ngày 31: Nếu tháng hiện tại không có ngày 31, thì ngày cuối tháng sẽ khớp
                    val lastDayOfMonth = targetDate.lengthOfMonth()
                    val actualRepeatDay = if (repeatDay > lastDayOfMonth) lastDayOfMonth else repeatDay
                    
                    dayOfMonth == actualRepeatDay
                }
                "YEARLY" -> {
                    // Task lặp hàng năm: kiểm tra tháng và ngày
                    // repeatValues lưu dạng "month,day" (Ví dụ: "3,31")
                    val parts = task.repeatValues?.split(",") ?: return@filter false
                    if (parts.size < 2) return@filter false
                    
                    val repeatMonth = parts[0].toIntOrNull() ?: 0
                    val repeatDay = parts[1].toIntOrNull() ?: 0
                    
                    val isAfterStart = !targetDate.isBefore(task.date)
                    if (!isAfterStart || targetDate.monthValue != repeatMonth) return@filter false

                    // Xử lý tương tự Monthly cho ngày 29/2 hoặc 31/tháng
                    val lastDayOfMonth = targetDate.lengthOfMonth()
                    val actualRepeatDay = if (repeatDay > lastDayOfMonth) lastDayOfMonth else repeatDay
                    
                    targetDate.dayOfMonth == actualRepeatDay
                }
                else -> false
            }
        }
    }

    /**
     * Chuyển đổi DayOfWeek của LocalDate sang hệ 1-7 (Thứ 2 là 1, Chủ nhật là 7)
     */
    private fun getCustomDayOfWeek(date: LocalDate): Int {
        // getValue() của DayOfWeek: 1 (Mon) to 7 (Sun) - Khớp với logic lưu trữ
        return date.dayOfWeek.value
    }
}
