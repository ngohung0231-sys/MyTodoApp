package com.hungday.mytodoapp.database

import androidx.room.TypeConverter
import com.hungday.mytodoapp.model.SubTask
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DateConverter {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    @TypeConverter
    fun fromString(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun dateToString(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun fromTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, timeFormatter) }
    }

    @TypeConverter
    fun timeToString(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }

    @TypeConverter
    fun fromSubTaskList(value: List<SubTask>?): String? {
        if (value == null) return null
        val array = JSONArray()
        value.forEach { subTask ->
            val json = JSONObject()
            json.put("id", subTask.id)
            json.put("title", subTask.title)
            json.put("isCompleted", subTask.isCompleted)
            json.put("isTask", subTask.isTask)
            array.put(json)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubTaskList(value: String?): List<SubTask>? {
        if (value == null) return null
        val list = mutableListOf<SubTask>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            val json = array.getJSONObject(i)
            list.add(
                SubTask(
                    id = json.getString("id"),
                    title = json.getString("title"),
                    isCompleted = json.getBoolean("isCompleted"),
                    isTask = json.optBoolean("isTask", true)
                )
            )
        }
        return list
    }
}