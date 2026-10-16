package com.josepharemu.reminderassistant

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,

    val type: String,

    val date: String,

    val time: String,

    val priority: String,

    val category: String,

    val reminderTiming: String,

    val notes: String,

    val isCompleted: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(defaultValue = "None")
    val repeatRule: String = "None"
)