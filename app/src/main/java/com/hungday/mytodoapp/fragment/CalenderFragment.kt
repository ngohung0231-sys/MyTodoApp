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
import com.hungday.mytodoapp.adapter.FolderGroupAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.CalendarDay
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.FolderWithTasks
import com.hungday.mytodoapp.model.Task
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class CalenderFragment : Fragment(R.layout.fragment_calender) {
    private lateinit var database: TodoDatabase
    private lateinit var repository: TodoRepository

    private lateinit var calendarGridAdapter: CalendarGridAdapter
    private lateinit var folderGroupAdapter: FolderGroupAdapter

    private var allTasks = mutableListOf<Task>()
    private var allFolders = mutableListOf<Folder>()
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

        // Init Database
        database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao())

        // Init Views
        btnBack = view.findViewById(R.id.btnBack)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        rvCalendarGrid = view.findViewById(R.id.rvCalendarGrid)
        rvTasks = view.findViewById(R.id.rvTasks)
        tvSelectedDateLabel = view.findViewById(R.id.tvSelectedDateLabel)
        blank = view.findViewById(R.id.blank)

        // Setup Adapters
        setupCalendarAdapter()
        setupTaskAdapter()

        // Load Data
        loadData()

        // Listeners
        btnBack.setOnClickListener { findNavController().popBackStack() }
        btnPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateCalendar()
        }
        btnNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateCalendar()
        }

        updateCalendar()
    }

    private fun setupCalendarAdapter() {
        calendarGridAdapter = CalendarGridAdapter(calendarDays, currentMonth) { clickedDay ->
            selectedDate = clickedDay.date
            updateCalendar()
            refreshTasks()
        }
        rvCalendarGrid.layoutManager = GridLayoutManager(requireContext(), 7)
        rvCalendarGrid.adapter = calendarGridAdapter
    }

    private fun setupTaskAdapter() {
        folderGroupAdapter = FolderGroupAdapter(emptyList(), { folder ->
            // TODO: Navigate to folder
        }, { task, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                repository.updateTaskStatus(task.id, isChecked)
            }
        })
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = folderGroupAdapter
    }

    private fun loadData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allTasks.collect { tasks ->
                allTasks = tasks.toMutableList()
                updateCalendar()
                refreshTasks()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allFolders.collect { folders ->
                allFolders = folders.toMutableList()
                refreshTasks()
            }
        }
    }

    private fun updateCalendar() {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        tvMonthYear.text = currentMonth.format(formatter)

        calendarDays.clear()
        
        val yearMonth = YearMonth.from(currentMonth)
        val firstOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        
        // Find the first day of the week for the first day of the month
        // Adjusting to start from Monday (1) to Sunday (7)
        var dayOfWeek = firstOfMonth.dayOfWeek.value
        
        // Days from previous month to fill the first row
        val prevMonth = currentMonth.minusMonths(1)
        val prevYearMonth = YearMonth.from(prevMonth)
        val daysInPrevMonth = prevYearMonth.lengthOfMonth()
        
        for (i in dayOfWeek - 1 downTo 1) {
            val date = prevMonth.withDayOfMonth(daysInPrevMonth - i + 1)
            calendarDays.add(CalendarDay(date, "", date.dayOfMonth.toString(), date == selectedDate, hasTask(date)))
        }

        // Days of current month
        for (i in 1..daysInMonth) {
            val date = currentMonth.withDayOfMonth(i)
            calendarDays.add(CalendarDay(date, "", i.toString(), date == selectedDate, hasTask(date)))
        }

        // Days from next month to fill the grid (total 35 or 42 cells for consistent rows)
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
        val label = if (selectedDate == today) "Tasks for Today" else "Tasks for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}"
        tvSelectedDateLabel.text = label

        val filteredTasks = allTasks.filter { it.date == selectedDate }
        updateTaskDisplay(filteredTasks)
    }

    private fun updateTaskDisplay(tasks: List<Task>) {
        val groups = getFolderGroups(tasks)
        folderGroupAdapter.updateData(groups)
        rvTasks.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
        blank.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getFolderGroups(tasks: List<Task>): List<FolderWithTasks> {
        return allFolders.mapNotNull { folder ->
            val tasksInFolder = tasks.filter { it.folderId == folder.folderId }
            if (tasksInFolder.isNotEmpty()) FolderWithTasks(folder, tasksInFolder) else null
        }
    }
}
