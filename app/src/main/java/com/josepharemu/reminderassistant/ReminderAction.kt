package com.josepharemu.reminderassistant

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_actions",
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["reminderId"]
        )
    ]
)
data class ReminderAction(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val reminderId: Int,

    val actionType: String,

    val timestamp: Long =
        System.currentTimeMillis(),

    val occurrenceDate: String? = null,

    val details: String? = null
)