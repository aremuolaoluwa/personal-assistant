package com.josepharemu.reminderassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DoneForTodayReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val reminderId =
            intent.getIntExtra(
                "REMINDER_ID",
                0
            )

        val occurrenceDate =
            intent.getStringExtra(
                "OCCURRENCE_DATE"
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
                    ) ?: return@launch

                if (
                    currentReminder.isCompleted ||
                    !ReminderScheduler.isRecurring(
                        currentReminder.repeatRule
                    )
                ) {

                    return@launch
                }

                database
                    .reminderActionDao()
                    .insertAction(
                        ReminderAction(
                            reminderId =
                                reminderId,
                            actionType =
                                "DONE_TODAY",
                            occurrenceDate =
                                occurrenceDate
                                    ?: currentReminder.date,
                            details =
                                "Done for today"
                        )
                    )

                /*
                 * Cancel all alarms first.
                 *
                 * If the automatic recurrence receiver
                 * already moved the reminder forward,
                 * we will simply schedule that new
                 * occurrence again below.
                 */
                ReminderScheduler
                    .cancelReminder(
                        context,
                        reminderId
                    )

                var reminderToSchedule =
                    currentReminder

                /*
                 * Only advance the date if this
                 * notification still belongs to the
                 * reminder's current occurrence.
                 *
                 * This prevents:
                 *
                 * 16 Oct -> 17 Oct -> 18 Oct
                 *
                 * when the automatic recurrence alarm
                 * had already changed it to 17 Oct.
                 */
                if (
                    occurrenceDate == null ||
                    currentReminder.date ==
                    occurrenceDate
                ) {

                    val nextReminder =
                        ReminderScheduler
                            .getNextOccurrence(
                                currentReminder
                            )

                    /*
                     * No date change means this is the final
                     * allowed occurrence. "Done for today"
                     * should therefore finish the series.
                     */
                    if (
                        nextReminder.date ==
                        currentReminder.date
                    ) {

                        dao.updateReminder(
                            currentReminder.copy(
                                isCompleted = true,
                                repeatRule = "None",
                                repeatAnchorDate = "None",
                                repeatUntilDate = "None"
                            )
                        )

                        UpcomingReminderWidgetUpdater
                            .updateAll(
                                context
                            )

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

                    reminderToSchedule =
                        nextReminder
                }

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

                    reminderToSchedule =
                        latestReminder

                    ReminderScheduler
                        .scheduleReminder(
                            context,
                            reminderToSchedule
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
