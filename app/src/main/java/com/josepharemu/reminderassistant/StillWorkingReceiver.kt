package com.josepharemu.reminderassistant

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StillWorkingReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val reminderId =
            intent.getIntExtra(
                "REMINDER_ID",
                0
            )

        val notificationId =
            intent.getIntExtra(
                "NOTIFICATION_ID",
                0
            )

        if (
            reminderId == 0 ||
            notificationId == 0
        ) {
            return
        }

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.cancel(
            notificationId
        )

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

                val actionLabel =
                    when (reminder.type) {

                        "Event" ->
                            "Still relevant"

                        "Reminder" ->
                            "Keep active"

                        else ->
                            "Still working"
                    }

                database
                    .reminderActionDao()
                    .insertAction(
                        ReminderAction(
                            reminderId =
                                reminderId,
                            actionType =
                                "KEPT_ACTIVE",
                            occurrenceDate =
                                reminder.date,
                            details =
                                actionLabel
                        )
                    )

            } finally {

                pendingResult.finish()
            }
        }
    }
}
