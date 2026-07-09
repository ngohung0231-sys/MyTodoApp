package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.database.DateConverter
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.Task
import java.time.LocalDate
import java.util.Locale

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hungday.mytodoapp.adapter.FolderAddTaskAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.hungday.mytodoapp.receiver.NotificationReceiver
import java.util.Calendar
import android.Manifest

class AddTaskFragment : Fragment(R.layout.fragment_add_task) {
    // Khai báo cho database, RVFolders
    private lateinit var repository: TodoRepository
    private lateinit var folderAddTaskAdapter: FolderAddTaskAdapter
    private lateinit var tvSelectedFolder: TextView

    // Biến lưu trữ tạm thời
    private var selectedPriority = "Low"
    private var selectedFolderId = 1
    private var selectedDate: LocalDate? = null
    private var selectedTime: String? = null
    private var selectedHour: Int = LocalTime.now().hour
    private var selectedMinute: Int = LocalTime.now().minute
    private var selectedReminderMinutes: Int? = null

    // Biến cho tính năng lặp lại
    private var repeatType = "NONE"
    private var repeatValues: String? = null
    private val dayOfWeekNames by lazy {
        arrayOf(
            getString(R.string.mon), getString(R.string.tue), getString(R.string.wed),
            getString(R.string.thu), getString(R.string.fri), getString(R.string.sat), getString(R.string.sun)
        )
    }
    private val selectedDays = BooleanArray(7) // Để lưu trạng thái chọn các thứ trong tuần

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Permission denied. Notifications will not show.", Toast.LENGTH_LONG).show()
        }
    }

    // Biến trạng thái đóng/mở sập gụ
    private var isPriorityExpanded = true
    private var isFolderExpanded = false
    private var isDateExpanded = false

    // Các thành phần UI
    private lateinit var btnBack: ImageView
    private lateinit var etTaskTitle: EditText

    // Các cụm hàng bấm (Rows)
    private lateinit var rowSetPriority: LinearLayout
    private lateinit var rowSelectFolder: LinearLayout
    private lateinit var rowSetDate: LinearLayout
    private lateinit var rowAddNoti: LinearLayout

    // Các vùng mở rộng (Expanders)
    private lateinit var expandableSetPriority: LinearLayout
    private lateinit var expandableSelectFolder: LinearLayout
    private lateinit var expandableSetDate: LinearLayout
    private lateinit var expandableAddNoti: LinearLayout

    // Các mũi tên (Chevrons)
    private lateinit var chevronPriority: ImageView
    private lateinit var chevronFolder: ImageView
    private lateinit var chevronDate: ImageView

    // Priority Buttons & Text
    private lateinit var btnLow: TextView
    private lateinit var btnMedium: TextView
    private lateinit var btnHigh: TextView
    private lateinit var tvSelectedPriority: TextView

    // Select Folders
    private lateinit var rvFolders: RecyclerView
    // Set Date & Time
    private lateinit var calendarView: CalendarView
    private lateinit var timePicker: TimePicker
    private lateinit var tvSelectedDate: TextView

    // Set Notification
    private lateinit var switchAddTask: SwitchCompat
    private lateinit var tvNotiSwitch: TextView
    private lateinit var btnNotiAtTime: TextView
    private lateinit var btnNoti10Min: TextView
    private lateinit var btnNoti30Min: TextView
    private lateinit var btnNoti1Hour: TextView

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

    // Button Add Task
    private lateinit var btnAddTask: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupFolderList()
        setupListeners()
    }

    private fun initDatabase() {
        val database = com.hungday.mytodoapp.database.TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao(), database.trashDao(), requireContext())
        
        // Nhận folderId từ arguments nếu có (khi được gọi từ FolderDetailFragment)
        selectedFolderId = arguments?.getInt("folderId") ?: 1
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
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

        switchAddTask = view.findViewById(R.id.switchAddNoti)
        tvNotiSwitch = view.findViewById(R.id.tvNotiSwitch)
        btnNotiAtTime = view.findViewById(R.id.btnNotiAtTime)
        btnNoti10Min = view.findViewById(R.id.btnNoti10Min)
        btnNoti30Min = view.findViewById(R.id.btnNoti30Min)
        btnNoti1Hour = view.findViewById(R.id.btnNoti1Hour)

        // Repeat UI
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

        // Khởi tạo các nút thứ trong tuần
        val dayIds = arrayOf(R.id.day2, R.id.day3, R.id.day4, R.id.day5, R.id.day6, R.id.day7, R.id.dayCN)
        dayIds.forEach { id -> weekdayButtons.add(view.findViewById(id)) }

        btnAddTask = view.findViewById(R.id.btnAddTask)

        // Thiết lập trạng thái mặc định ban đầu
        btnLow.isSelected = true
        btnLow.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        // Khởi tạo trạng thái cho Notification (Vô hiệu hóa nếu chưa chọn Ngày/Giờ)
        updateNotificationRowState()
    }

    private fun setupFolderList() {
        // 1. Cấu hình RecyclerView rvFolders (Quản lý layout dọc/ngang)
        rvFolders.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        // 2. Lấy danh sách Folder từ Room DB đổ động vào Adapter
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allFolders.collect { foldersList ->
                if (foldersList.isEmpty()) {
                    showNoFolderDialog()
                    return@collect
                }

                // Kiểm tra xem selectedFolderId hiện tại có tồn tại trong list không
                val existingFolder = foldersList.find { it.folderId == selectedFolderId }
                if (existingFolder == null) {
                    // Nếu không tồn tại (đã bị xóa), chọn cái đầu tiên tìm thấy
                    selectedFolderId = foldersList[0].folderId
                }

                folderAddTaskAdapter = FolderAddTaskAdapter(foldersList) { selectedFolder ->
                    // Bước A: Gán ID folder vừa chọn vào biến tạm để lát insert DB
                    selectedFolderId = selectedFolder.folderId

                    // Bước B: Hiển thị tên Folder đã chọn lên thanh tiêu đề sập gụ
                    if (::tvSelectedFolder.isInitialized) {
                        tvSelectedFolder.text = selectedFolder.folderName
                    }

                    // Bước C: Tự động đóng sập gụ lại sau khi chọn xong
                    isFolderExpanded = false
                    toggleExpandableRow(
                        view as ViewGroup,
                        expandableSelectFolder,
                        chevronFolder,
                        isFolderExpanded
                    )
                }

                rvFolders.adapter = folderAddTaskAdapter
                
                // Cập nhật selection cho Adapter
                folderAddTaskAdapter.setSelectedFolder(selectedFolderId)
                foldersList.find { it.folderId == selectedFolderId }?.let {
                    tvSelectedFolder.text = it.folderName
                }
            }
        }
    }

    private fun showNoFolderDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_delete_folder, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDelete)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirmDelete)

        tvTitle.setText(R.string.no_folder_q)
        tvTitle.setTextColor(resources.getColor(R.color.blue, null))
        tvMessage.setText(R.string.no_folder_msg)

        btnCancel.setText(R.string.back)
        btnCancel.backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.red, null))
        btnConfirm.setText(R.string.create_folder_dialog)
        btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.blue, null))

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
            findNavController().popBackStack()
        }
        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            findNavController().navigate(R.id.addFolderFragment)
        }

        alertDialog.show()
    }

    private fun setupListeners() {
        // Nút Back trên Toolbar
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Đổi background động khi Ô nhập tên Task nhận/mất focus
        etTaskTitle.setOnFocusChangeListener { _, hasFocus ->
            val bgRes = if (hasFocus) R.drawable.filter_task_bg else R.drawable.bg_search_task
            etTaskTitle.setBackgroundResource(bgRes)
        }

        // Cấu hình chọn Độ ưu tiên (Priority Selection)
        btnLow.setOnClickListener { handlePrioritySelection("Low", btnLow, R.color.green) }
        btnMedium.setOnClickListener { handlePrioritySelection("Medium", btnMedium, R.color.blue) }
        btnHigh.setOnClickListener { handlePrioritySelection("High", btnHigh, R.color.red) }

        // Sập gụ Độ ưu tiên
        rowSetPriority.setOnClickListener {
            isPriorityExpanded = !isPriorityExpanded
            toggleExpandableRow(view as ViewGroup, expandableSetPriority, chevronPriority, isPriorityExpanded)
        }

        // Sập gụ Chọn Folder
        rowSelectFolder.setOnClickListener {
            isFolderExpanded = !isFolderExpanded
            toggleExpandableRow(view as ViewGroup, expandableSelectFolder, chevronFolder, isFolderExpanded)
        }

        // Sập gụ Cài đặt Ngày/Giờ
        rowSetDate.setOnClickListener {
            isDateExpanded = !isDateExpanded
            toggleExpandableRow(view as ViewGroup, expandableSetDate, chevronDate, isDateExpanded)
        }

        // Sự kiện click cả thanh hàng Thông báo
        rowAddNoti.setOnClickListener {
            if (rowAddNoti.isEnabled) {
                etTaskTitle.clearFocus()
                switchAddTask.isChecked = !switchAddTask.isChecked
            }
        }

        // Bắt sự kiện gạt công tắc thông báo (SwitchCompat)
        switchAddTask.setOnCheckedChangeListener { _, isChecked ->
            if (!rowAddNoti.isEnabled && isChecked) {
                switchAddTask.isChecked = false
                return@setOnCheckedChangeListener
            }
            etTaskTitle.clearFocus()

            val customTransition = TransitionSet().addTransition(ChangeBounds()).setDuration(300)
            TransitionManager.beginDelayedTransition(view as ViewGroup, customTransition)

            expandableAddNoti.isVisible = isChecked
            tvNotiSwitch.text = if (isChecked) getString(R.string.on) else getString(R.string.off)

            if (isChecked) {
                // Mặc định khi vừa bật lên thì chọn mốc đầu tiên (At time)
                selectedReminderMinutes = 0
                updateNotiSelection(btnNotiAtTime)
            } else {
                selectedReminderMinutes = null
                clearNotiSelection()
            }
        }

        // Chọn mốc thời gian nhắc nhở (Notification Time Options)
        btnNotiAtTime.setOnClickListener { updateNotiState(0, btnNotiAtTime) }
        btnNoti10Min.setOnClickListener { updateNotiState(10, btnNoti10Min) }
        btnNoti30Min.setOnClickListener { updateNotiState(30, btnNoti30Min) }
        btnNoti1Hour.setOnClickListener { updateNotiState(60, btnNoti1Hour) }

        // Logic cho tính năng Repeat
        switchRepeat.setOnCheckedChangeListener { _, isChecked ->
            TransitionManager.beginDelayedTransition(view as ViewGroup)
            lnlRepeatOptions.isVisible = isChecked
            if (!isChecked) {
                repeatType = "NONE"
                repeatValues = null
                resetRepeatUI()
            }
        }

        // Cấu hình chọn kiểu lặp chính (Grid 2x2)
        btnRepeatDaily.setOnClickListener { selectRepeatType("DAILY") }
        btnRepeatWeekly.setOnClickListener { selectRepeatType("WEEKLY") }
        btnRepeatMonthly.setOnClickListener { selectRepeatType("MONTHLY") }
        btnRepeatYearly.setOnClickListener { selectRepeatType("YEARLY") }

        // Cấu hình chọn các thứ trong tuần (Multi-select)
        weekdayButtons.forEachIndexed { index, btn ->
            btn.setOnClickListener {
                btn.isSelected = !btn.isSelected
                updateWeeklySummary()
            }
        }

        setupDateTimePickers()

        // Sự kiện thêm task
        btnAddTask.setOnClickListener {
            val taskTitle = etTaskTitle.text.toString().trim()

            // 1. Kiểm tra tiêu đề có bị trống không
            if (taskTitle.isEmpty()) {
                etTaskTitle.error = "Title cannot be empty"
                etTaskTitle.requestFocus()
                return@setOnClickListener
            }

            // Check permission if user wants notification
            if (selectedReminderMinutes != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnClickListener
                    }
                }
            }

            val finalDate = selectedDate
            val isUpcoming = finalDate?.isAfter(LocalDate.now()) ?: false

            val dateText = finalDate?.let {
                val monthStr = it.month.name.lowercase(Locale.getDefault())
                    .replaceFirstChar { char -> char.uppercase() }
                    .take(3)
                val dayStr = String.format(Locale.getDefault(), "%02d", it.dayOfMonth)
                "$monthStr $dayStr"
            }

            val newTask = Task(
                title = taskTitle,
                time = if (selectedTime != null) LocalTime.of(selectedHour, selectedMinute) else null,
                timeStr = selectedTime,
                priority = selectedPriority,
                isCompleted = false,
                isUpcoming = isUpcoming,
                folderId = selectedFolderId,
                isNotify = selectedReminderMinutes,
                date = finalDate,
                dateStr = dateText,
                repeatType = repeatType,
                repeatValues = repeatValues
            )

            // 6. Đẩy dữ liệu xuống Room DB
            val appContext = context?.applicationContext ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val taskId = repository.insertTask(newTask)

                if (selectedReminderMinutes != null && finalDate != null) {
                    scheduleNotification(appContext, taskId.toInt(), taskTitle, finalDate, selectedHour, selectedMinute, selectedReminderMinutes!!)
                }

                withContext(Dispatchers.Main) {
                    if (isAdded) findNavController().popBackStack()
                }
            }
        }
    }

    private fun setupDateTimePickers() {
        Log.d("CalendarLog", "Selected Time: $selectedTime")
        Log.d("CalendarLog", "Selected Date: $selectedDate")
        // Đón đầu mốc Ngày khi người dùng bấm chọn trên lịch tháng
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            val dateString = DateConverter.dateToString(selectedDate)
            tvSelectedDate.text = dateString
            Log.d("CalendarLog", "Selected Date: $selectedDate")
            updateNotificationRowState()
        }

        // Đón đầu mốc Giờ khi người dùng cuộn đồng hồ
        timePicker.setOnTimeChangedListener { _, hourOfDay, minute ->
            selectedHour = hourOfDay
            selectedMinute = minute
            // Format 24h về chuỗi AM/PM sạch sẽ
            val isPm = hourOfDay >= 12
            val hour12 = when {
                hourOfDay == 0 -> 12
                hourOfDay > 12 -> hourOfDay - 12
                else -> hourOfDay
            }
            val amPmStr = if (isPm) "PM" else "AM"
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPmStr)
            Log.d("CalendarLog", "Selected Time: $selectedTime")

            if(selectedDate == null) {
                selectedDate = LocalDate.now()
                val dateString = DateConverter.dateToString(selectedDate)
                tvSelectedDate.text = dateString
            }
            updateNotificationRowState()
        }
    }

    private fun scheduleNotification(context: Context, taskId: Int, title: String, date: LocalDate, hour: Int, minute: Int, reminderMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("TASK_ID", taskId)
            putExtra("TASK_TITLE", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            add(Calendar.MINUTE, -reminderMinutes)
        }

        val triggerTime = calendar.timeInMillis
        if (triggerTime <= System.currentTimeMillis()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                // Optionally request SCHEDULE_EXACT_ALARM permission
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    //-------------------- Repeat Feature Logic --------------------//

    /**
     * Hàm chọn kiểu lặp chính và xử lý hiển thị vùng mở rộng
     */
    private fun selectRepeatType(type: String) {
        repeatType = type
        
        // Cập nhật trạng thái Selected của các nút Grid
        btnRepeatDaily.isSelected = (type == "DAILY")
        btnRepeatWeekly.isSelected = (type == "WEEKLY")
        btnRepeatMonthly.isSelected = (type == "MONTHLY")
        btnRepeatYearly.isSelected = (type == "YEARLY")

        // Hiển thị vùng mở rộng tương ứng bằng TransitionManager
        TransitionManager.beginDelayedTransition(view as ViewGroup)
        lnlWeeklySelection.isVisible = (type == "WEEKLY")
        lnlDateSelectionSummary.isVisible = (type == "MONTHLY" || type == "YEARLY")

        if (type == "MONTHLY" || type == "YEARLY") {
            updateRepeatDateSummary()
        }
    }

    private fun resetRepeatUI() {
        listOf(btnRepeatDaily, btnRepeatWeekly, btnRepeatMonthly, btnRepeatYearly).forEach {
            it.isSelected = false
        }
        lnlWeeklySelection.isVisible = false
        lnlDateSelectionSummary.isVisible = false
        weekdayButtons.forEach { 
            it.isSelected = false
        }
    }

    private fun updateWeeklySummary() {
        val selectedIndices = mutableListOf<Int>()
        val selectedNames = mutableListOf<String>()
        
        weekdayButtons.forEachIndexed { index, btn ->
            if (btn.isSelected) {
                selectedIndices.add(index + 1) // 1=T2, ..., 7=CN
                selectedNames.add(dayOfWeekNames[index])
            }
        }
        
        repeatValues = if (selectedIndices.isNotEmpty()) selectedIndices.joinToString(",") else null
        tvWeeklySummary.text = if (selectedNames.isNotEmpty()) getString(R.string.every_format, selectedNames.joinToString(", ")) 
                               else getString(R.string.every_week_on)
    }

    private fun updateRepeatDateSummary() {
        val date = selectedDate ?: LocalDate.now()
        repeatValues = if (repeatType == "MONTHLY") date.dayOfMonth.toString() 
                       else "${date.monthValue},${date.dayOfMonth}"
        
        tvRepeatDateVal.text = if (repeatType == "MONTHLY") getString(R.string.day_monthly_format, date.dayOfMonth)
                               else getString(R.string.day_yearly_format, date.dayOfMonth, date.monthValue)
    }

    // Xóa các hàm Dialog cũ không còn dùng

    //-------------------- Các hàm chức năng bổ trợ (Helper Functions) --------------------//

    /**
     * Hàm dùng chung xử lý đóng/mở sập gụ và xoay mũi tên mượt mà bằng Animation
     */
    private fun toggleExpandableRow(rootView: ViewGroup, expandableLayout: View, chevron: ImageView, isExpanded: Boolean) {
        etTaskTitle.clearFocus()

        val customTransition = TransitionSet().addTransition(ChangeBounds()).setDuration(300)
        TransitionManager.beginDelayedTransition(rootView, customTransition)

        expandableLayout.isVisible = isExpanded

        val targetRotation = if (isExpanded) 180f else 0f
        chevron.animate().rotation(targetRotation).setDuration(250).start()
    }

    /**
     * Tối ưu hóa logic nhuộm màu và nhả trạng thái cho cụm 3 nút Priority
     */
    private fun handlePrioritySelection(priority: String, clickedButton: TextView, selectedColorRes: Int) {
        val ctx = context ?: return
        etTaskTitle.clearFocus()
        selectedPriority = priority
        tvSelectedPriority.text = selectedPriority

        val priorityButtons = listOf(
            Pair(btnLow, R.color.green),
            Pair(btnMedium, R.color.blue),
            Pair(btnHigh, R.color.red)
        )

        priorityButtons.forEach { (btn, defaultColorRes) ->
            if (btn == clickedButton) {
                btn.isSelected = true
                btn.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            } else {
                btn.isSelected = false
                btn.setTextColor(ContextCompat.getColor(ctx, defaultColorRes))
            }
        }
    }

    /**
     * Cập nhật trạng thái của hàng Thông báo dựa trên việc đã chọn Ngày & Giờ hay chưa
     */
    private fun updateNotificationRowState() {
        // Điều kiện: Phải chọn cả Ngày và Giờ (selectedTime không null)
        val isDateTimeSelected = selectedDate != null && selectedTime != null
        
        rowAddNoti.isEnabled = isDateTimeSelected
        rowAddNoti.alpha = if (isDateTimeSelected) 1.0f else 0.5f
        switchAddTask.isEnabled = isDateTimeSelected
        
        // Nếu bị vô hiệu hóa mà đang bật thì tắt đi
        if (!isDateTimeSelected && switchAddTask.isChecked) {
            switchAddTask.isChecked = false
        }
    }

    /**
     * Cập nhật số phút nhắc nhở và giao diện nút tương ứng
     */
    private fun updateNotiState(minutes: Int, selectedView: TextView) {
        etTaskTitle.clearFocus()
        selectedReminderMinutes = minutes
        updateNotiSelection(selectedView)
    }

    private fun updateNotiSelection(selectedView: TextView) {
        val ctx = context ?: return
        val notiButtons = listOf(btnNotiAtTime, btnNoti10Min, btnNoti30Min, btnNoti1Hour)
        notiButtons.forEach { btn ->
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
        val notiButtons = listOf(btnNotiAtTime, btnNoti10Min, btnNoti30Min, btnNoti1Hour)
        notiButtons.forEach { btn ->
            btn.isSelected = false
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.black))
        }
    }

}