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
    val repeatRule: String = "None",

    /*
     * Stores the original date used as the recurrence anchor.
     *
     * Examples:
     * Monthly reminder beginning 31/01/2027:
     * anchor remains 31/01/2027 even when February
     * occurrence must temporarily be 28/02/2027.
     *
     * Yearly reminder beginning 29/02/2028:
     * anchor preserves 29 February through
     * non-leap years.
     */
    @ColumnInfo(defaultValue = "None")
    val repeatAnchorDate: String = "None"
)