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
import java.time.YearMonth
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

    /*
     * Deadline escalation alarms use their own request-code
     * range so they never overwrite normal occurrence alarms.
     */
    private val escalationTimingCodes = mapOf(
        "At time of event" to 21,
        "10 minutes before" to 22,
        "30 minutes before" to 23,
        "1 hour before" to 24,
        "2 hours before" to 25,
        "1 day before" to 26,
        "3 days before" to 27,
        "1 week before" to 28
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

        /*
         * A recurring reminder must never schedule an
         * occurrence after its Repeat Until date.
         */
        if (
            isRecurring(reminder.repeatRule) &&
            !isOccurrenceWithinRepeatLimit(reminder)
        ) {
            return false
        }

        val selectedTimings =
            parseReminderTimings(
                reminder.reminderTiming
            )

        val deadlineDateTime =
            if (
                reminder.escalationEnabled &&
                reminder.type.equals(
                    "Deadline",
                    ignoreCase = true
                )
            ) {
                parseDeadlineDateTime(
                    reminder
                )
            } else {
                null
            }

        val escalationTimings =
            if (deadlineDateTime != null) {
                getEscalationTimings(
                    reminder.priority
                )
            } else {
                emptyList()
            }

        val currentOccurrenceIsDeadline =
            deadlineDateTime != null &&
                eventDateTime ==
                deadlineDateTime

        var scheduledAtLeastOneNotification =
            false

        /*
         * Keep the user's manually selected notification
         * timings. On the final deadline occurrence, skip
         * only timings that automatic escalation will
         * already provide so the user does not receive
         * duplicate notifications at the same moment.
         */
        selectedTimings.forEach { timing ->

            val coveredByEscalation =
                currentOccurrenceIsDeadline &&
                    timing in escalationTimings

            if (!coveredByEscalation) {

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
        }

        /*
         * Automatic escalation is calculated from the
         * final deadline, not from the next recurring
         * occurrence. The user's priority controls how
         * intensive the extra alerts become.
         */
        if (deadlineDateTime != null) {

            escalationTimings.forEach { timing ->

                val notificationDateTime =
                    calculateNotificationTime(
                        deadlineDateTime,
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

                    scheduleSingleDeadlineEscalationAlarm(
                        context = context,
                        reminder = reminder,
                        timing = timing,
                        triggerTime = triggerTime
                    )

                    scheduledAtLeastOneNotification =
                        true
                }
            }
        }

        if (
            isRecurring(reminder.repeatRule) &&
            hasNextOccurrence(reminder)
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
                    triggerTime = eventTriggerTime
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

                putExtra(
                    "REMINDER_REPEAT_RULE",
                    reminder.repeatRule
                )

                putExtra(
                    "OCCURRENCE_DATE",
                    reminder.date
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

    private fun scheduleSingleDeadlineEscalationAlarm(
        context: Context,
        reminder: Reminder,
        timing: String,
        triggerTime: Long
    ) {

        val notificationId =
            createEscalationAlarmId(
                reminder.id,
                timing
            )

        val timingText =
            when (timing) {

                "At time of event" ->
                    "Deadline reached"

                else ->
                    timing
            }

        val message =
            "$timingText • Final deadline " +
                "${reminder.deadlineDate} at " +
                reminder.deadlineTime

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

                /*
                 * Escalation notifications refer to the whole
                 * deadline, not one recurring occurrence.
                 * Therefore "Done" completes the deadline
                 * reminder instead of advancing one week.
                 */
                putExtra(
                    "REMINDER_REPEAT_RULE",
                    "None"
                )

                putExtra(
                    "OCCURRENCE_DATE",
                    reminder.deadlineDate
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

    private fun getEscalationTimings(
        priority: String
    ): List<String> {

        return when (priority) {

            "Critical" ->
                listOf(
                    "1 week before",
                    "3 days before",
                    "1 day before",
                    "2 hours before",
                    "1 hour before",
                    "30 minutes before",
                    "10 minutes before",
                    "At time of event"
                )

            "Important" ->
                listOf(
                    "3 days before",
                    "1 day before",
                    "2 hours before",
                    "30 minutes before",
                    "At time of event"
                )

            else ->
                listOf(
                    "1 day before",
                    "1 hour before",
                    "At time of event"
                )
        }
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
     * Returns exactly one next occurrence.
     *
     * This is used by "Done for today" and by
     * the automatic recurrence receiver.
     */
    fun getNextOccurrence(
        reminder: Reminder
    ): Reminder {

        if (!isRecurring(reminder.repeatRule)) {
            return reminder
        }

        val currentDate =
            parseDate(reminder.date)
                ?: return reminder

        val anchorDate =
            resolveAnchorDate(reminder)
                ?: currentDate

        val nextDate =
            when (reminder.repeatRule) {

                "Daily" -> {
                    currentDate.plusDays(1)
                }

                "Weekly" -> {
                    currentDate.plusWeeks(1)
                }

                "Monthly" -> {

                    val nextMonth =
                        YearMonth
                            .from(currentDate)
                            .plusMonths(1)

                    val desiredDay =
                        anchorDate.dayOfMonth

                    val actualDay =
                        minOf(
                            desiredDay,
                            nextMonth.lengthOfMonth()
                        )

                    LocalDate.of(
                        nextMonth.year,
                        nextMonth.monthValue,
                        actualDay
                    )
                }

                "Yearly" -> {

                    val nextYear =
                        currentDate.year + 1

                    val anchorMonth =
                        anchorDate.monthValue

                    val targetMonth =
                        YearMonth.of(
                            nextYear,
                            anchorMonth
                        )

                    val desiredDay =
                        anchorDate.dayOfMonth

                    val actualDay =
                        minOf(
                            desiredDay,
                            targetMonth.lengthOfMonth()
                        )

                    LocalDate.of(
                        nextYear,
                        anchorMonth,
                        actualDay
                    )
                }

                else -> {
                    currentDate
                }
            }

        val repeatUntilDate =
            parseRepeatUntilDate(reminder)

        /*
         * Returning the same reminder is deliberate.
         * Existing callers already use the date change to
         * determine whether a recurrence was advanced.
         */
        if (
            repeatUntilDate != null &&
            nextDate.isAfter(repeatUntilDate)
        ) {
            return reminder
        }

        return reminder.copy(
            date =
                nextDate.format(
                    dateFormatter
                ),

            repeatAnchorDate =
                anchorDate.format(
                    dateFormatter
                )
        )
    }

    /**
     * True when another occurrence exists without crossing
     * the optional Repeat Until date.
     */
    fun hasNextOccurrence(
        reminder: Reminder
    ): Boolean {

        if (!isRecurring(reminder.repeatRule)) {
            return false
        }

        val nextReminder =
            getNextOccurrence(reminder)

        return nextReminder.date !=
                reminder.date
    }

    /**
     * True when the current occurrence is the last allowed
     * occurrence in a limited recurring series.
     */
    fun isFinalRecurringOccurrence(
        reminder: Reminder
    ): Boolean {

        return isRecurring(reminder.repeatRule) &&
                !hasNextOccurrence(reminder)
    }

    /*
     * Used after a phone restart.
     *
     * If several occurrences were missed while
     * the phone was off, keep advancing until
     * the next event is genuinely in the future.
     */
    fun moveRecurringReminderToFuture(
        reminder: Reminder
    ): Reminder {

        if (!isRecurring(reminder.repeatRule)) {
            return reminder
        }

        var workingReminder =
            ensureAnchorDate(reminder)

        var eventDateTime =
            parseEventDateTime(
                workingReminder
            ) ?: return workingReminder

        val now =
            LocalDateTime.now()

        var safetyCounter = 0

        while (
            !eventDateTime.isAfter(now) &&
            safetyCounter < 10000
        ) {

            val nextReminder =
                getNextOccurrence(
                    workingReminder
                )

            if (
                nextReminder.date ==
                workingReminder.date
            ) {
                break
            }

            workingReminder =
                nextReminder

            eventDateTime =
                parseEventDateTime(
                    workingReminder
                ) ?: break

            safetyCounter++
        }

        return workingReminder
    }

    private fun ensureAnchorDate(
        reminder: Reminder
    ): Reminder {

        if (!isRecurring(reminder.repeatRule)) {
            return reminder
        }

        if (
            reminder.repeatAnchorDate != "None" &&
            parseDate(
                reminder.repeatAnchorDate
            ) != null
        ) {
            return reminder
        }

        return reminder.copy(
            repeatAnchorDate =
                reminder.date
        )
    }

    private fun resolveAnchorDate(
        reminder: Reminder
    ): LocalDate? {

        if (
            reminder.repeatAnchorDate != "None"
        ) {

            val storedAnchor =
                parseDate(
                    reminder.repeatAnchorDate
                )

            if (storedAnchor != null) {
                return storedAnchor
            }
        }

        return parseDate(
            reminder.date
        )
    }

    private fun parseRepeatUntilDate(
        reminder: Reminder
    ): LocalDate? {

        if (
            reminder.repeatUntilDate.isBlank() ||
            reminder.repeatUntilDate == "None"
        ) {
            return null
        }

        return parseDate(
            reminder.repeatUntilDate
        )
    }

    private fun isOccurrenceWithinRepeatLimit(
        reminder: Reminder
    ): Boolean {

        val repeatUntilDate =
            parseRepeatUntilDate(reminder)
                ?: return true

        val occurrenceDate =
            parseDate(reminder.date)
                ?: return false

        return !occurrenceDate
            .isAfter(repeatUntilDate)
    }

    private fun parseDate(
        value: String
    ): LocalDate? {

        return try {

            LocalDate.parse(
                value,
                dateFormatter
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    private fun parseDeadlineDateTime(
        reminder: Reminder
    ): LocalDateTime? {

        if (
            reminder.deadlineDate.isBlank() ||
            reminder.deadlineDate == "None" ||
            reminder.deadlineTime.isBlank() ||
            reminder.deadlineTime == "None"
        ) {
            return null
        }

        return try {

            val date =
                LocalDate.parse(
                    reminder.deadlineDate,
                    dateFormatter
                )

            val time =
                LocalTime.parse(
                    reminder.deadlineTime,
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

    fun isRecurring(
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

        escalationTimingCodes.forEach { (timing, _) ->

            val alarmId =
                createEscalationAlarmId(
                    reminderId,
                    timing
                )

            val reminderIntent =
                Intent(
                    context,
                    ReminderReceiver::class.java
                )

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    alarmId,
                    reminderIntent,
                    PendingIntent.FLAG_NO_CREATE or
                        PendingIntent.FLAG_IMMUTABLE
                )

            if (pendingIntent != null) {

                alarmManager.cancel(
                    pendingIntent
                )

                pendingIntent.cancel()
            }

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

            if (snoozePendingIntent != null) {

                alarmManager.cancel(
                    snoozePendingIntent
                )

                snoozePendingIntent.cancel()
            }

            notificationManager.cancel(
                alarmId
            )
        }

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

    private fun createEscalationAlarmId(
        reminderId: Int,
        timing: String
    ): Int {

        val timingCode =
            escalationTimingCodes[timing] ?: 21

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
