package com.josepharemu.reminderassistant

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FollowUpReceiver : BroadcastReceiver() {

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
                        .getDatabase(context)

                val reminder =
                    database
                        .reminderDao()
                        .getReminderById(
                            reminderId
                        )
                        ?: return@launch

                if (
                    reminder.isCompleted ||
                    !reminder.followUpUntilCompleted ||
                    ReminderScheduler.isRecurring(
                        reminder.repeatRule
                    )
                ) {
                    return@launch
                }

                ReminderScheduler
                    .scheduleNextFollowUp(
                        context,
                        reminder
                    )

                ReminderReceiver
                    .ensureNotificationChannels(
                        context
                    )

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }

                val notificationId =
                    ReminderScheduler
                        .createFollowUpNotificationId(
                            reminder.id
                        )

                val openAppIntent =
                    Intent(
                        context,
                        MainActivity::class.java
                    ).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                val openAppPendingIntent =
                    PendingIntent.getActivity(
                        context,
                        notificationId,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                val doneIntent =
                    Intent(
                        context,
                        DoneReceiver::class.java
                    ).apply {

                        putExtra(
                            "REMINDER_ID",
                            reminder.id
                        )
                    }

                val donePendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        notificationId * 10 + 1,
                        doneIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                val stillWorkingIntent =
                    Intent(
                        context,
                        StillWorkingReceiver::class.java
                    ).apply {

                        putExtra(
                            "REMINDER_ID",
                            reminder.id
                        )

                        putExtra(
                            "NOTIFICATION_ID",
                            notificationId
                        )
                    }

                val stillWorkingPendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        notificationId * 10 + 3,
                        stillWorkingIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                val message =
                    when (reminder.type) {

                        "Deadline" ->
                            "Still incomplete • Final deadline was " +
                                "${reminder.deadlineDate} at " +
                                reminder.deadlineTime

                        "Task" ->
                            "Still open • Task was due " +
                                "${reminder.date} at ${reminder.time}"

                        "Event" ->
                            "Still open • Event was scheduled for " +
                                "${reminder.date} at ${reminder.time}"

                        else ->
                            "Still open • Scheduled for " +
                                "${reminder.date} at ${reminder.time}"
                    }

                val snoozeIntent =
                    Intent(
                        context,
                        SnoozeReceiver::class.java
                    ).apply {

                        putExtra(
                            "REMINDER_ID",
                            reminder.id
                        )

                        putExtra(
                            "NOTIFICATION_ID",
                            notificationId
                        )

                        putExtra(
                            "REMINDER_TITLE",
                            reminder.title
                        )

                        putExtra(
                            "REMINDER_MESSAGE",
                            message
                        )

                        putExtra(
                            "REMINDER_REPEAT_RULE",
                            "None"
                        )

                        putExtra(
                            "OCCURRENCE_DATE",
                            reminder.date
                        )

                        putExtra(
                            "FOLLOW_UP_ENABLED",
                            true
                        )

                        putExtra(
                            "REMINDER_PRIORITY",
                            reminder.priority
                        )

                        putExtra(
                            "REMINDER_TYPE",
                            reminder.type
                        )

                        putExtra(
                            "REMINDER_TIMING",
                            "Follow-up"
                        )

                        putExtra(
                            "IS_FOLLOW_UP",
                            true
                        )
                    }

                val snoozePendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        notificationId * 10 + 2,
                        snoozeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                    )

                val doneButtonText =
                    when (reminder.type) {

                        "Task",
                        "Deadline" ->
                            "Completed"

                        else ->
                            "Done"
                    }

                val keepActiveButtonText =
                    when (reminder.type) {

                        "Event" ->
                            "Still relevant"

                        "Reminder" ->
                            "Keep active"

                        else ->
                            "Still working"
                    }

                val notification =
                    NotificationCompat.Builder(
                        context,
                        ReminderReceiver
                            .channelIdForPriority(
                                reminder.priority
                            )
                    )
                        .setSmallIcon(
                            android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(
                            reminder.title
                        )
                        .setContentText(
                            message
                        )
                        .setStyle(
                            NotificationCompat
                                .BigTextStyle()
                                .bigText(message)
                        )
                        .setPriority(
                            ReminderReceiver
                                .compatPriorityForPriority(
                                    reminder.priority
                                )
                        )
                        .setCategory(
                            NotificationCompat.CATEGORY_REMINDER
                        )
                        .setContentIntent(
                            openAppPendingIntent
                        )
                        .addAction(
                            0,
                            doneButtonText,
                            donePendingIntent
                        )
                        .addAction(
                            0,
                            keepActiveButtonText,
                            stillWorkingPendingIntent
                        )
                        .addAction(
                            0,
                            "Snooze 10 min",
                            snoozePendingIntent
                        )
                        .setAutoCancel(true)
                        .build()

                val notificationManager =
                    context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                notificationManager.notify(
                    notificationId,
                    notification
                )

                database
                    .reminderActionDao()
                    .insertAction(
                        ReminderAction(
                            reminderId =
                                reminder.id,
                            actionType =
                                "FOLLOW_UP_SENT",
                            occurrenceDate =
                                if (
                                    reminder.type ==
                                    "Deadline" &&
                                    reminder.deadlineDate !=
                                    "None"
                                ) {
                                    reminder.deadlineDate
                                } else {
                                    reminder.date
                                },
                            details =
                                "Follow-up notification sent"
                        )
                    )

            } finally {

                pendingResult.finish()
            }
        }
    }

}
