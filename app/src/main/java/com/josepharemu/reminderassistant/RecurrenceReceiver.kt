package com.josepharemu.reminderassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecurrenceReceiver : BroadcastReceiver() {

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

                val dao =
                    database.reminderDao()

                val currentReminder =
                    dao.getReminderById(
                        reminderId
                    )

                if (
                    currentReminder == null ||
                    currentReminder.isCompleted ||
                    !ReminderScheduler.isRecurring(
                        currentReminder.repeatRule
                    )
                ) {

                    return@launch
                }

                val nextReminder =
                    ReminderScheduler
                        .getNextOccurrence(
                            currentReminder
                        )

                /*
                 * If the next occurrence would be after the
                 * Repeat Until date, getNextOccurrence()
                 * returns the current occurrence unchanged.
                 * The series therefore stops here.
                 */
                if (
                    nextReminder.date ==
                    currentReminder.date
                ) {
                    return@launch
                }

                dao.updateReminderOccurrence(
                    reminderId =
                        reminderId,

                    newDate =
                        nextReminder.date,

                    anchorDate =
                        nextReminder.repeatAnchorDate
                )

                val latestReminder =
                    dao.getReminderById(
                        reminderId
                    )

                if (
                    latestReminder != null &&
                    !latestReminder.isCompleted &&
                    ReminderScheduler.isRecurring(
                        latestReminder.repeatRule
                    )
                ) {

                    ReminderScheduler
                        .scheduleReminder(
                            context,
                            latestReminder
                        )
                }

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
