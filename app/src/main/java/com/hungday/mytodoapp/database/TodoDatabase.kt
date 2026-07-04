package com.hungday.mytodoapp.database

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hungday.mytodoapp.model.Folder
import com.hungday.mytodoapp.model.Task
import com.hungday.mytodoapp.model.TodoList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Folder::class, Task::class, TodoList::class], version = 17, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun getDatabase(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                    .fallbackToDestructiveMigration() // Tự kích hoạt dọn dẹp cấu trúc bảng lỗi cũ
                    .addCallback(TodoDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class TodoDatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                initializeData(context)
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                initializeData(context)
            }
        }

        fun initializeData(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = getDatabase(context)
                    val todoDao = database.todoDao()

                    val folders = listOf(
                        Folder(folderName = "Others", folderImg = com.hungday.mytodoapp.R.drawable.ic_project, folderColor = Color.parseColor("#4997cf")),
                        Folder(folderName = "Personal", folderImg = com.hungday.mytodoapp.R.drawable.ic_profile, folderColor = Color.parseColor("#ee4d5e")),
                        Folder(folderName = "Exercise", folderImg = com.hungday.mytodoapp.R.drawable.ic_exercise, folderColor = Color.parseColor("#44be65")),
                        Folder(folderName = "Travel", folderImg = com.hungday.mytodoapp.R.drawable.ic_travel, folderColor = Color.parseColor("#f9a9ab")),
                        Folder(folderName = "Study", folderImg = com.hungday.mytodoapp.R.drawable.ic_study, folderColor = Color.parseColor("#f89520")),
                        Folder(folderName = "Groceries", folderImg = com.hungday.mytodoapp.R.drawable.ic_shopping, folderColor = Color.parseColor("#a792ec"))
                    )

                    folders.forEach { folder ->
                        todoDao.insertFolder(folder)
                    }
                    Log.d("DatabaseLog", "Default folders inserted successfully!")
                } catch (e: Exception) {
                    Log.e("DatabaseLog", "Error inserting default folders", e)
                }
            }
        }
    }
}