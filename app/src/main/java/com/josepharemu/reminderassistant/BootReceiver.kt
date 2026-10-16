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
            "BootReceiver",
            "Received action: ${intent.action}"
        )

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()

        val scope =
            CoroutineScope(
                SupervisorJob() + Dispatchers.IO
            )

        scope.launch {

            try {

                val database =
                    ReminderDatabase.getDatabase(context)

                val activeReminders =
                    database
                        .reminderDao()
                        .getActiveReminders()
                        .first()

                Log.d(
                    "BootReceiver",
                    "Found ${activeReminders.size} active reminders"
                )

                activeReminders.forEach { reminder ->

                    val scheduled =
                        ReminderScheduler.scheduleReminder(
                            context,
                            reminder
                        )

                    Log.d(
                        "BootReceiver",
                        "Rescheduled ${reminder.title}: $scheduled"
                    )
                }

            } catch (exception: Exception) {

                Log.e(
                    "BootReceiver",
                    "Failed to restore reminders",
                    exception
                )

            } finally {

                pendingResult.finish()
            }
        }
    }
}