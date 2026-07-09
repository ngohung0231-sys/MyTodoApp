package com.hungday.mytodoapp.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.activity.MainActivity
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.database.TodoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "task_reminder_channel"
        const val CHANNEL_NAME = "Task Reminders"
        const val ACTION_BIRTHDAY = "com.hungday.mytodoapp.ACTION_BIRTHDAY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BIRTHDAY) {
            showBirthdayNotification(context)
            rescheduleBirthdayNextYear(context)
            return
        }

        val notificationId = intent.getIntExtra("TASK_ID", 0)
        if (notificationId == 0) return

        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TodoDatabase.getDatabase(context)
                val task = db.todoDao().getTaskById(notificationId)

                // Validation: Only notify if task exists and is NOT completed
                if (task != null && !task.isCompleted) {
                    withContext(Dispatchers.Main) {
                        showTaskNotification(context, task.id, task.title)
                    }
                    
                    // Auto-forward recurring task data in DB to next occurrence
                    if (task.repeatType != "NONE") {
                        val nextDate = when (task.repeatType) {
                            "DAILY" -> task.date?.plusDays(1)
                            "WEEKLY" -> task.date?.plusWeeks(1)
                            "MONTHLY" -> task.date?.plusMonths(1)
                            "YEARLY" -> task.date?.plusYears(1)
                            else -> task.date?.plusDays(1)
                        }
                        val updatedTask = task.copy(date = nextDate)
                        db.todoDao().updateTask(updatedTask)
                        
                        // Notify UI/Widget that data changed
                        val repository = TodoRepository(db.todoDao(), db.trashDao(), context)
                        repository.updateTask(updatedTask) // This triggers widget update
                    }

                    // Reschedule the alarm for the next time
                    rescheduleTaskIfNeeded(context, task.id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showTaskNotification(context: Context, taskId: Int, taskTitle: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Task Reminders")
            .setContentText("It's time to $taskTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId, notification)
    }

    private fun rescheduleTaskIfNeeded(context: Context, taskId: Int) {
        if (taskId == 0) return
        
        CoroutineScope(Dispatchers.IO).launch {
            val db = TodoDatabase.getDatabase(context)
            val task = db.todoDao().getTaskById(taskId) ?: return@launch
            
            if (task.repeatType == "NONE") return@launch

            val now = LocalDateTime.now()
            var nextTrigger = LocalDateTime.of(task.date ?: LocalDate.now(), task.time ?: java.time.LocalTime.MIDNIGHT)
            
            // Adjust based on repeat type
            while (nextTrigger.isBefore(now) || nextTrigger.isEqual(now)) {
                nextTrigger = when (task.repeatType) {
                    "DAILY" -> nextTrigger.plusDays(1)
                    "WEEKLY" -> nextTrigger.plusWeeks(1)
                    "MONTHLY" -> nextTrigger.plusMonths(1)
                    "YEARLY" -> nextTrigger.plusYears(1)
                    else -> nextTrigger.plusDays(1)
                }
            }

            val triggerTime = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("TASK_ID", task.id)
                putExtra("TASK_TITLE", task.title)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, task.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelTaskAlarm(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun rescheduleBirthdayNextYear(context: Context) {
        val sharedPref = context.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val birthdateStr = sharedPref.getString("USER_BIRTHDAY", null) ?: return
        
        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birthdate = LocalDate.parse(birthdateStr, formatter)
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_BIRTHDAY
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 999, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val now = LocalDate.now()
            val nextBirthday = birthdate.withYear(now.year + 1)
            val triggerTime = nextBirthday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showBirthdayNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val activityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Happy Birthday!")
            .setContentText("HAPPY BIRTHDAY TO YOUUU!!! 😘🤩")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }

    private fun createChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
