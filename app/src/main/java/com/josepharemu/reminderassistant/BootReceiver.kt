package com.josepharemu.reminderassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        Log.d(
            "PRA_BOOT",
            "Received action: ${intent.action}"
        )

        if (
            intent.action !=
            Intent.ACTION_BOOT_COMPLETED
        ) {
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

                val activeReminders =
                    dao
                        .getActiveReminders()
                        .first()

                Log.d(
                    "PRA_BOOT",
                    "Found ${activeReminders.size} active reminders"
                )

                activeReminders.forEach {
                        reminder ->

                    /*
                     * If a recurring reminder became
                     * overdue while the phone was off,
                     * move it to its next valid future
                     * occurrence first.
                     */
                    val reminderToSchedule =
                        ReminderScheduler
                            .moveRecurringReminderToFuture(
                                reminder
                            )

                    if (
                        reminderToSchedule.date !=
                        reminder.date
                    ) {

                        dao.updateReminderDate(
                            reminderId =
                                reminder.id,

                            newDate =
                                reminderToSchedule.date
                        )
                    }

                    val scheduled =
                        ReminderScheduler
                            .scheduleReminder(
                                context,
                                reminderToSchedule
                            )

                    Log.d(
                        "PRA_BOOT",
                        "Rescheduled ${reminder.title}: $scheduled"
                    )
                }

            } catch (
                exception: Exception
            ) {

                Log.e(
                    "PRA_BOOT",
                    "Failed to restore reminders",
                    exception
                )

            } finally {

                pendingResult.finish()
            }
        }
    }
}