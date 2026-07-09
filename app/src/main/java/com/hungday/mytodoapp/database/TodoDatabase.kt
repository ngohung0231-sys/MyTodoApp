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
import com.hungday.mytodoapp.model.TrashItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

@Database(entities = [Folder::class, Task::class, TodoList::class, TrashItem::class], version = 21, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun trashDao(): TrashDao

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
                        Folder(folderName = "Others", folderImg = com.hungday.mytodoapp.R.drawable.ic_project, folderColor = "#4997cf".toColorInt()),
                        Folder(folderName = "Personal", folderImg = com.hungday.mytodoapp.R.drawable.ic_profile, folderColor = "#ee4d5e".toColorInt()),
                        Folder(folderName = "Exercise", folderImg = com.hungday.mytodoapp.R.drawable.ic_exercise, folderColor = "#44be65".toColorInt()),
                        Folder(folderName = "Travel", folderImg = com.hungday.mytodoapp.R.drawable.ic_travel, folderColor = "#f9a9ab".toColorInt()),
                        Folder(folderName = "Study", folderImg = com.hungday.mytodoapp.R.drawable.ic_study, folderColor = "#f89520".toColorInt()),
                        Folder(folderName = "Shopping", folderImg = com.hungday.mytodoapp.R.drawable.ic_shopping, folderColor = "#a792ec".toColorInt())
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