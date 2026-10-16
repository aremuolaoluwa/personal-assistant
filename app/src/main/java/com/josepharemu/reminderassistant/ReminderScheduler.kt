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

    private const val RECURRENCE_CODE = 90

    fun scheduleReminder(
        context: Context,
        reminder: Reminder
    ): Boolean {

        if (reminder.time.isBlank()) {
            return false
        }

        val eventDateTime =
            parseEventDateTime(reminder)
                ?: return false

        val selectedTimings =
            parseReminderTimings(
                reminder.reminderTiming
            )

        var scheduledAtLeastOneNotification =
            false

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

            if (
                triggerTime >
                System.currentTimeMillis()
            ) {

                scheduleSingleNotificationAlarm(
                    context = context,
                    reminder = reminder,
                    timing = timing,
                    triggerTime = triggerTime
                )

                scheduledAtLeastOneNotification =
                    true
            }
        }

        /*
         * Recurring reminders need a separate
         * internal alarm at the actual event time.
         *
         * This alarm does not show a notification.
         * It moves the reminder to its next date.
         */
        if (
            isRecurring(
                reminder.repeatRule
            )
        ) {

            val eventTriggerTime =
                eventDateTime
                    .atZone(
                        ZoneId.systemDefault()
                    )
                    .toInstant()
                    .toEpochMilli()

            if (
                eventTriggerTime >
                System.currentTimeMillis()
            ) {

                scheduleRecurrenceAlarm(
                    context = context,
                    reminder = reminder,
                    triggerTime =
                        eventTriggerTime
                )
            }
        }

        return scheduledAtLeastOneNotification
    }

    private fun scheduleSingleNotificationAlarm(
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

        val intent =
            Intent(
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

        scheduleAlarm(
            context = context,
            triggerTime = triggerTime,
            pendingIntent = pendingIntent
        )
    }

    private fun scheduleRecurrenceAlarm(
        context: Context,
        reminder: Reminder,
        triggerTime: Long
    ) {

        val recurrenceAlarmId =
            createRecurrenceAlarmId(
                reminder.id
            )

        val intent =
            Intent(
                context,
                RecurrenceReceiver::class.java
            ).apply {

                putExtra(
                    "REMINDER_ID",
                    reminder.id
                )
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                recurrenceAlarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        scheduleAlarm(
            context = context,
            triggerTime = triggerTime,
            pendingIntent = pendingIntent
        )
    }

    private fun scheduleAlarm(
        context: Context,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {

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

    /*
     * Used by the reboot receiver and recurrence receiver.
     *
     * If a recurring reminder is now in the past,
     * this moves it forward until its event time
     * is in the future.
     */
    fun moveRecurringReminderToFuture(
        reminder: Reminder
    ): Reminder {

        if (
            !isRecurring(
                reminder.repeatRule
            )
        ) {
            return reminder
        }

        val originalDateTime =
            parseEventDateTime(reminder)
                ?: return reminder

        var nextDateTime =
            originalDateTime

        val now =
            LocalDateTime.now()

        var safetyCounter = 0

        while (
            !nextDateTime.isAfter(now) &&
            safetyCounter < 10000
        ) {

            nextDateTime =
                advanceOccurrence(
                    nextDateTime,
                    reminder.repeatRule
                )

            safetyCounter++
        }

        if (
            nextDateTime ==
            originalDateTime
        ) {
            return reminder
        }

        return reminder.copy(
            date =
                nextDateTime
                    .toLocalDate()
                    .format(
                        dateFormatter
                    )
        )
    }

    private fun advanceOccurrence(
        dateTime: LocalDateTime,
        repeatRule: String
    ): LocalDateTime {

        return when (repeatRule) {

            "Daily" ->
                dateTime.plusDays(1)

            "Weekly" ->
                dateTime.plusWeeks(1)

            "Monthly" ->
                dateTime.plusMonths(1)

            "Yearly" ->
                dateTime.plusYears(1)

            else ->
                dateTime
        }
    }

    private fun parseEventDateTime(
        reminder: Reminder
    ): LocalDateTime? {

        return try {

            val date =
                LocalDate.parse(
                    reminder.date,
                    dateFormatter
                )

            val time =
                LocalTime.parse(
                    reminder.time,
                    timeFormatter
                )

            LocalDateTime.of(
                date,
                time
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    private fun isRecurring(
        repeatRule: String
    ): Boolean {

        return repeatRule ==
                "Daily" ||
                repeatRule ==
                "Weekly" ||
                repeatRule ==
                "Monthly" ||
                repeatRule ==
                "Yearly"
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
         * Cancel all normal reminder alarms,
         * snoozed alarms and visible notifications.
         */
        timingCodes.forEach { (timing, _) ->

            val alarmId =
                createAlarmId(
                    reminderId,
                    timing
                )

            val reminderIntent =
                Intent(
                    context,
                    ReminderReceiver::class.java
                )

            val normalPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    alarmId,
                    reminderIntent,
                    PendingIntent.FLAG_NO_CREATE or
                            PendingIntent.FLAG_IMMUTABLE
                )

            if (
                normalPendingIntent != null
            ) {

                alarmManager.cancel(
                    normalPendingIntent
                )

                normalPendingIntent.cancel()
            }

            /*
             * Snooze now uses a different request
             * code so it cannot overwrite a future
             * recurring notification.
             */
            val snoozeAlarmId =
                createSnoozeAlarmRequestCode(
                    alarmId
                )

            val snoozeIntent =
                Intent(
                    context,
                    ReminderReceiver::class.java
                )

            val snoozePendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    snoozeAlarmId,
                    snoozeIntent,
                    PendingIntent.FLAG_NO_CREATE or
                            PendingIntent.FLAG_IMMUTABLE
                )

            if (
                snoozePendingIntent != null
            ) {

                alarmManager.cancel(
                    snoozePendingIntent
                )

                snoozePendingIntent.cancel()
            }

            notificationManager.cancel(
                alarmId
            )
        }

        /*
         * Cancel the recurrence alarm itself.
         */
        val recurrenceIntent =
            Intent(
                context,
                RecurrenceReceiver::class.java
            )

        val recurrencePendingIntent =
            PendingIntent.getBroadcast(
                context,
                createRecurrenceAlarmId(
                    reminderId
                ),
                recurrenceIntent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
            )

        if (
            recurrencePendingIntent != null
        ) {

            alarmManager.cancel(
                recurrencePendingIntent
            )

            recurrencePendingIntent.cancel()
        }

        /*
         * Compatibility with reminders created
         * before multiple notification support.
         */
        val legacyIntent =
            Intent(
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

        if (
            legacyPendingIntent != null
        ) {

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

    private fun createRecurrenceAlarmId(
        reminderId: Int
    ): Int {

        return reminderId * 100 +
                RECURRENCE_CODE
    }

    fun createSnoozeAlarmRequestCode(
        notificationId: Int
    ): Int {

        return notificationId * 10 + 9
    }
}