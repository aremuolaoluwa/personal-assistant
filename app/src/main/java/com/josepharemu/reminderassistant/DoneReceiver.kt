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

        /*
         * Cancel all future alarms and any visible
         * notifications belonging to this reminder.
         */
        ReminderScheduler.cancelReminder(
            context,
            reminderId
        )

        val pendingResult = goAsync()

        val scope =
            CoroutineScope(
                SupervisorJob() + Dispatchers.IO
            )

        scope.launch {

            try {

                val database =
                    ReminderDatabase.getDatabase(
                        context
                    )

                database
                    .reminderDao()
                    .markReminderCompleted(
                        reminderId
                    )

            } finally {

                pendingResult.finish()
            }
        }
    }
}