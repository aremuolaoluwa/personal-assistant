package com.josepharemu.reminderassistant

import android.content.Context

object ReminderHistoryLogger {

    suspend fun record(
        context: Context,
        reminderId: Int,
        actionType: String,
        occurrenceDate: String? = null,
        details: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {

        if (reminderId == 0) {
            return
        }

        val database =
            ReminderDatabase
                .getDatabase(
                    context
                )

        val reminderExists =
            database
                .reminderDao()
                .getReminderById(
                    reminderId
                ) != null

        if (!reminderExists) {
            return
        }

        database
            .reminderActionDao()
            .insertAction(
                ReminderAction(
                    reminderId =
                        reminderId,
                    actionType =
                        actionType,
                    timestamp =
                        timestamp,
                    occurrenceDate =
                        occurrenceDate,
                    details =
                        details
                )
            )
    }
}
