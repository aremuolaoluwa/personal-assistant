package com.josepharemu.reminderassistant

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderScheduler {

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    private val timingCodes = mapOf(
        "At time of event" to 1,
        "10 minutes before" to 2,
        "30 minutes before" to 3,
        "1 hour before" to 4,
        "2 hours before" to 5,
        "1 day before" to 6,
        "3 days before" to 7,
        "1 week before" to 8
    )

    fun scheduleReminder(
        context: Context,
        reminder: Reminder
    ): Boolean {

        if (reminder.time.isBlank()) {
            return false
        }

        val eventDateTime = try {

            val date = LocalDate.parse(
                reminder.date,
                dateFormatter
            )

            val time = LocalTime.parse(
                reminder.time,
                timeFormatter
            )

            LocalDateTime.of(
                date,
                time
            )

        } catch (exception: Exception) {

            return false
        }

        val selectedTimings =
            parseReminderTimings(
                reminder.reminderTiming
            )

        var scheduledAtLeastOne = false

        selectedTimings.forEach { timing ->

            val notificationDateTime =
                calculateNotificationTime(
                    eventDateTime,
                    timing
                )

            val triggerTime =
                notificationDateTime
                    .atZone(
                        ZoneId.systemDefault()
                    )
                    .toInstant()
                    .toEpochMilli()

            /*
             * Do not create an alarm if that particular
             * reminder time has already passed.
             */
            if (
                triggerTime >
                System.currentTimeMillis()
            ) {

                scheduleSingleAlarm(
                    context = context,
                    reminder = reminder,
                    timing = timing,
                    triggerTime = triggerTime
                )

                scheduledAtLeastOne = true
            }
        }

        return scheduledAtLeastOne
    }

    private fun calculateNotificationTime(
        eventDateTime: LocalDateTime,
        timing: String
    ): LocalDateTime {

        return when (timing) {

            "10 minutes before" ->
                eventDateTime.minusMinutes(10)

            "30 minutes before" ->
                eventDateTime.minusMinutes(30)

            "1 hour before" ->
                eventDateTime.minusHours(1)

            "2 hours before" ->
                eventDateTime.minusHours(2)

            "1 day before" ->
                eventDateTime.minusDays(1)

            "3 days before" ->
                eventDateTime.minusDays(3)

            "1 week before" ->
                eventDateTime.minusWeeks(1)

            else ->
                eventDateTime
        }
    }

    private fun scheduleSingleAlarm(
        context: Context,
        reminder: Reminder,
        timing: String,
        triggerTime: Long
    ) {

        val notificationId =
            createAlarmId(
                reminder.id,
                timing
            )

        val message =
            if (reminder.notes.isBlank()) {

                "$timing • ${reminder.type} scheduled " +
                        "for ${reminder.date} at ${reminder.time}"

            } else {

                "$timing • ${reminder.notes}"
            }

        val intent = Intent(
            context,
            ReminderReceiver::class.java
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
        }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

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

    fun cancelReminder(
        context: Context,
        reminderId: Int
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        /*
         * Cancel every possible alarm belonging
         * to this reminder.
         */
        timingCodes.forEach { (timing, _) ->

            val alarmId =
                createAlarmId(
                    reminderId,
                    timing
                )

            val intent = Intent(
                context,
                ReminderReceiver::class.java
            )

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    alarmId,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or
                            PendingIntent.FLAG_IMMUTABLE
                )

            if (pendingIntent != null) {

                alarmManager.cancel(
                    pendingIntent
                )

                pendingIntent.cancel()
            }

            notificationManager.cancel(
                alarmId
            )
        }

        /*
         * Compatibility with reminders that were
         * scheduled before multiple alarms were added.
         */
        val legacyIntent = Intent(
            context,
            ReminderReceiver::class.java
        )

        val legacyPendingIntent =
            PendingIntent.getBroadcast(
                context,
                reminderId,
                legacyIntent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
            )

        if (legacyPendingIntent != null) {

            alarmManager.cancel(
                legacyPendingIntent
            )

            legacyPendingIntent.cancel()
        }

        notificationManager.cancel(
            reminderId
        )
    }

    private fun createAlarmId(
        reminderId: Int,
        timing: String
    ): Int {

        val timingCode =
            timingCodes[timing] ?: 1

        return reminderId * 100 +
                timingCode
    }
}