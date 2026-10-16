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

        /*
         * Remove the notification that
         * is currently visible.
         */
        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.cancel(
            notificationId
        )

        /*
         * The snooze alarm gets a different
         * PendingIntent request code from the
         * normal recurring alarm.
         */
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

                /*
                 * The notification itself keeps
                 * the original notification ID.
                 */
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
                alarmManager
                    .canScheduleExactAlarms()
            ) {

                alarmManager
                    .setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )

            } else {

                alarmManager
                    .setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
            }

        } else {

            alarmManager
                .setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
        }
    }
}