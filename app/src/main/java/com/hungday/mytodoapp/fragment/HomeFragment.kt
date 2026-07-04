package com.hungday.mytodoapp.fragment

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.model.Task
import androidx.core.net.toUri
import androidx.core.graphics.toColorInt
import android.graphics.BitmapFactory
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.hungday.mytodoapp.adapter.CalendarAdapter
import com.hungday.mytodoapp.adapter.FolderAdapter
import com.hungday.mytodoapp.adapter.FolderGroupAdapter
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import com.hungday.mytodoapp.model.CalendarDay
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.FolderWithTasks
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class HomeFragment : Fragment(R.layout.fragment_home) {
    // Database & Repository
    private lateinit var database: TodoDatabase
    private lateinit var repository: TodoRepository

    // Adapters
    private lateinit var folderGroupAdapter: FolderGroupAdapter
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var calendarAdapter: CalendarAdapter

    // UI Components
    private lateinit var rvFolderGroup: RecyclerView
    private lateinit var rvFolders: RecyclerView
    private lateinit var rvCalendar: RecyclerView
    private lateinit var blank: FrameLayout
    private lateinit var btnSetting: ImageView
    private lateinit var etSearchTask: EditText
    private lateinit var tvTabToday: TextView
    private lateinit var tvTabUpcoming: TextView
    private lateinit var tvUserName: TextView
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var tvSeeAllFolders: TextView
    private lateinit var tvSeeAllTasks: TextView
    private lateinit var lnlFolders: LinearLayout
    private lateinit var lnlFilter: LinearLayout

    // Data lists & Filter states
    private var calendarDays = mutableListOf<CalendarDay>()
    private var allTasks = mutableListOf<Task>()
    private var allFolders = mutableListOf<Folder>()
    private enum class FilterMode { TODAY, UPCOMING, CALENDAR }
    private var currentFilterMode = FilterMode.TODAY
    private var selectedCalendarDate: LocalDate = LocalDate.now()

    // Permission launcher for POST_NOTIFICATIONS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDatabase()
        initViews(view)
        setupInitialState()
        setupAdapters()
        observeData()
        setupListeners()
    }

    private fun initDatabase() {
        database = TodoDatabase.getDatabase(requireContext())
        repository = TodoRepository(database.todoDao())
    }

    private fun initViews(view: View) {
        rvFolderGroup = view.findViewById(R.id.rvFolderGroup)
        rvFolders = view.findViewById(R.id.rvFolders)
        rvCalendar = view.findViewById(R.id.rvCalendar)

        tvTabToday = view.findViewById(R.id.tvTabToday)
        tvTabUpcoming = view.findViewById(R.id.tvTabUpcoming)
        tvUserName = view.findViewById(R.id.tvUserName)
        imgAvatar = view.findViewById(R.id.imgAvatar)
        etSearchTask = view.findViewById(R.id.etSearchTask)

        tvSeeAllFolders = view.findViewById(R.id.tvSeeAllFolders)
        tvSeeAllTasks = view.findViewById(R.id.tvSeeAllTasks)
        lnlFolders = view.findViewById(R.id.lnlFolders)
        lnlFilter = view.findViewById(R.id.lnlFilter)
        blank = view.findViewById(R.id.blank)
        btnSetting = view.findViewById(R.id.btnSetting)
    }

    private fun setupInitialState() {
        val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", android.content.Context.MODE_PRIVATE)
        tvUserName.text = "Hello, ${sharedPref.getString("USER_NAME", "User")}!"
        sharedPref.getString("USER_AVATAR", null)?.let { loadAvatarSafely(imgAvatar, it) }

        // Khôi phục trạng thái Filter (Cách 1)
        val savedMode = sharedPref.getString("LAST_FILTER_MODE", FilterMode.TODAY.name)
        currentFilterMode = try {
            FilterMode.valueOf(savedMode ?: FilterMode.TODAY.name)
        } catch (e: Exception) {
            FilterMode.TODAY
        }

        // Nếu là CALENDAR thì tạm thời đưa về TODAY cho đơn giản khi quay lại màn hình
        if (currentFilterMode == FilterMode.CALENDAR) currentFilterMode = FilterMode.TODAY

        updateTabUI(currentFilterMode == FilterMode.TODAY)

        askNotificationPermission()
    }

    private fun setupAdapters() {
        // 1. Calendar Horizontal List
        generateCurrentWeek()
        calendarAdapter = CalendarAdapter(calendarDays) { selectedDay ->
            val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", android.content.Context.MODE_PRIVATE)
            currentFilterMode = FilterMode.CALENDAR
            sharedPref.edit().putString("LAST_FILTER_MODE", FilterMode.CALENDAR.name).apply()
            selectedCalendarDate = selectedDay.date
            refreshTasks()

            lnlFolders.visibility = View.VISIBLE
            lnlFilter.visibility = View.VISIBLE
            rvFolders.visibility = View.VISIBLE

            updateTabIndicator(selectedDay.date == LocalDate.now())
        }
        rvCalendar.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvCalendar.adapter = calendarAdapter

        // 2. Folder Horizontal List
        folderAdapter = FolderAdapter(allFolders) { folder ->
            val bundle = Bundle().apply {
                putInt("folderId", folder.folderId)
            }
            findNavController().navigate(R.id.action_homeFragment_to_folderDetailFragment, bundle)
        }
        rvFolders.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvFolders.adapter = folderAdapter

        // 3. Task Group Vertical List
        folderGroupAdapter = FolderGroupAdapter(emptyList(), { folder ->
            val bundle = Bundle().apply {
                putInt("folderId", folder.folderId)
            }
            findNavController().navigate(R.id.action_homeFragment_to_folderDetailFragment, bundle)
        }, { task ->
            val bundle = Bundle().apply {
                putInt("taskId", task.id)
            }
            findNavController().navigate(R.id.editTaskFragment, bundle)
        }, { task, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                repository.updateTaskStatus(task.id, isChecked)
            }
        })
        rvFolderGroup.layoutManager = LinearLayoutManager(requireContext())
        rvFolderGroup.adapter = folderGroupAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                repository.allFolders,
                repository.allTasks,
                repository.allLists
            ) { folders, tasks, lists ->
                Triple(folders, tasks, lists)
            }.collect { (folders, tasks, lists) ->
                allTasks = tasks.toMutableList()
                allFolders = folders.map { folder ->
                    folder.copy(
                        taskCount = tasks.count { it.folderId == folder.folderId },
                        listCount = lists.count { it.folderId == folder.folderId }
                    )
                }.toMutableList()

                generateCurrentWeek()
                if (::calendarAdapter.isInitialized) calendarAdapter.notifyDataSetChanged()
                folderAdapter.updateData(allFolders)
                refreshTasks()
            }
        }
    }

    private fun setupListeners() {
        val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", android.content.Context.MODE_PRIVATE)

        // Change fragment
        tvSeeAllFolders.setOnClickListener { findNavController().navigate(R.id.foldersFragment) }
        tvSeeAllTasks.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(findNavController().graph.startDestinationId, false, true)
                .build()
            findNavController().navigate(R.id.taskFragment, null, navOptions)
        }
        btnSetting.setOnClickListener { findNavController().navigate(R.id.settingFragment) }

        // filter today/Upcoming
        tvTabToday.setOnClickListener {
            updateTabUI(isToday = true)
            currentFilterMode = FilterMode.TODAY
            sharedPref.edit().putString("LAST_FILTER_MODE", FilterMode.TODAY.name).apply()
            refreshTasks()
            etSearchTask.clearFocus()
            calendarAdapter.selectToday()
            rvCalendar.smoothScrollToPosition(0)
        }

        tvTabUpcoming.setOnClickListener {
            updateTabUI(isToday = false)
            currentFilterMode = FilterMode.UPCOMING
            sharedPref.edit().putString("LAST_FILTER_MODE", FilterMode.UPCOMING.name).apply()
            refreshTasks()
            etSearchTask.clearFocus()
        }

        // Search task
        etSearchTask.setOnFocusChangeListener { _, hasFocus ->
            etSearchTask.setBackgroundResource(if (hasFocus) R.drawable.filter_task_bg else R.drawable.bg_search_task)
        }

        etSearchTask.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                refreshTasks()
                val isSearchEmpty = s.isNullOrEmpty()
                toggleExtraSectionsVisibility(isSearchEmpty)
            }
        })

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (etSearchTask.isFocused || etSearchTask.text.isNotEmpty()) {
                    etSearchTask.setText("")
                    etSearchTask.clearFocus()
                    toggleExtraSectionsVisibility(true)
                    val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(etSearchTask.windowToken, 0)
                    updateTaskDisplay(allTasks)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }

    //-------------------- Helper Functions --------------------//

    private fun updateTabUI(isToday: Boolean) {
        val activeTab = if (isToday) tvTabToday else tvTabUpcoming
        val inactiveTab = if (isToday) tvTabUpcoming else tvTabToday

        activeTab.setTextColor("#4a93ce".toColorInt())
        activeTab.setBackgroundResource(R.drawable.filter_task_bg)

        inactiveTab.setTextColor("#A0A0A0".toColorInt())
        inactiveTab.setBackgroundResource(android.R.color.transparent)
    }

    private fun updateTabIndicator(isToday: Boolean) {
        if (isToday) {
            tvTabToday.setTextColor("#4a93ce".toColorInt())
            tvTabToday.setBackgroundResource(R.drawable.filter_task_bg)
            tvTabUpcoming.setTextColor("#A0A0A0".toColorInt())
            tvTabUpcoming.setBackgroundResource(android.R.color.transparent)
        } else {
            tvTabUpcoming.setTextColor("#4a93ce".toColorInt())
            tvTabUpcoming.setBackgroundResource(R.drawable.filter_task_bg)
            tvTabToday.setTextColor("#A0A0A0".toColorInt())
            tvTabToday.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun toggleExtraSectionsVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        lnlFolders.visibility = visibility
        lnlFilter.visibility = visibility
        rvFolders.visibility = visibility
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadAvatarSafely(imageView: ImageView, uriString: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()
                val context = requireContext()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

                options.inSampleSize = calculateInSampleSize(options, 512, 512)
                options.inJustDecodeBounds = false

                val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

                withContext(Dispatchers.Main) {
                    if (bitmap != null) imageView.setImageBitmap(bitmap)
                    else imageView.setImageResource(R.drawable.icon)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { imageView.setImageResource(R.drawable.icon) }
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun refreshTasks() {
        val query = etSearchTask.text.toString().trim().lowercase()
        val filteredTasks = if (query.isNotEmpty()) {
            allTasks.filter { it.title.lowercase().contains(query) }
        } else {
            val today = LocalDate.now()
            when (currentFilterMode) {
                FilterMode.TODAY -> allTasks.filter { it.date == today || (it.dateStr.isNullOrEmpty() && it.timeStr.isNullOrEmpty()) }
                FilterMode.UPCOMING -> allTasks.filter { it.date.isAfter(today) || (it.dateStr.isNullOrEmpty() && it.timeStr.isNullOrEmpty()) }
                FilterMode.CALENDAR -> allTasks.filter { it.date == selectedCalendarDate }
            }
        }
        updateTaskDisplay(filteredTasks)
    }

    private fun updateTaskDisplay(tasks: List<Task>) {
        val groups = getFolderGroups(tasks)
        folderGroupAdapter.updateData(groups)
        rvFolderGroup.visibility = if (groups.isEmpty()) View.GONE else View.VISIBLE
        blank.visibility = if (groups.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun getFolderGroups(tasks: List<Task>): List<FolderWithTasks> {
        return allFolders.mapNotNull { folder ->
            val tasksInFolder = tasks.filter { it.folderId == folder.folderId }
            if (tasksInFolder.isNotEmpty()) FolderWithTasks(folder, tasksInFolder) else null
        }
    }

    private fun generateCurrentWeek() {
        calendarDays.clear()
        val today = LocalDate.now()
        val dayFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)
        for (i in 0..6) {
            val nextDay = today.plusDays(i.toLong())
            calendarDays.add(CalendarDay(
                date = nextDay,
                dayOfWeek = nextDay.format(dayFormatter),
                dayOfMonth = nextDay.dayOfMonth.toString(),
                isSelected = (i == 0),
                hasTask = allTasks.any { it.date == nextDay }
            ))
        }
    }

}
