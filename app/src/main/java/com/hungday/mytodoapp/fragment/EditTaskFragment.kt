package com.hungday.mytodoapp.fragment

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.adapter.FolderAddTaskAdapter
import com.hungday.mytodoapp.database.DateConverter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Task
import com.hungday.mytodoapp.receiver.NotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class EditTaskFragment : Fragment(R.layout.fragment_add_task) {
    private lateinit var repository: TodoRepository
    private lateinit var folderAddTaskAdapter: FolderAddTaskAdapter
    private lateinit var tvSelectedFolder: TextView

    private var taskId: Int = -1
    private var existingTask: Task? = null
    private var isDataLoading = false

    private var selectedPriority = "Low"
    private var selectedFolderId = 1
    private var selectedDate: LocalDate? = null
    private var selectedTime: String? = null
    private var selectedHour: Int = LocalTime.now().hour
    private var selectedMinute: Int = LocalTime.now().minute
    private var selectedReminderMinutes: Int? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Permission denied. Notifications will not show.", Toast.LENGTH_LONG).show()
        }
    }

    private var isPriorityExpanded = false
    private var isFolderExpanded = false
    private var isDateExpanded = false

    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var btnTrash: ImageView
    private lateinit var etTaskTitle: EditText
    private lateinit var rowSetPriority: LinearLayout
    private lateinit var rowSelectFolder: LinearLayout
    private lateinit var rowSetDate: LinearLayout
    private lateinit var rowAddNoti: LinearLayout
    private lateinit var expandableSetPriority: LinearLayout
    private lateinit var expandableSelectFolder: LinearLayout
    private lateinit var expandableSetDate: LinearLayout
    private lateinit var expandableAddNoti: LinearLayout
    private lateinit var chevronPriority: ImageView
    private lateinit var chevronFolder: ImageView
    private lateinit var chevronDate: ImageView
    private lateinit var btnLow: TextView
    private lateinit var btnMedium: TextView
    private lateinit var btnHigh: TextView
    private lateinit var tvSelectedPriority: TextView
    private lateinit var rvFolders: RecyclerView
    private lateinit var calendarView: CalendarView
    private lateinit var timePicker: TimePicker
    private lateinit var tvSelectedDate: TextView
    private lateinit var switchAddNoti: SwitchCompat
    private lateinit var tvNotiSwitch: TextView
    private lateinit var btnNotiAtTime: TextView
    private lateinit var btnNoti10Min: TextView
    private lateinit var btnNoti30Min: TextView
    private lateinit var btnNoti1Hour: TextView
    private lateinit var btnUpdateTask: Button

    // Repeat UI
    private lateinit var switchRepeat: SwitchCompat
    private lateinit var lnlRepeatOptions: LinearLayout
    private lateinit var btnRepeatDaily: TextView
    private lateinit var btnRepeatWeekly: TextView
    private lateinit var btnRepeatMonthly: TextView
    private lateinit var btnRepeatYearly: TextView
    private lateinit var lnlWeeklySelection: LinearLayout
    private lateinit var lnlDateSelectionSummary: LinearLayout
    private lateinit var tvWeeklySummary: TextView
    private lateinit var tvRepeatDateVal: TextView
    private val weekdayButtons = mutableListOf<TextView>()

    private var repeatType = "NONE"
    private var repeatValues: String? = null
    private val dayOfWeekNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val selectedDays = BooleanArray(7) { false }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupListeners()
        loadData()
    }

    private fun initDatabase() {
        taskId = arguments?.getInt("taskId") ?: -1
        val database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao(), requireContext())
    }

    private fun loadData() {
        if (taskId == -1) {
            findNavController().popBackStack()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            isDataLoading = true
            // 1. Lấy dữ liệu Task trước
            existingTask = repository.getTaskById(taskId)
            existingTask?.let { task ->
                etTaskTitle.setText(task.title)
                selectedPriority = task.priority
                tvSelectedPriority.text = selectedPriority
                updatePriorityUI(selectedPriority)

                selectedFolderId = task.folderId
                selectedDate = task.date
                
                task.date?.let {
                    val calendar = Calendar.getInstance()
                    calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
                    calendarView.date = calendar.timeInMillis
                }

                task.time?.let { time ->
                    selectedHour = time.hour
                    selectedMinute = time.minute
                    selectedTime = task.timeStr
                    timePicker.hour = selectedHour
                    timePicker.minute = selectedMinute
                }
                
                updateDateTimeSummary()
                updateNotificationRowState()

                if (task.isNotify != null) {
                    selectedReminderMinutes = task.isNotify
                    switchAddNoti.isChecked = true
                    expandableAddNoti.isVisible = true
                    tvNotiSwitch.text = getString(R.string.on)
                    when (selectedReminderMinutes) {
                        0 -> updateNotiSelection(btnNotiAtTime)
                        10 -> updateNotiSelection(btnNoti10Min)
                        30 -> updateNotiSelection(btnNoti30Min)
                        60 -> updateNotiSelection(btnNoti1Hour)
                    }
                }

                repeatType = task.repeatType
                repeatValues = task.repeatValues
                if (repeatType != "NONE") {
                    switchRepeat.isChecked = true
                    lnlRepeatOptions.isVisible = true
                    selectRepeatType(repeatType)
                    
                    if (repeatType == "WEEKLY" && !repeatValues.isNullOrEmpty()) {
                        repeatValues!!.split(",").forEach {
                            val index = it.toIntOrNull()
                            if (index != null && index in 0..6) {
                                selectedDays[index] = true
                                weekdayButtons[index].isSelected = true
                                weekdayButtons[index].setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            }
                        }
                        updateWeeklySummary()
                    }
                }
            }
            isDataLoading = false

            // 2. Sau khi đã có task (để lấy folderId), bắt đầu quan sát Folders
            repository.allFolders.collect { foldersList ->
                if (foldersList.isEmpty()) {
                    // Trong trường hợp hy hữu toàn bộ folder bị xóa khi đang edit task
                    findNavController().popBackStack()
                    return@collect
                }

                val ctx = context ?: return@collect
                rvFolders.layoutManager = LinearLayoutManager(ctx)
                
                // Nếu folder cũ của task đã bị xóa, tự động gán vào folder đầu tiên có sẵn
                if (foldersList.none { it.folderId == selectedFolderId }) {
                    selectedFolderId = foldersList[0].folderId
                }

                folderAddTaskAdapter = FolderAddTaskAdapter(foldersList) { selectedFolder ->
                    selectedFolderId = selectedFolder.folderId
                    tvSelectedFolder.text = selectedFolder.folderName
                    isFolderExpanded = false
                    toggleExpandableRow(view as ViewGroup, expandableSelectFolder, chevronFolder, isFolderExpanded)
                }
                rvFolders.adapter = folderAddTaskAdapter

                // Đồng bộ selection
                folderAddTaskAdapter.setSelectedFolder(selectedFolderId)
                foldersList.find { it.folderId == selectedFolderId }?.let {
                    tvSelectedFolder.text = it.folderName
                }
            }
        }
    }

    private fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tvTitle)
        tvTitle.text = getString(R.string.edit_task)
        
        btnBack = view.findViewById(R.id.btnBack)
        btnTrash = view.findViewById(R.id.btnTrash)
        btnTrash.isVisible = taskId != -1
        
        etTaskTitle = view.findViewById(R.id.etTaskTitle)
        rowSetPriority = view.findViewById(R.id.rowSetPriority)
        rowSelectFolder = view.findViewById(R.id.rowSelectFolder)
        rowSetDate = view.findViewById(R.id.rowSetDate)
        rowAddNoti = view.findViewById(R.id.rowAddNoti)
        expandableSetPriority = view.findViewById(R.id.expandableSetPriority)
        expandableSelectFolder = view.findViewById(R.id.expandableSelectFolder)
        expandableSetDate = view.findViewById(R.id.expandableSetDate)
        expandableAddNoti = view.findViewById(R.id.expandableAddNoti)
        chevronPriority = view.findViewById(R.id.chevronPriority)
        chevronFolder = view.findViewById(R.id.chevronFolder)
        chevronDate = view.findViewById(R.id.chevronDate)
        btnLow = view.findViewById(R.id.btnLow)
        btnMedium = view.findViewById(R.id.btnMedium)
        btnHigh = view.findViewById(R.id.btnHigh)
        tvSelectedPriority = view.findViewById(R.id.tvSelectedPriority)
        tvSelectedFolder = view.findViewById(R.id.tvSelectedFolder)
        rvFolders = view.findViewById(R.id.rvFolders)
        calendarView = view.findViewById(R.id.calendarView)
        timePicker = view.findViewById(R.id.timePicker)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        switchAddNoti = view.findViewById(R.id.switchAddNoti)
        tvNotiSwitch = view.findViewById(R.id.tvNotiSwitch)
        btnNotiAtTime = view.findViewById(R.id.btnNotiAtTime)
        btnNoti10Min = view.findViewById(R.id.btnNoti10Min)
        btnNoti30Min = view.findViewById(R.id.btnNoti30Min)
        btnNoti1Hour = view.findViewById(R.id.btnNoti1Hour)
        btnUpdateTask = view.findViewById(R.id.btnAddTask)
        btnUpdateTask.text = getString(R.string.update_task)

        // Initialize Repeat UI
        switchRepeat = view.findViewById(R.id.switchRepeat)
        lnlRepeatOptions = view.findViewById(R.id.lnlRepeatOptions)
        btnRepeatDaily = view.findViewById(R.id.btnRepeatDaily)
        btnRepeatWeekly = view.findViewById(R.id.btnRepeatWeekly)
        btnRepeatMonthly = view.findViewById(R.id.btnRepeatMonthly)
        btnRepeatYearly = view.findViewById(R.id.btnRepeatYearly)
        lnlWeeklySelection = view.findViewById(R.id.lnlWeeklySelection)
        lnlDateSelectionSummary = view.findViewById(R.id.lnlDateSelectionSummary)
        tvWeeklySummary = view.findViewById(R.id.tvWeeklySummary)
        tvRepeatDateVal = view.findViewById(R.id.tvRepeatDateVal)

        weekdayButtons.clear()
        weekdayButtons.add(view.findViewById(R.id.day2))
        weekdayButtons.add(view.findViewById(R.id.day3))
        weekdayButtons.add(view.findViewById(R.id.day4))
        weekdayButtons.add(view.findViewById(R.id.day5))
        weekdayButtons.add(view.findViewById(R.id.day6))
        weekdayButtons.add(view.findViewById(R.id.day7))
        weekdayButtons.add(view.findViewById(R.id.dayCN))

        // Setup weekday buttons click listeners
        weekdayButtons.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                selectedDays[index] = !selectedDays[index]
                textView.isSelected = selectedDays[index]
                textView.setTextColor(if (selectedDays[index]) ContextCompat.getColor(requireContext(), R.color.white) else ContextCompat.getColor(requireContext(), R.color.black))
                updateWeeklySummary()
            }
        }

        expandableSetPriority.visibility = View.GONE
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { findNavController().popBackStack() }

        btnTrash.setOnClickListener {
            showDeleteConfirmDialog()
        }

        btnLow.setOnClickListener { handlePrioritySelection("Low", btnLow, R.color.green) }
        btnMedium.setOnClickListener { handlePrioritySelection("Medium", btnMedium, R.color.blue) }
        btnHigh.setOnClickListener { handlePrioritySelection("High", btnHigh, R.color.red) }

        rowSetPriority.setOnClickListener {
            isPriorityExpanded = !isPriorityExpanded
            toggleExpandableRow(view as ViewGroup, expandableSetPriority, chevronPriority, isPriorityExpanded)
        }

        rowSelectFolder.setOnClickListener {
            isFolderExpanded = !isFolderExpanded
            toggleExpandableRow(view as ViewGroup, expandableSelectFolder, chevronFolder, isFolderExpanded)
        }

        rowSetDate.setOnClickListener {
            isDateExpanded = !isDateExpanded
            toggleExpandableRow(view as ViewGroup, expandableSetDate, chevronDate, isDateExpanded)
        }

        switchAddNoti.setOnCheckedChangeListener { _, isChecked ->
            expandableAddNoti.isVisible = isChecked
            tvNotiSwitch.text = if (isChecked) getString(R.string.on) else getString(R.string.off)
            if (isChecked) {
                if (selectedReminderMinutes == null) selectedReminderMinutes = 0
                when (selectedReminderMinutes) {
                    0 -> updateNotiSelection(btnNotiAtTime)
                    10 -> updateNotiSelection(btnNoti10Min)
                    30 -> updateNotiSelection(btnNoti30Min)
                    60 -> updateNotiSelection(btnNoti1Hour)
                }
            } else {
                selectedReminderMinutes = null
                clearNotiSelection()
            }
        }

        btnNotiAtTime.setOnClickListener { updateNotiState(0, btnNotiAtTime) }
        btnNoti10Min.setOnClickListener { updateNotiState(10, btnNoti10Min) }
        btnNoti30Min.setOnClickListener { updateNotiState(30, btnNoti30Min) }
        btnNoti1Hour.setOnClickListener { updateNotiState(60, btnNoti1Hour) }

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            if (isDataLoading) return@setOnDateChangeListener
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            updateDateTimeSummary()
            updateNotificationRowState()
        }

        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            if (isDataLoading) return@setOnTimeChangedListener
            selectedHour = hourOfDay
            selectedMinute = minute
            val isPm = hourOfDay >= 12
            val hour12 = when {
                hourOfDay == 0 -> 12
                hourOfDay > 12 -> hourOfDay - 12
                else -> hourOfDay
            }
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, if (isPm) "PM" else "AM")
            updateDateTimeSummary()
            updateNotificationRowState()
        }

        switchRepeat.setOnCheckedChangeListener { _, isChecked ->
            lnlRepeatOptions.isVisible = isChecked
            if (!isChecked) {
                repeatType = "NONE"
                repeatValues = null
                resetRepeatUI()
            } else {
                if (repeatType == "NONE") selectRepeatType("DAILY")
            }
        }

        btnRepeatDaily.setOnClickListener { selectRepeatType("DAILY") }
        btnRepeatWeekly.setOnClickListener { selectRepeatType("WEEKLY") }
        btnRepeatMonthly.setOnClickListener { selectRepeatType("MONTHLY") }
        btnRepeatYearly.setOnClickListener { selectRepeatType("YEARLY") }

        btnUpdateTask.setOnClickListener {
            val taskTitle = etTaskTitle.text.toString().trim()
            if (taskTitle.isEmpty()) {
                etTaskTitle.error = getString(R.string.title_empty_error)
                return@setOnClickListener
            }

            val finalDate = selectedDate ?: existingTask?.date
            val isUpcoming = finalDate?.isAfter(LocalDate.now()) ?: false

            val dateText = finalDate?.let {
                val monthStr = it.month.name.lowercase(Locale.getDefault())
                    .replaceFirstChar { char -> char.uppercase() }
                    .take(3)
                val dayStr = String.format(Locale.getDefault(), "%02d", it.dayOfMonth)
                "$monthStr $dayStr"
            }

            val updatedTask = existingTask?.copy(
                title = taskTitle,
                time = if (selectedTime != null) LocalTime.of(selectedHour, selectedMinute) else (existingTask?.time),
                timeStr = if (selectedTime != null) selectedTime else (existingTask?.timeStr),
                priority = selectedPriority,
                isUpcoming = isUpcoming,
                folderId = selectedFolderId,
                isNotify = selectedReminderMinutes,
                date = finalDate,
                dateStr = dateText ?: existingTask?.dateStr,
                repeatType = repeatType,
                repeatValues = repeatValues
            )

            updatedTask?.let { task ->
                val appContext = context?.applicationContext ?: return@setOnClickListener
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    repository.updateTask(task)
                    if (selectedReminderMinutes != null && finalDate != null) {
                        scheduleNotification(appContext, task.id, taskTitle, finalDate, selectedHour, selectedMinute, selectedReminderMinutes!!)
                    }
                    withContext(Dispatchers.Main) {
                        if (isAdded) findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_confirm_delete_folder, null)
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDelete)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirmDelete)

        tvTitle.text = getString(R.string.delete_task_q)
        tvMessage.text = getString(R.string.delete_task_msg)

        btnCancel.setOnClickListener { alertDialog.dismiss() }
        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            existingTask?.let { task ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    repository.moveTaskToTrash(task)
                    withContext(Dispatchers.Main) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
        alertDialog.show()
    }

    private fun handlePrioritySelection(priority: String, clickedButton: TextView, colorRes: Int) {
        selectedPriority = priority
        tvSelectedPriority.text = selectedPriority
        updatePriorityUI(priority)
    }

    private fun updatePriorityUI(priority: String) {
        val ctx = context ?: return
        val buttons = listOf(btnLow to "Low", btnMedium to "Medium", btnHigh to "High")
        val colors = mapOf("Low" to R.color.green, "Medium" to R.color.blue, "High" to R.color.red)

        buttons.forEach { (btn, p) ->
            if (p == priority) {
                btn.isSelected = true
                btn.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            } else {
                btn.isSelected = false
                btn.setTextColor(ContextCompat.getColor(ctx, colors[p]!!))
            }
        }
    }

    private fun updateNotiState(minutes: Int, selectedView: TextView) {
        selectedReminderMinutes = minutes
        updateNotiSelection(selectedView)
    }

    private fun updateNotiSelection(selectedView: TextView) {
        val ctx = context ?: return
        val buttons = listOf(btnNotiAtTime, btnNoti10Min, btnNoti30Min, btnNoti1Hour)
        buttons.forEach { btn ->
            if (btn == selectedView) {
                btn.isSelected = true
                btn.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            } else {
                btn.isSelected = false
                btn.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            }
        }
    }

    private fun clearNotiSelection() {
        val ctx = context ?: return
        listOf(btnNotiAtTime, btnNoti10Min, btnNoti30Min, btnNoti1Hour).forEach {
            it.isSelected = false
            it.setTextColor(ContextCompat.getColor(ctx, R.color.black))
        }
    }

    private fun toggleExpandableRow(rootView: ViewGroup, layout: View, chevron: ImageView, isExpanded: Boolean) {
        val transition = TransitionSet().addTransition(ChangeBounds()).setDuration(300)
        TransitionManager.beginDelayedTransition(rootView, transition)
        layout.isVisible = isExpanded
        chevron.animate().rotation(if (isExpanded) 180f else 0f).setDuration(250).start()
    }

    private fun updateDateTimeSummary() {
        val dateStr = selectedDate?.let {
            val monthStr = it.month.name.lowercase(Locale.getDefault())
                .replaceFirstChar { char -> char.uppercase() }
                .take(3)
            val dayStr = String.format(Locale.getDefault(), "%02d", it.dayOfMonth)
            "$monthStr $dayStr"
        } ?: ""
        val timeStr = selectedTime ?: ""
        tvSelectedDate.text = if (timeStr.isNotEmpty()) "$dateStr, $timeStr" else dateStr
    }

    private fun updateNotificationRowState() {
        val isDateTimeSelected = selectedDate != null && selectedTime != null
        rowAddNoti.isEnabled = isDateTimeSelected
        rowAddNoti.alpha = if (isDateTimeSelected) 1.0f else 0.5f
        switchAddNoti.isEnabled = isDateTimeSelected
        if (!isDateTimeSelected && switchAddNoti.isChecked) {
            switchAddNoti.isChecked = false
        }
    }

    private fun scheduleNotification(context: Context, taskId: Int, title: String, date: LocalDate, hour: Int, minute: Int, reminderMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = Calendar.getInstance().apply {
            set(date.year, date.monthValue - 1, date.dayOfMonth, hour, minute, 0)
            add(Calendar.MINUTE, -reminderMinutes)
        }
        if (calendar.timeInMillis > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        }
    }

    private fun selectRepeatType(type: String) {
        repeatType = type
        resetRepeatUI()
        val themeColor = if (requireActivity().getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE).getBoolean("IS_PINK_THEME", false)) R.color.pink else R.color.blue
        
        when (type) {
            "DAILY" -> {
                btnRepeatDaily.isSelected = true
                btnRepeatDaily.setTextColor(ContextCompat.getColor(requireContext(), themeColor))
            }
            "WEEKLY" -> {
                btnRepeatWeekly.isSelected = true
                btnRepeatWeekly.setTextColor(ContextCompat.getColor(requireContext(), themeColor))
                lnlWeeklySelection.isVisible = true
                updateWeeklySummary()
            }
            "MONTHLY" -> {
                btnRepeatMonthly.isSelected = true
                btnRepeatMonthly.setTextColor(ContextCompat.getColor(requireContext(), themeColor))
                lnlDateSelectionSummary.isVisible = true
                updateRepeatDateSummary()
            }
            "YEARLY" -> {
                btnRepeatYearly.isSelected = true
                btnRepeatYearly.setTextColor(ContextCompat.getColor(requireContext(), themeColor))
                lnlDateSelectionSummary.isVisible = true
                updateRepeatDateSummary()
            }
        }
    }

    private fun resetRepeatUI() {
        val buttons = listOf(btnRepeatDaily, btnRepeatWeekly, btnRepeatMonthly, btnRepeatYearly)
        buttons.forEach {
            it.isSelected = false
            it.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        }
        lnlWeeklySelection.isVisible = false
        lnlDateSelectionSummary.isVisible = false
    }

    private fun updateWeeklySummary() {
        val selectedIndices = mutableListOf<Int>()
        selectedDays.forEachIndexed { index, b -> if (b) selectedIndices.add(index) }
        repeatValues = if (selectedIndices.isEmpty()) null else selectedIndices.joinToString(",")
        
        if (selectedIndices.isEmpty()) {
            tvWeeklySummary.text = getString(R.string.every_week_on)
        } else {
            val names = selectedIndices.map { dayOfWeekNames[it] }.joinToString(", ")
            tvWeeklySummary.text = getString(R.string.every_format, names)
        }
    }

    private fun updateRepeatDateSummary() {
        val date = selectedDate ?: LocalDate.now()
        repeatValues = if (repeatType == "MONTHLY") date.dayOfMonth.toString() 
                       else "${date.dayOfMonth},${date.monthValue}"
        
        tvRepeatDateVal.text = if (repeatType == "MONTHLY") getString(R.string.day_monthly_format, date.dayOfMonth)
                               else getString(R.string.day_yearly_format, date.dayOfMonth, date.monthValue)
    }
}
