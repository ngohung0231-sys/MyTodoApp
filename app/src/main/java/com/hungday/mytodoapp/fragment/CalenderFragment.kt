package com.hungday.mytodoapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.CalendarGridAdapter
import com.hungday.mytodoapp.adapter.TimelineHourAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.CalendarDay
import com.hungday.mytodoapp.model.HourTimeline
import com.hungday.mytodoapp.model.Task
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalenderFragment : Fragment(R.layout.fragment_calender) {
    private lateinit var database: TodoDatabase
    private lateinit var repository: TodoRepository

    private lateinit var calendarGridAdapter: CalendarGridAdapter
    private lateinit var timelineHourAdapter: TimelineHourAdapter

    private var allTasks = mutableListOf<Task>()
    private var calendarDays = mutableListOf<CalendarDay>()

    private var selectedDate: LocalDate = LocalDate.now()
    private var currentMonth: LocalDate = LocalDate.now()

    private lateinit var btnBack: ImageView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var tvMonthYear: TextView
    private lateinit var rvCalendarGrid: RecyclerView
    private lateinit var rvTasks: RecyclerView
    private lateinit var tvSelectedDateLabel: TextView
    private lateinit var blank: FrameLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupAdapters()
        setupListeners()
        loadData()
        updateCalendar()
    }

    private fun initDatabase() {
        database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao())
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        rvCalendarGrid = view.findViewById(R.id.rvCalendarGrid)
        rvTasks = view.findViewById(R.id.rvTasks)
        tvSelectedDateLabel = view.findViewById(R.id.tvSelectedDateLabel)
        blank = view.findViewById(R.id.blank)
    }

    private fun setupAdapters() {
        // 1. Calendar Grid Adapter
        calendarGridAdapter = CalendarGridAdapter(calendarDays, currentMonth) { clickedDay ->
            selectedDate = clickedDay.date
            updateCalendar()
            refreshTasks()
        }
        rvCalendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendarGrid.adapter = calendarGridAdapter

        // 2. Timeline Hour Adapter
        timelineHourAdapter = TimelineHourAdapter(emptyList()) { task ->
            val bundle = Bundle().apply {
                putInt("taskId", task.id)
            }
            findNavController().navigate(R.id.editTaskFragment, bundle)
        }
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = timelineHourAdapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { findNavController().popBackStack() }
        btnPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateCalendar()
        }
        btnNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateCalendar()
        }
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allTasks.collect { tasks ->
                allTasks = tasks.toMutableList()
                updateCalendar()
                refreshTasks()
            }
        }
        // Lưu ý: Không cần collect allFolders ở màn hình này nữa vì chúng ta gom nhóm theo Giờ
    }

    private fun updateCalendar() {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        tvMonthYear.text = currentMonth.format(formatter)

        calendarDays.clear()

        val yearMonth = YearMonth.from(currentMonth)
        val firstOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()

        val dayOfWeek = firstOfMonth.dayOfWeek.value

        val prevMonth = currentMonth.minusMonths(1)
        val prevYearMonth = YearMonth.from(prevMonth)
        val daysInPrevMonth = prevYearMonth.lengthOfMonth()

        for (i in dayOfWeek - 1 downTo 1) {
            val date = prevMonth.withDayOfMonth(daysInPrevMonth - i + 1)
            calendarDays.add(CalendarDay(date, "", date.dayOfMonth.toString(), date == selectedDate, hasTask(date)))
        }

        for (i in 1..daysInMonth) {
            val date = currentMonth.withDayOfMonth(i)
            calendarDays.add(CalendarDay(date, "", i.toString(), date == selectedDate, hasTask(date)))
        }

        val nextMonth = currentMonth.plusMonths(1)
        var nextDay = 1
        val totalCells = if (calendarDays.size > 35) 42 else 35
        while (calendarDays.size < totalCells) {
            val date = nextMonth.withDayOfMonth(nextDay)
            calendarDays.add(CalendarDay(date, "", nextDay.toString(), date == selectedDate, hasTask(date)))
            nextDay++
        }

        calendarGridAdapter.updateData(calendarDays, currentMonth)
    }

    private fun hasTask(date: LocalDate): Boolean {
        return allTasks.any { it.date == date }
    }

    private fun refreshTasks() {
        val today = LocalDate.now()
        val pickedDate = selectedDate?.let {
            val monthStr = it.month.name.lowercase(Locale.getDefault())
                .replaceFirstChar { char -> char.uppercase() }
                .take(3)
            val dayStr = String.format(Locale.getDefault(), "%02d", it.dayOfMonth)
            val yearStr = it.year
            "$monthStr $dayStr, $yearStr"
        }
        val label = if (selectedDate == today) "Tasks for Today" else "Tasks for $pickedDate"
        tvSelectedDateLabel.text = label

        // Lọc ra các task của ngày đang chọn
        val filteredTasks = allTasks.filter { it.date == selectedDate }
        updateTaskDisplay(filteredTasks)
    }

    private fun updateTaskDisplay(tasks: List<Task>) {
        val timelineGroups = generateTimelineGroups(tasks)
        timelineHourAdapter.updateData(timelineGroups)

        // Toàn bộ 24 giờ luôn luôn hiển thị khung trục dọc, nên chỉ hiện rỗng nếu ko có gì xử lý hoặc tùy ý bro
        // Ở đây để tối ưu: Nếu trong ngày đó hoàn toàn KHÔNG CÓ BẤT KỲ TASK NÀO ở bất kỳ giờ nào, ta hiện màn hình Blank
        val hasAnyTaskInDay = tasks.isNotEmpty()
        rvTasks.visibility = if (hasAnyTaskInDay) View.VISIBLE else View.GONE
        blank.visibility = if (hasAnyTaskInDay) View.GONE else View.VISIBLE
    }

    /**
     * Hàm cốt lõi: Tự động sinh ra 24 khung giờ trong ngày (hoặc từ 06:00 AM đến 11:00 PM tùy chọn)
     * Sau đó găm các Task có giờ tương ứng vào đúng nhóm.
     */
    private fun generateTimelineGroups(tasks: List<Task>): List<HourTimeline> {
        val list = mutableListOf<HourTimeline>()

        // 0. Xử lý các task không có giờ (All day)
        val allDayTasks = tasks.filter { it.time == null }
        if (allDayTasks.isNotEmpty()) {
            list.add(HourTimeline("All day", null, allDayTasks))
        }

        // 1. Trích xuất ra danh sách các số giờ duy nhất xuất hiện trong ngày đó và sắp xếp tăng dần
        // Ví dụ: Hôm nay có task lúc 9:15, 9:45 và 15:30 -> Lấy ra được list: [9, 15]
        val activeHours = tasks.mapNotNull { it.time?.hour }
            .distinct()
            .sorted()

        // 2. Chỉ lặp qua những khung giờ thực sự có task
        for (h in activeHours) {
            val startLocalTime = LocalTime.of(h, 0)

            // Định dạng chuỗi hiển thị 12 giờ AM/PM (Ví dụ: 09:00 AM, 03:00 PM)
            val amPm = if (h >= 12) "PM" else "AM"
            val displayHour = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            val hourText = String.format("%02d:00 %s", displayHour, amPm)

            // Lọc ra các task con thuộc khung giờ này (Ví dụ: từ h:00 đến h:59)
            val tasksInThisHour = tasks.filter { task ->
                task.time?.hour == h
            }

            list.add(HourTimeline(hourText, startLocalTime, tasksInThisHour))
        }

        return list
    }
}