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
        repository = TodoRepository(database.todoDao(), database.trashDao())
    }

    private fun loadData() {
        if (taskId == -1) {
            findNavController().popBackStack()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // 1. Lấy dữ liệu Task trước
            existingTask = repository.getTaskById(taskId)
            existingTask?.let { task ->
                etTaskTitle.setText(task.title)
                selectedPriority = task.priority
                tvSelectedPriority.text = selectedPriority
                updatePriorityUI(selectedPriority)

                selectedFolderId = task.folderId
                selectedDate = task.date
                tvSelectedDate.text = DateConverter.dateToString(selectedDate)

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
            }

            // 2. Sau khi đã có task (để lấy folderId), bắt đầu quan sát Folders
            repository.allFolders.collect { foldersList ->
                val ctx = context ?: return@collect
                rvFolders.layoutManager = LinearLayoutManager(ctx)
                folderAddTaskAdapter = FolderAddTaskAdapter(foldersList) { selectedFolder ->
                    selectedFolderId = selectedFolder.folderId
                    tvSelectedFolder.text = selectedFolder.folderName
                    isFolderExpanded = false
                    toggleExpandableRow(view as ViewGroup, expandableSelectFolder, chevronFolder, isFolderExpanded)
                }
                rvFolders.adapter = folderAddTaskAdapter

                // Đồng bộ selection ngay khi list folder xuất hiện
                val currentFolderId = selectedFolderId
                folderAddTaskAdapter.setSelectedFolder(currentFolderId)
                foldersList.find { it.folderId == currentFolderId }?.let {
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
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            tvSelectedDate.text = DateConverter.dateToString(selectedDate)
        }

        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
            val isPm = hourOfDay >= 12
            val hour12 = when {
                hourOfDay == 0 -> 12
                hourOfDay > 12 -> hourOfDay - 12
                else -> hourOfDay
            }
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, if (isPm) "PM" else "AM")
        }

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
                dateStr = dateText ?: existingTask?.dateStr
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
}
