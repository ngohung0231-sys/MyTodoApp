package com.hungday.mytodoapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.activity.MainActivity
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit

class TodoWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_FILTER_TODAY = "com.hungday.mytodoapp.ACTION_FILTER_TODAY"
        const val ACTION_FILTER_UPCOMING = "com.hungday.mytodoapp.ACTION_FILTER_UPCOMING"
        const val ACTION_CLICK_TASK = "com.hungday.mytodoapp.ACTION_CLICK_TASK"
        const val EXTRA_FILTER = "extra_filter"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val FILTER_TODAY = "today"
        const val FILTER_UPCOMING = "upcoming"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val appPrefs = context.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
            val isPinkTheme = appPrefs.getBoolean("IS_PINK_THEME", false)
            val themeColor = if (isPinkTheme) context.getColor(R.color.pink) else context.getColor(R.color.blue)

            val appLocales = AppCompatDelegate.getApplicationLocales()
            val localizedContext = if (!appLocales.isEmpty) {
                val locale = appLocales.get(0)
                val config = android.content.res.Configuration(context.resources.configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config)
            } else {
                context
            }

            val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
            val filter = prefs.getString("filter_$appWidgetId", FILTER_TODAY) ?: FILTER_TODAY

            val views = RemoteViews(context.packageName, R.layout.widget_todo)

            // Set up ListView
            val intent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_FILTER, filter)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.lv_tasks, intent)
            views.setEmptyView(R.id.lv_tasks, R.id.tv_empty)
            views.setTextViewText(R.id.tv_empty, localizedContext.getString(R.string.no_tasks_here))

            // Set up PendingIntent template for item clicks
            val clickIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_CLICK_TASK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val clickPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.lv_tasks, clickPendingIntent)

            // Update UI based on filter
            views.setTextViewText(R.id.tv_filter_today, localizedContext.getString(R.string.today))
            views.setTextViewText(R.id.tv_filter_upcoming, localizedContext.getString(R.string.upcoming))

            if (filter == FILTER_TODAY) {
                views.setTextColor(R.id.tv_filter_today, themeColor)
                views.setTextColor(R.id.tv_filter_upcoming, Color.BLACK)
            } else {
                views.setTextColor(R.id.tv_filter_today, Color.BLACK)
                views.setTextColor(R.id.tv_filter_upcoming, themeColor)
            }

            // Filter button intents
            val todayIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_FILTER_TODAY
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val todayPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 2, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_filter_today, todayPendingIntent)

            val upcomingIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_FILTER_UPCOMING
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val upcomingPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 2 + 1, upcomingIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.tv_filter_upcoming, upcomingPendingIntent)

            // Add Task button intent (Deep link)
            views.setInt(R.id.btn_add_task, "setColorFilter", themeColor)
            val addTaskIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mytodoapp://add_task")).apply {
                `package` = context.packageName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addTaskPendingIntent = PendingIntent.getActivity(
                context, 0, addTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_add_task, addTaskPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_tasks)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        
        when (intent.action) {
            ACTION_FILTER_TODAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
                    prefs.edit { putString("filter_$appWidgetId", FILTER_TODAY) }
                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
            }
            ACTION_FILTER_UPCOMING -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
                    prefs.edit { putString("filter_$appWidgetId", FILTER_UPCOMING) }
                    updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
            }
            ACTION_CLICK_TASK -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                if (taskId != -1) {
                    val database = TodoDatabase.getDatabase(context)
                    val repository = TodoRepository(database.todoDao(), database.trashDao(), context)
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.updateTaskStatus(taskId, true)
                        // Note: updateTaskStatus calls notifyWidgetDataChanged internally, 
                        // which triggers updateAppWidget for all widgets.
                    }
                }
            }
        }
    }
}