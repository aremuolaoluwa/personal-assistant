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

class ReminderReceiver : BroadcastReceiver() {

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
                reminderId
            )

        val title =
            intent.getStringExtra(
                "REMINDER_TITLE"
            ) ?: "Reminder"

        val message =
            intent.getStringExtra(
                "REMINDER_MESSAGE"
            ) ?: "You have something to remember."

        val repeatRule =
            intent.getStringExtra(
                "REMINDER_REPEAT_RULE"
            ) ?: "None"

        val occurrenceDate =
            intent.getStringExtra(
                "OCCURRENCE_DATE"
            )

        val followUpEnabled =
            intent.getBooleanExtra(
                "FOLLOW_UP_ENABLED",
                false
            )

        val priority =
            intent.getStringExtra(
                "REMINDER_PRIORITY"
            ) ?: "Normal"

        val reminderType =
            intent.getStringExtra(
                "REMINDER_TYPE"
            ) ?: "Reminder"

        val reminderTiming =
            intent.getStringExtra(
                "REMINDER_TIMING"
            ) ?: "At time of event"

        val isFollowUp =
            intent.getBooleanExtra(
                "IS_FOLLOW_UP",
                false
            )

        val recurring =
            ReminderScheduler.isRecurring(
                repeatRule
            )

        ensureNotificationChannels(
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
            return
        }

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
            if (recurring) {

                Intent(
                    context,
                    DoneForTodayReceiver::class.java
                ).apply {

                    putExtra(
                        "REMINDER_ID",
                        reminderId
                    )

                    putExtra(
                        "OCCURRENCE_DATE",
                        occurrenceDate
                    )
                }

            } else {

                Intent(
                    context,
                    DoneReceiver::class.java
                ).apply {

                    putExtra(
                        "REMINDER_ID",
                        reminderId
                    )
                }
            }

        val donePendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId * 10 + 1,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val snoozeIntent =
            Intent(
                context,
                SnoozeReceiver::class.java
            ).apply {

                putExtra(
                    "REMINDER_ID",
                    reminderId
                )

                putExtra(
                    "NOTIFICATION_ID",
                    notificationId
                )

                putExtra(
                    "REMINDER_TITLE",
                    title
                )

                putExtra(
                    "REMINDER_MESSAGE",
                    message
                )

                putExtra(
                    "REMINDER_REPEAT_RULE",
                    repeatRule
                )

                putExtra(
                    "OCCURRENCE_DATE",
                    occurrenceDate
                )

                putExtra(
                    "FOLLOW_UP_ENABLED",
                    followUpEnabled
                )

                putExtra(
                    "REMINDER_PRIORITY",
                    priority
                )

                putExtra(
                    "REMINDER_TYPE",
                    reminderType
                )

                putExtra(
                    "REMINDER_TIMING",
                    reminderTiming
                )

                putExtra(
                    "IS_FOLLOW_UP",
                    isFollowUp
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

        val stillWorkingIntent =
            Intent(
                context,
                StillWorkingReceiver::class.java
            ).apply {

                putExtra(
                    "REMINDER_ID",
                    reminderId
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

        val isEventAdvanceAlert =
            reminderType.equals(
                "Event",
                ignoreCase = true
            ) &&
                !isFollowUp &&
                reminderTiming !=
                    "At time of event"

        val doneButtonText =
            when {

                recurring ->
                    "Done for today"

                reminderType.equals(
                    "Task",
                    ignoreCase = true
                ) ||
                    reminderType.equals(
                        "Deadline",
                        ignoreCase = true
                    ) ->
                    "Completed"

                else ->
                    "Done"
            }

        val keepActiveButtonText =
            when {

                reminderType.equals(
                    "Event",
                    ignoreCase = true
                ) ->
                    "Still relevant"

                reminderType.equals(
                    "Reminder",
                    ignoreCase = true
                ) ->
                    "Keep active"

                else ->
                    "Still working"
            }

        val notificationBuilder =
            NotificationCompat.Builder(
                context,
                channelIdForPriority(
                    priority
                )
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    title
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
                    compatPriorityForPriority(
                        priority
                    )
                )
                .setCategory(
                    NotificationCompat.CATEGORY_REMINDER
                )
                .setContentIntent(
                    openAppPendingIntent
                )
                .setAutoCancel(true)

        /*
         * Event alerts that fire before the actual event do not
         * show a completion button. Tapping "Done" several days
         * early could otherwise complete the whole event or move
         * a recurring event to its next occurrence.
         */
        if (!isEventAdvanceAlert) {

            notificationBuilder.addAction(
                0,
                doneButtonText,
                donePendingIntent
            )
        }

        if (
            followUpEnabled &&
            !recurring &&
            !isEventAdvanceAlert
        ) {

            notificationBuilder.addAction(
                0,
                keepActiveButtonText,
                stillWorkingPendingIntent
            )
        }

        notificationBuilder.addAction(
            0,
            "Snooze 10 min",
            snoozePendingIntent
        )

        val notification =
            notificationBuilder.build()

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.notify(
            notificationId,
            notification
        )
    }

    companion object {

        const val CHANNEL_ID =
            "personal_reminder_channel"

        private const val NORMAL_CHANNEL_ID =
            "personal_reminder_normal_v2"

        private const val IMPORTANT_CHANNEL_ID =
            "personal_reminder_important_v2"

        private const val CRITICAL_CHANNEL_ID =
            "personal_reminder_critical_v2"

        fun channelIdForPriority(
            priority: String
        ): String {

            return when (priority) {

                "Critical" ->
                    CRITICAL_CHANNEL_ID

                "Important" ->
                    IMPORTANT_CHANNEL_ID

                else ->
                    NORMAL_CHANNEL_ID
            }
        }

        fun compatPriorityForPriority(
            priority: String
        ): Int {

            return when (priority) {

                "Critical" ->
                    NotificationCompat.PRIORITY_MAX

                "Important" ->
                    NotificationCompat.PRIORITY_HIGH

                else ->
                    NotificationCompat.PRIORITY_DEFAULT
            }
        }

        fun ensureNotificationChannels(
            context: Context
        ) {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O
            ) {

                val normalChannel =
                    NotificationChannel(
                        NORMAL_CHANNEL_ID,
                        "Normal reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {

                        description =
                            "Standard reminders and follow-ups"
                    }

                val importantChannel =
                    NotificationChannel(
                        IMPORTANT_CHANNEL_ID,
                        "Important reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {

                        description =
                            "Important reminders that need stronger attention"
                    }

                val criticalChannel =
                    NotificationChannel(
                        CRITICAL_CHANNEL_ID,
                        "Critical reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {

                        description =
                            "Critical reminders and urgent follow-ups"
                    }

                val notificationManager =
                    context.getSystemService(
                        Context.NOTIFICATION_SERVICE
                    ) as NotificationManager

                notificationManager
                    .createNotificationChannels(
                        listOf(
                            normalChannel,
                            importantChannel,
                            criticalChannel
                        )
                    )
            }
        }
    }
}
