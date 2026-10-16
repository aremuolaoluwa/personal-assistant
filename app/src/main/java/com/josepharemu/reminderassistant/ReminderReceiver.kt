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

        val recurring =
            ReminderScheduler.isRecurring(
                repeatRule
            )

        createNotificationChannel(
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
            if (recurring) {
                "Done for today"
            } else {
                "Done"
            }

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
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
                    NotificationCompat.PRIORITY_HIGH
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
    }

    private fun createNotificationChannel(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Reminder Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        "Notifications for scheduled reminders"
                }

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager
                .createNotificationChannel(
                    channel
                )
        }
    }

    companion object {

        const val CHANNEL_ID =
            "personal_reminder_channel"
    }
}