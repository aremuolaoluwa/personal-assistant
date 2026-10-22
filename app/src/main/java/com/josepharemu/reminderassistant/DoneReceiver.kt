package com.josepharemu.reminderassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DoneReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val reminderId =
            intent.getIntExtra(
                "REMINDER_ID",
                0
            )

        if (reminderId == 0) {
            return
        }

        val pendingResult =
            goAsync()

        val scope =
            CoroutineScope(
                SupervisorJob() +
                        Dispatchers.IO
            )

        scope.launch {

            try {

                val database =
                    ReminderDatabase
                        .getDatabase(
                            context
                        )

                val reminder =
                    database
                        .reminderDao()
                        .getReminderById(
                            reminderId
                        ) ?: return@launch

                val completionLabel =
                    when (reminder.type) {

                        "Task",
                        "Deadline" ->
                            "Completed"

                        else ->
                            "Done"
                    }

                database
                    .reminderActionDao()
                    .insertAction(
                        ReminderAction(
                            reminderId =
                                reminderId,
                            actionType =
                                "COMPLETED",
                            occurrenceDate =
                                reminder.date,
                            details =
                                completionLabel
                        )
                    )

                /*
                 * Mark the whole reminder series
                 * as completed.
                 */
                database
                    .reminderDao()
                    .markReminderCompleted(
                        reminderId
                    )

                /*
                 * Cancel every normal,
                 * recurring and snoozed alarm.
                 */
                ReminderScheduler
                    .cancelReminder(
                        context,
                        reminderId
                    )

                UpcomingReminderWidgetUpdater
                    .updateAll(
                        context
                    )

            } finally {

                pendingResult.finish()
            }
        }
    }
}
