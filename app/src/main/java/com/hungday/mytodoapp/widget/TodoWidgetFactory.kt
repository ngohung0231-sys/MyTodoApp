package com.hungday.mytodoapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import androidx.core.graphics.toColorInt

class TodoWidgetFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks = listOf<Task>()
    private var folders = listOf<Folder>()
    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var filter = TodoWidgetProvider.FILTER_TODAY

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Load data from DB synchronously
        val database = TodoDatabase.getDatabase(context)
        val allTasksFromDb = runBlocking {
            database.todoDao().getAllTasksSync()
        }
        val allFoldersFromDb = runBlocking {
            database.todoDao().getAllFolders().first()
        }
        folders = allFoldersFromDb

        // Get current filter from prefs
        val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
        filter = prefs.getString("filter_$appWidgetId", TodoWidgetProvider.FILTER_TODAY) ?: TodoWidgetProvider.FILTER_TODAY

        val today = LocalDate.now()
        tasks = when (filter) {
            TodoWidgetProvider.FILTER_TODAY -> {
                allTasksFromDb.filter { it.date == null || it.date == today }
            }
            TodoWidgetProvider.FILTER_UPCOMING -> {
                allTasksFromDb.filter { it.date != null && it.date.isAfter(today) }
            }
            else -> allTasksFromDb
        }
    }

    override fun onDestroy() {
        tasks = listOf()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) return RemoteViews(context.packageName, R.layout.item_widget_task)

        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.item_widget_task)

        val appPrefs = context.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val isPinkTheme = appPrefs.getBoolean("IS_PINK_THEME", false)
        val themeColor = if (isPinkTheme) context.getColor(R.color.pink) else context.getColor(R.color.blue)

        views.setTextViewText(R.id.tv_task_title, task.title)
        
        // Show folder name to help identify tasks
        val folder = folders.find { it.folderId == task.folderId }
        if (folder != null) {
            views.setTextViewText(R.id.tv_folder_name, folder.folderName)
            views.setViewVisibility(R.id.tv_folder_name, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.tv_folder_name, View.GONE)
        }

        // Color status circle (using theme color if task is completed, or hollow otherwise)
        if (task.isCompleted) {
            views.setImageViewResource(R.id.iv_status, R.drawable.ic_done)
            views.setInt(R.id.iv_status, "setColorFilter", themeColor)
        } else {
            views.setImageViewResource(R.id.iv_status, R.drawable.ic_circle_hollow)
            views.setInt(R.id.iv_status, "setColorFilter", android.graphics.Color.parseColor("#CCCCCC"))
        }

        // Set Priority indicator color
        val priorityColor = when (task.priority) {
            "High" -> "#EE4D5E".toColorInt() // Red
            "Medium" -> "#4997CF".toColorInt() // Blue
            else -> "#44BE65".toColorInt() // Green (Low)
        }
        views.setInt(R.id.view_priority_indicator, "setBackgroundColor", priorityColor)
        
        // Show date/time if available
        val timeText = when {
            !task.timeStr.isNullOrEmpty() -> task.timeStr
            task.date != null -> task.date.toString()
            else -> null
        }

        if (timeText != null) {
            views.setTextViewText(R.id.tv_task_time, timeText)
            views.setViewVisibility(R.id.tv_task_time, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.tv_task_time, View.GONE)
        }

        // Fill-in intent for marking task as completed
        // Only allow clicking if task is not yet completed (or we could toggle back, but app logic moves to trash)
        if (!task.isCompleted) {
            val fillInIntent = Intent().apply {
                putExtra(TodoWidgetProvider.EXTRA_TASK_ID, task.id)
            }
            // Ensure BOTH the circle and the text area are clickable
            views.setOnClickFillInIntent(R.id.iv_status, fillInIntent)
            views.setOnClickFillInIntent(R.id.tv_task_title, fillInIntent)
            views.setOnClickFillInIntent(R.id.lnl_task_item, fillInIntent)
        } else {
            // Task is already checked, wait for it to disappear
            views.setOnClickFillInIntent(R.id.lnl_task_item, Intent()) 
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = tasks[position].id.toLong()

    override fun hasStableIds(): Boolean = true
}