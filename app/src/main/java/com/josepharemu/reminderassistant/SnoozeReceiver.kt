package com.josepharemu.reminderassistant

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class SnoozeReceiver : BroadcastReceiver() {

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

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.cancel(
            notificationId
        )

        val snoozeAlarmRequestCode =
            ReminderScheduler
                .createSnoozeAlarmRequestCode(
                    notificationId
                )

        val reminderIntent =
            Intent(
                context,
                ReminderReceiver::class.java
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
                    "Snoozed reminder • $message"
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
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                snoozeAlarmRequestCode,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val triggerTime =
            System.currentTimeMillis() +
                    (10 * 60 * 1000L)

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            if (
                alarmManager.canScheduleExactAlarms()
            ) {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )

            } else {

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

        } else {

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}
