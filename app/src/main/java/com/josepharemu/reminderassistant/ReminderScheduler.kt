package com.josepharemu.reminderassistant

import android.app.AlarmManager
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

    fun scheduleReminder(
        context: Context,
        reminder: Reminder
    ): Boolean {

        // A notification cannot be scheduled without a time.
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

        val notificationDateTime =
            when (reminder.reminderTiming) {

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

        val triggerTime =
            notificationDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

        // Do not schedule notifications in the past.
        if (triggerTime <= System.currentTimeMillis()) {
            return false
        }

        val message =
            if (reminder.notes.isBlank()) {

                "${reminder.type} scheduled for " +
                        "${reminder.date} at ${reminder.time}"

            } else {

                reminder.notes
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
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        /*
         * Android 12+ requires special access
         * for exact alarms.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (alarmManager.canScheduleExactAlarms()) {

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )

            } else {

                /*
                 * Fallback if exact alarm permission
                 * has not yet been granted.
                 *
                 * This alarm can be delayed by Android.
                 */
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

        return true
    }

    fun cancelReminder(
        context: Context,
        reminderId: Int
    ) {

        val intent = Intent(
            context,
            ReminderReceiver::class.java
        )

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
            )

        if (pendingIntent != null) {

            val alarmManager =
                context.getSystemService(
                    Context.ALARM_SERVICE
                ) as AlarmManager

            alarmManager.cancel(
                pendingIntent
            )

            pendingIntent.cancel()
        }
    }
}