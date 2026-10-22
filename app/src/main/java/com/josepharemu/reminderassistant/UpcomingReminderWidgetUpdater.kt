package com.josepharemu.reminderassistant

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object UpcomingReminderWidgetUpdater {

    private val storedDateFormatter =
        DateTimeFormatter.ofPattern(
            "dd/MM/yyyy"
        )

    private val storedTimeFormatter =
        DateTimeFormatter.ofPattern(
            "HH:mm"
        )

    suspend fun updateAll(
        context: Context
    ) {

        val appWidgetManager =
            AppWidgetManager.getInstance(
                context
            )

        val componentName =
            ComponentName(
                context,
                UpcomingReminderWidget::class.java
            )

        val appWidgetIds =
            appWidgetManager
                .getAppWidgetIds(
                    componentName
                )

        if (appWidgetIds.isEmpty()) {
            return
        }

        update(
            context = context,
            appWidgetManager =
                appWidgetManager,
            appWidgetIds =
                appWidgetIds
        )
    }

    suspend fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        if (appWidgetIds.isEmpty()) {
            return
        }

        val reminders =
            ReminderDatabase
                .getDatabase(
                    context
                )
                .reminderDao()
                .getActiveRemindersOnce()

        val nextReminder =
            chooseNextReminder(
                reminders
            )

        appWidgetIds.forEach {
                appWidgetId ->

            val remoteViews =
                createRemoteViews(
                    context = context,
                    appWidgetId =
                        appWidgetId,
                    nextReminder =
                        nextReminder,
                    activeCount =
                        reminders.size
                )

            appWidgetManager
                .updateAppWidget(
                    appWidgetId,
                    remoteViews
                )
        }
    }

    private fun createRemoteViews(
        context: Context,
        appWidgetId: Int,
        nextReminder: Reminder?,
        activeCount: Int
    ): RemoteViews {

        val remoteViews =
            RemoteViews(
                context.packageName,
                R.layout.upcoming_reminder_widget
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
                appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        remoteViews.setOnClickPendingIntent(
            R.id.widget_root,
            openAppPendingIntent
        )

        if (nextReminder == null) {

            remoteViews.setTextViewText(
                R.id.widget_header,
                "ALL CAUGHT UP"
            )

            remoteViews.setTextViewText(
                R.id.widget_title,
                "No active reminders"
            )

            remoteViews.setTextViewText(
                R.id.widget_schedule,
                "Tap to open Personal Reminder Assistant"
            )

            remoteViews.setViewVisibility(
                R.id.widget_meta,
                View.GONE
            )

            remoteViews.setViewVisibility(
                R.id.widget_footer,
                View.GONE
            )

            return remoteViews
        }

        val dueDateTime =
            effectiveDateTime(
                nextReminder
            )

        val overdue =
            dueDateTime != null &&
                dueDateTime.isBefore(
                    LocalDateTime.now()
                )

        remoteViews.setTextViewText(
            R.id.widget_header,
            if (overdue) {
                "NEEDS ATTENTION"
            } else {
                "UP NEXT"
            }
        )

        remoteViews.setTextColor(
            R.id.widget_header,
            Color.parseColor(
                if (overdue) {
                    "#B42318"
                } else {
                    "#2457E6"
                }
            )
        )

        remoteViews.setTextViewText(
            R.id.widget_title,
            nextReminder.title
        )

        remoteViews.setTextViewText(
            R.id.widget_schedule,
            formatSchedule(
                dueDateTime
            )
        )

        remoteViews.setTextViewText(
            R.id.widget_meta,
            buildMetaText(
                nextReminder
            )
        )

        remoteViews.setTextColor(
            R.id.widget_meta,
            priorityColor(
                nextReminder.priority
            )
        )

        remoteViews.setViewVisibility(
            R.id.widget_meta,
            View.VISIBLE
        )

        val otherCount =
            (activeCount - 1)
                .coerceAtLeast(
                    0
                )

        remoteViews.setTextViewText(
            R.id.widget_footer,
            if (otherCount == 0) {
                "Tap to open"
            } else if (otherCount == 1) {
                "1 more active reminder • Tap to open"
            } else {
                "$otherCount more active reminders • Tap to open"
            }
        )

        remoteViews.setViewVisibility(
            R.id.widget_footer,
            View.VISIBLE
        )

        return remoteViews
    }

    private fun chooseNextReminder(
        reminders: List<Reminder>
    ): Reminder? {

        if (reminders.isEmpty()) {
            return null
        }

        return reminders
            .map { reminder ->

                reminder to
                    effectiveDateTime(
                        reminder
                    )
            }
            .sortedWith(
                compareBy<
                    Pair<
                        Reminder,
                        LocalDateTime?
                    >
                > {
                    it.second == null
                }.thenBy {
                    it.second ?: LocalDateTime.MAX
                }
            )
            .firstOrNull()
            ?.first
    }

    private fun effectiveDateTime(
        reminder: Reminder
    ): LocalDateTime? {

        if (
            reminder.type.equals(
                "Deadline",
                ignoreCase = true
            ) &&
            reminder.deadlineDate != "None" &&
            reminder.deadlineDate.isNotBlank() &&
            reminder.deadlineTime != "None" &&
            reminder.deadlineTime.isNotBlank()
        ) {

            parseDateTime(
                date = reminder.deadlineDate,
                time = reminder.deadlineTime
            )?.let {
                return it
            }
        }

        return parseDateTime(
            date = reminder.date,
            time = reminder.time
        )
    }

    private fun parseDateTime(
        date: String,
        time: String
    ): LocalDateTime? {

        return try {

            LocalDateTime.of(
                LocalDate.parse(
                    date,
                    storedDateFormatter
                ),
                LocalTime.parse(
                    time,
                    storedTimeFormatter
                )
            )

        } catch (
            exception: Exception
        ) {

            null
        }
    }

    private fun formatSchedule(
        dateTime: LocalDateTime?
    ): String {

        if (dateTime == null) {
            return "Schedule unavailable"
        }

        val today =
            LocalDate.now()

        val date =
            dateTime.toLocalDate()

        val time =
            dateTime
                .toLocalTime()
                .format(
                    storedTimeFormatter
                )

        return when {

            date.isBefore(today) ->
                "Overdue • " +
                    date.format(
                        DateTimeFormatter.ofPattern(
                            "d MMM yyyy",
                            Locale.UK
                        )
                    ) +
                    " • $time"

            date == today ->
                "Today • $time"

            date == today.plusDays(1) ->
                "Tomorrow • $time"

            date.year == today.year ->
                date.format(
                    DateTimeFormatter.ofPattern(
                        "EEE, d MMM",
                        Locale.UK
                    )
                ) +
                    " • $time"

            else ->
                date.format(
                    DateTimeFormatter.ofPattern(
                        "d MMM yyyy",
                        Locale.UK
                    )
                ) +
                    " • $time"
        }
    }

    private fun priorityColor(
        priority: String
    ): Int {

        return when (priority) {

            "Critical" ->
                Color.parseColor(
                    "#B42318"
                )

            "Important" ->
                Color.parseColor(
                    "#B26A00"
                )

            else ->
                Color.parseColor(
                    "#2457E6"
                )
        }
    }

    private fun buildMetaText(
        reminder: Reminder
    ): String {

        val typeText =
            if (
                reminder.type.equals(
                    "Deadline",
                    ignoreCase = true
                )
            ) {
                "FINAL DEADLINE"
            } else {
                reminder.type.uppercase(
                    Locale.UK
                )
            }

        return "${reminder.priority.uppercase(Locale.UK)} • $typeText"
    }
}
