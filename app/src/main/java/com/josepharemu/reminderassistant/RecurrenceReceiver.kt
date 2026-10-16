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
                    currentReminder.repeatRule ==
                    "None"
                ) {

                    return@launch
                }

                /*
                 * Move the date to the next valid
                 * future occurrence.
                 */
                val futureReminder =
                    ReminderScheduler
                        .moveRecurringReminderToFuture(
                            currentReminder
                        )

                if (
                    futureReminder.date !=
                    currentReminder.date
                ) {

                    /*
                     * Only update the date.
                     *
                     * This avoids accidentally
                     * overwriting another change,
                     * such as the user pressing Done.
                     */
                    dao.updateReminderDate(
                        reminderId =
                            reminderId,

                        newDate =
                            futureReminder.date
                    )
                }

                /*
                 * Reload the reminder before scheduling.
                 *
                 * If the user pressed Done while this
                 * receiver was working, we do not want
                 * to start another recurring alarm.
                 */
                val latestReminder =
                    dao.getReminderById(
                        reminderId
                    )

                if (
                    latestReminder != null &&
                    !latestReminder.isCompleted &&
                    latestReminder.repeatRule !=
                    "None"
                ) {

                    ReminderScheduler
                        .scheduleReminder(
                            context,
                            latestReminder
                        )
                }

            } finally {

                pendingResult.finish()
            }
        }
    }
}