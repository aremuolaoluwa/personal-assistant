package com.josepharemu.reminderassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.josepharemu.reminderassistant.ui.theme.CriticalRed
import com.josepharemu.reminderassistant.ui.theme.ImportantAmber
import com.josepharemu.reminderassistant.ui.theme.PersonalReminderAssistantTheme
import com.josepharemu.reminderassistant.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()

        setContent {

            PersonalReminderAssistantTheme {

                PersonalReminderApp()
            }
        }
    }
}

data class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun PersonalReminderApp() {

    val context =
        LocalContext.current

    val database =
        remember {

            ReminderDatabase
                .getDatabase(
                    context
                )
        }

    val coroutineScope =
        rememberCoroutineScope()

    var currentScreen
            by remember {

                mutableStateOf(
                    "home"
                )
            }

    var selectedReminder
            by remember {

                mutableStateOf<Reminder?>(
                    null
                )
            }

    val activeReminders
            by database
                .reminderDao()
                .getActiveReminders()
                .collectAsState(
                    initial =
                        emptyList()
                )

    val completedReminders
            by database
                .reminderDao()
                .getCompletedReminders()
                .collectAsState(
                    initial =
                        emptyList()
                )

    val reminderActions
            by database
                .reminderActionDao()
                .getAllActions()
                .collectAsState(
                    initial =
                        emptyList()
                )

    val destinations =
        listOf(

            MainDestination(
                route = "home",
                label = "Home",
                icon =
                    Icons.Outlined.Home
            ),

            MainDestination(
                route = "schedule",
                label = "Schedule",
                icon =
                    Icons.Outlined.EventNote
            ),

            MainDestination(
                route = "history",
                label = "History",
                icon =
                    Icons.Outlined.History
            ),

            MainDestination(
                route = "settings",
                label = "Settings",
                icon =
                    Icons.Outlined.Settings
            )
        )

    val mainRoutes =
        destinations
            .map {
                it.route
            }
            .toSet()

    fun completeReminder(
        reminder: Reminder
    ) {

        if (
            ReminderScheduler
                .isRecurring(
                    reminder.repeatRule
                )
        ) {

            coroutineScope.launch {

                database
                    .reminderActionDao()
                    .insertAction(
                        ReminderAction(
                            reminderId =
                                reminder.id,
                            actionType =
                                "DONE_TODAY",
                            occurrenceDate =
                                reminder.date,
                            details =
                                "Done for today"
                        )
                    )

                ReminderScheduler
                    .cancelReminder(
                        context,
                        reminder.id
                    )

                val nextReminder =
                    ReminderScheduler
                        .getNextOccurrence(
                            reminder
                        )

                if (
                    nextReminder.date ==
                    reminder.date
                ) {

                    /*
                     * This was the final allowed occurrence.
                     * Completing it finishes the recurring series.
                     */
                    database
                        .reminderDao()
                        .updateReminder(
                            reminder.copy(
                                isCompleted = true,
                                repeatRule = "None",
                                repeatAnchorDate = "None",
                                repeatUntilDate = "None"
                            )
                        )

                } else {

                    database
                        .reminderDao()
                        .updateReminderOccurrence(
                            reminderId =
                                reminder.id,

                            newDate =
                                nextReminder.date,

                            anchorDate =
                                nextReminder
                                    .repeatAnchorDate
                        )

                    ReminderScheduler
                        .scheduleReminder(
                            context,
                            nextReminder
                        )
                }
            }

        } else {

            ReminderScheduler
                .cancelReminder(
                    context,
                    reminder.id
                )

            coroutineScope.launch {

                val completionLabel =
                    when (reminder.type) {

                        "Task",
                        "Deadline" ->
                            "Completed"

                        else ->
                            "Done"
                    }

                database
                    .reminderActionDao()
                    .insertAction(
                        ReminderAction(
                            reminderId =
                                reminder.id,
                            actionType =
                                "COMPLETED",
                            occurrenceDate =
                                reminder.date,
                            details =
                                completionLabel
                        )
                    )

                database
                    .reminderDao()
                    .updateReminder(
                        reminder.copy(
                            isCompleted =
                                true
                        )
                    )
            }
        }
    }

    fun deleteReminder(
        reminder: Reminder
    ) {

        ReminderScheduler
            .cancelReminder(
                context,
                reminder.id
            )

        coroutineScope.launch {

            database
                .reminderDao()
                .deleteReminder(
                    reminder
                )
        }
    }

    fun endRepeat(
        reminder: Reminder
    ) {

        coroutineScope.launch {

            ReminderScheduler
                .cancelReminder(
                    context,
                    reminder.id
                )

            val oneOffReminder =
                reminder.copy(
                    repeatRule =
                        "None",

                    repeatAnchorDate =
                        "None",

                    repeatUntilDate =
                        "None"
                )

            database
                .reminderDao()
                .updateReminder(
                    oneOffReminder
                )

            ReminderScheduler
                .scheduleReminder(
                    context,
                    oneOffReminder
                )
        }
    }

    fun restoreReminder(
        reminder: Reminder
    ) {

        coroutineScope.launch {

            var restoredReminder =
                reminder.copy(
                    isCompleted =
                        false
                )

            restoredReminder =
                ReminderScheduler
                    .moveRecurringReminderToFuture(
                        restoredReminder
                    )

            database
                .reminderDao()
                .updateReminder(
                    restoredReminder
                )

            database
                .reminderActionDao()
                .insertAction(
                    ReminderAction(
                        reminderId =
                            restoredReminder.id,
                        actionType =
                            "RESTORED",
                        occurrenceDate =
                            restoredReminder.date,
                        details =
                            "Restored to active reminders"
                    )
                )

            ReminderScheduler
                .scheduleReminder(
                    context,
                    restoredReminder
                )
        }
    }

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        bottomBar = {

            if (
                currentScreen in
                mainRoutes
            ) {

                NavigationBar(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ) {

                    destinations
                        .forEach {
                                destination ->

                            NavigationBarItem(

                                selected =
                                    currentScreen ==
                                            destination.route,

                                onClick = {

                                    currentScreen =
                                        destination.route

                                    selectedReminder =
                                        null
                                },

                                icon = {

                                    Icon(
                                        imageVector =
                                            destination.icon,

                                        contentDescription =
                                            destination.label
                                    )
                                },

                                label = {

                                    Text(
                                        text =
                                            destination.label
                                    )
                                }
                            )
                        }
                }
            }
        },

        floatingActionButton = {

            if (
                currentScreen ==
                "home" ||
                currentScreen ==
                "schedule"
            ) {

                ExtendedFloatingActionButton(

                    onClick = {

                        currentScreen =
                            "addReminder"
                    },

                    icon = {

                        Icon(
                            imageVector =
                                Icons.Outlined.Add,

                            contentDescription =
                                "Add reminder"
                        )
                    },

                    text = {

                        Text(
                            text =
                                "Add reminder"
                        )
                    }
                )
            }
        }

    ) { innerPadding ->

        when (
            currentScreen
        ) {

            "home" -> {

                DashboardScreen(

                    reminders =
                        activeReminders,

                    completedCount =
                        completedReminders
                            .size,

                    onEdit = {
                            reminder ->

                        selectedReminder =
                            reminder

                        currentScreen =
                            "editReminder"
                    },

                    onDone = {
                            reminder ->

                        completeReminder(
                            reminder
                        )
                    },

                    onDelete = {
                            reminder ->

                        deleteReminder(
                            reminder
                        )
                    },

                    onEndRepeat = {
                            reminder ->

                        endRepeat(
                            reminder
                        )
                    },

                    onOpenSchedule = {

                        currentScreen =
                            "schedule"
                    },

                    modifier =
                        Modifier.padding(
                            innerPadding
                        )
                )
            }

            "schedule" -> {

                ScheduleScreen(

                    reminders =
                        activeReminders,

                    onEdit = {
                            reminder ->

                        selectedReminder =
                            reminder

                        currentScreen =
                            "editReminder"
                    },

                    onDone = {
                            reminder ->

                        completeReminder(
                            reminder
                        )
                    },

                    onDelete = {
                            reminder ->

                        deleteReminder(
                            reminder
                        )
                    },

                    onEndRepeat = {
                            reminder ->

                        endRepeat(
                            reminder
                        )
                    },

                    modifier =
                        Modifier.padding(
                            innerPadding
                        )
                )
            }

            "history" -> {

                HistoryScreen(

                    reminders =
                        completedReminders,

                    actions =
                        reminderActions,

                    allReminders =
                        activeReminders +
                            completedReminders,

                    onRestore = {
                            reminder ->

                        restoreReminder(
                            reminder
                        )
                    },

                    onDelete = {
                            reminder ->

                        deleteReminder(
                            reminder
                        )
                    },

                    modifier =
                        Modifier.padding(
                            innerPadding
                        )
                )
            }

            "settings" -> {

                SettingsScreen(
                    modifier =
                        Modifier.padding(
                            innerPadding
                        )
                )
            }

            "addReminder" -> {

                AddReminderScreen(

                    onBack = {

                        currentScreen =
                            "home"
                    },

                    modifier =
                        Modifier.padding(
                            innerPadding
                        )
                )
            }

            "editReminder" -> {

                selectedReminder
                    ?.let {
                            reminder ->

                        EditReminderScreen(

                            reminder =
                                reminder,

                            onBack = {

                                selectedReminder =
                                    null

                                currentScreen =
                                    "home"
                            },

                            modifier =
                                Modifier.padding(
                                    innerPadding
                                )
                        )
                    }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    reminders: List<Reminder>,
    completedCount: Int,
    onEdit: (Reminder) -> Unit,
    onDone: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onEndRepeat: (Reminder) -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {

    val today =
        LocalDate.now()

    val sortedReminders =
        reminders.sortedWith(
            compareBy(
                {
                    parseReminderDate(
                        it.date
                    ) ?: LocalDate.MAX
                },
                {
                    it.time
                }
            )
        )

    val todayReminders =
        sortedReminders
            .filter {

                parseReminderDate(
                    it.date
                ) == today
            }

    val overdueReminders =
        sortedReminders
            .filter { reminder ->

                val dueDate =
                    effectiveDueDate(
                        reminder
                    )

                dueDate != null &&
                        dueDate.isBefore(
                            today
                        )
            }

    val allAttentionReminders =
        sortedReminders
            .filter { reminder ->

                val overdue =
                    effectiveDueDate(
                        reminder
                    )?.isBefore(
                        today
                    ) == true

                overdue ||
                        reminder.priority ==
                        "Critical" ||
                        reminder.priority ==
                        "Important"
            }

    val attentionReminders =
        allAttentionReminders
            .take(4)

    val approachingDeadlines =
        reminders
            .filter { reminder ->

                val dueDate =
                    effectiveDueDate(
                        reminder
                    )

                reminder.type.equals(
                    "Deadline",
                    ignoreCase = true
                ) &&
                        dueDate != null &&
                        !dueDate.isBefore(
                            today
                        )
            }
            .sortedBy { reminder ->
                effectiveDueDate(
                    reminder
                ) ?: LocalDate.MAX
            }
            .take(5)

    val recurringReminders =
        sortedReminders
            .filter {

                ReminderScheduler
                    .isRecurring(
                        it.repeatRule
                    )
            }
            .take(4)

    val criticalCount =
        reminders.count {

            it.priority ==
                    "Critical"
        }

    val greeting =
        when (
            LocalTime.now().hour
        ) {

            in 5..11 ->
                "Good morning"

            in 12..16 ->
                "Good afternoon"

            else ->
                "Good evening"
        }

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = 110.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                18.dp
            )

    ) {

        item {

            Column {

                Text(
                    text =
                        greeting,

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(
                    text =
                        "Your day at a glance",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )
            }
        }

        item {

            DashboardSummaryCard(

                attentionCount =
                    allAttentionReminders
                        .size,

                todayCount =
                    todayReminders
                        .size,

                criticalCount =
                    criticalCount,

                overdueCount =
                    overdueReminders
                        .size
            )
        }

        item {

            DashboardSectionHeader(

                title =
                    "Needs your attention",

                subtitle =
                    "Important, critical and overdue items"
            )
        }

        if (
            attentionReminders
                .isEmpty()
        ) {

            item {

                DashboardEmptyCard(
                    message =
                        "Nothing urgent right now."
                )
            }

        } else {

            items(
                items =
                    attentionReminders,

                key = {
                    "attention_${it.id}"
                }

            ) {
                    reminder ->

                DashboardReminderCard(

                    reminder =
                        reminder,

                    onEdit = {
                        onEdit(
                            reminder
                        )
                    },

                    onDone = {
                        onDone(
                            reminder
                        )
                    },

                    onDelete = {
                        onDelete(
                            reminder
                        )
                    },

                    onEndRepeat = {
                        onEndRepeat(
                            reminder
                        )
                    }
                )
            }
        }

        item {

            DashboardSectionHeader(

                title =
                    "Today",

                subtitle =
                    if (
                        todayReminders
                            .isEmpty()
                    ) {

                        "Your day is clear"

                    } else {

                        "${todayReminders.size} item" +
                                if (
                                    todayReminders
                                        .size == 1
                                ) {
                                    ""
                                } else {
                                    "s"
                                } +
                                " scheduled"
                    }
            )
        }

        if (
            todayReminders
                .isEmpty()
        ) {

            item {

                DashboardEmptyCard(
                    message =
                        "No reminders scheduled for today."
                )
            }

        } else {

            items(
                items =
                    todayReminders,

                key = {
                    "today_${it.id}"
                }

            ) {
                    reminder ->

                DashboardReminderCard(

                    reminder =
                        reminder,

                    onEdit = {
                        onEdit(
                            reminder
                        )
                    },

                    onDone = {
                        onDone(
                            reminder
                        )
                    },

                    onDelete = {
                        onDelete(
                            reminder
                        )
                    },

                    onEndRepeat = {
                        onEndRepeat(
                            reminder
                        )
                    }
                )
            }
        }

        item {

            DashboardSectionHeader(

                title =
                    "Approaching deadlines",

                subtitle =
                    "Keep important dates visible",

                actionText =
                    "View schedule",

                onAction =
                    onOpenSchedule
            )
        }

        if (
            approachingDeadlines
                .isEmpty()
        ) {

            item {

                DashboardEmptyCard(
                    message =
                        "No upcoming deadlines."
                )
            }

        } else {

            items(
                items =
                    approachingDeadlines,

                key = {
                    "deadline_${it.id}"
                }

            ) {
                    reminder ->

                DeadlineRow(
                    reminder =
                        reminder,

                    onClick = {
                        onEdit(
                            reminder
                        )
                    }
                )
            }
        }

        if (
            recurringReminders
                .isNotEmpty()
        ) {

            item {

                DashboardSectionHeader(

                    title =
                        "Ongoing commitments",

                    subtitle =
                        "Reminders that keep returning"
                )
            }

            items(
                items =
                    recurringReminders,

                key = {
                    "recurring_${it.id}"
                }

            ) {
                    reminder ->

                OngoingReminderRow(
                    reminder =
                        reminder,

                    onClick = {
                        onEdit(
                            reminder
                        )
                    }
                )
            }
        }

        item {

            DashboardProgressCard(
                completedCount =
                    completedCount
            )
        }
    }
}

@Composable
fun DashboardSummaryCard(
    attentionCount: Int,
    todayCount: Int,
    criticalCount: Int,
    overdueCount: Int
) {

    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    MaterialTheme
                        .colorScheme
                        .primary,

                    Color(
                        0xFF3B6CF4
                    )
                )
        )

    Surface(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                26.dp
            ),

        color =
            Color.Transparent
    ) {

        Column(
            modifier =
                Modifier
                    .background(
                        gradient
                    )
                    .padding(
                        22.dp
                    )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape =
                        CircleShape,

                    color =
                        Color.White
                            .copy(
                                alpha =
                                    0.16f
                            )
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .NotificationsActive,

                        contentDescription =
                            null,

                        tint =
                            Color.White,

                        modifier =
                            Modifier.padding(
                                10.dp
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.size(
                            12.dp
                        )
                )

                Column {

                    Text(
                        text =
                            "$attentionCount items need attention",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        color =
                            Color.White
                    )

                    Text(
                        text =
                            "Stay ahead of what matters.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        color =
                            Color.White
                                .copy(
                                    alpha =
                                        0.82f
                                )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        22.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                SummaryMetric(
                    label =
                        "Today",

                    value =
                        todayCount
                            .toString(),

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                SummaryMetric(
                    label =
                        "Critical",

                    value =
                        criticalCount
                            .toString(),

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                SummaryMetric(
                    label =
                        "Overdue",

                    value =
                        overdueCount
                            .toString(),

                    modifier =
                        Modifier.weight(
                            1f
                        )
                )
            }
        }
    }
}

@Composable
fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Surface(
        modifier =
            modifier,

        shape =
            RoundedCornerShape(
                16.dp
            ),

        color =
            Color.White
                .copy(
                    alpha =
                        0.12f
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    12.dp
                )
        ) {

            Text(
                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge,

                color =
                    Color.White
            )

            Text(
                text =
                    label,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    Color.White
                        .copy(
                            alpha =
                                0.78f
                        )
            )
        }
    }
}

@Composable
fun DashboardSectionHeader(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.SpaceBetween,

        verticalAlignment =
            Alignment.Bottom
    ) {

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            Text(
                text =
                    subtitle,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        if (
            actionText != null &&
            onAction != null
        ) {

            TextButton(
                onClick =
                    onAction
            ) {

                Text(
                    text =
                        actionText
                )
            }
        }
    }
}

@Composable
fun DashboardReminderCard(
    reminder: Reminder,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    onEndRepeat: () -> Unit
) {

    val recurring =
        ReminderScheduler
            .isRecurring(
                reminder.repeatRule
            )

    val priorityColor =
        priorityColor(
            reminder.priority
        )

    val isDeadline =
        reminder.type.equals(
            "Deadline",
            ignoreCase = true
        ) &&
                reminder.deadlineDate != "None" &&
                reminder.deadlineDate.isNotBlank()

    val finalDeadlineDate =
        if (isDeadline) {
            parseReminderDate(
                reminder.deadlineDate
            )
        } else {
            null
        }

    val deadlineDays =
        finalDeadlineDate?.let { date ->
            ChronoUnit.DAYS.between(
                LocalDate.now(),
                date
            )
        }

    val nextAlertDateTime =
        nextScheduledAlertDateTime(
            reminder
        )

    val deadlineOccurrenceDiffers =
        isDeadline &&
                (
                    reminder.date !=
                            reminder.deadlineDate ||
                            reminder.time !=
                            reminder.deadlineTime
                    )

    val showOccurrence =
        !isDeadline ||
                recurring ||
                deadlineOccurrenceDiffers

    val occurrenceLabel =
        when {
            isDeadline && recurring ->
                "Next recurring reminder"

            isDeadline ->
                "Reminder schedule"

            reminder.type.equals(
                "Event",
                ignoreCase = true
            ) ->
                if (recurring) {
                    "Next event"
                } else {
                    "Event"
                }

            reminder.type.equals(
                "Task",
                ignoreCase = true
            ) ->
                if (recurring) {
                    "Next task"
                } else {
                    "Task"
                }

            recurring ->
                "Next occurrence"

            else ->
                "Scheduled for"
        }

    ElevatedCard(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults
                .elevatedCardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.Top
            ) {

                Row(
                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    10.dp
                                )
                                .background(
                                    color =
                                        priorityColor,

                                    shape =
                                        CircleShape
                                )
                    )

                    Spacer(
                        modifier =
                            Modifier.size(
                                10.dp
                            )
                    )

                    Text(
                        text =
                            reminder.title,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        maxLines =
                            2,

                        overflow =
                            TextOverflow.Ellipsis,

                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.size(
                            8.dp
                        )
                )

                PriorityChip(
                    priority =
                        reminder.priority
                )
            }

            if (showOccurrence) {

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                Text(
                    text =
                        occurrenceLabel,

                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )

                Text(
                    text =
                        "${relativeReminderDateLabel(reminder.date)} • ${reminder.time}",

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    fontWeight =
                        FontWeight.SemiBold
                )
            }

            if (isDeadline) {

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(
                                alpha = 0.55f
                            )
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    14.dp
                                ),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            Text(
                                text =
                                    "Final deadline",

                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge,

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        3.dp
                                    )
                            )

                            Text(
                                text =
                                    formatReminderDate(
                                        reminder.deadlineDate
                                    ) +
                                            if (
                                                reminder.deadlineTime != "None" &&
                                                reminder.deadlineTime.isNotBlank()
                                            ) {
                                                " • ${reminder.deadlineTime}"
                                            } else {
                                                ""
                                            },

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyLarge,

                                fontWeight =
                                    FontWeight.SemiBold
                            )
                        }

                        if (deadlineDays != null) {

                            Spacer(
                                modifier =
                                    Modifier.size(
                                        10.dp
                                    )
                            )

                            Surface(
                                shape =
                                    RoundedCornerShape(
                                        50.dp
                                    ),

                                color =
                                    if (
                                        deadlineDays <= 3
                                    ) {
                                        CriticalRed.copy(
                                            alpha = 0.12f
                                        )
                                    } else {
                                        MaterialTheme
                                            .colorScheme
                                            .surface
                                    }
                            ) {

                                Text(
                                    text =
                                        deadlineText(
                                            deadlineDays
                                        ),

                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelLarge,

                                    color =
                                        if (
                                            deadlineDays <= 3
                                        ) {
                                            CriticalRed
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        },

                                    modifier =
                                        Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 6.dp
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    "Next alert",

                style =
                    MaterialTheme
                        .typography
                        .labelLarge,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            Text(
                text =
                    if (
                        nextAlertDateTime != null
                    ) {
                        formatAlertDateTime(
                            nextAlertDateTime
                        )
                    } else {
                        "No future alert scheduled"
                    },

                style =
                    MaterialTheme
                        .typography
                        .bodyLarge,

                fontWeight =
                    FontWeight.SemiBold,

                color =
                    if (
                        nextAlertDateTime != null
                    ) {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    }
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                SmallInfoChip(
                    text =
                        reminder.type
                )

                if (
                    reminder.category
                        .isNotBlank()
                ) {

                    SmallInfoChip(
                        text =
                            reminder.category
                    )
                }

                if (recurring) {

                    SmallInfoChip(
                        text =
                            reminder.repeatRule,

                        icon =
                            Icons
                                .Outlined
                                .Repeat
                    )
                }
            }

            if (recurring) {

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Text(
                    text =
                        recurrenceSummaryText(
                            reminder
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            if (
                reminder.notes
                    .isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Text(
                    text =
                        reminder.notes,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines =
                        2,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                FilledTonalButton(
                    onClick =
                        onDone,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .CheckCircle,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                18.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.size(
                                6.dp
                            )
                    )

                    Text(
                        text =
                            if (recurring) {
                                "Done today"
                            } else {
                                "Done"
                            }
                    )
                }

                OutlinedButton(
                    onClick =
                        onEdit,

                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .Edit,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                18.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.size(
                                6.dp
                            )
                    )

                    Text(
                        text =
                            "Edit"
                    )
                }
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                if (recurring) {

                    TextButton(
                        onClick =
                            onEndRepeat
                    ) {

                        Text(
                            text =
                                "End repeat"
                        )
                    }

                } else {

                    Spacer(
                        modifier =
                            Modifier
                                .height(
                                    1.dp
                                )
                    )
                }

                TextButton(
                    onClick =
                        onDelete
                ) {

                    Icon(
                        imageVector =
                            Icons
                                .Outlined
                                .Delete,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.size(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "Delete"
                    )
                }
            }
        }
    }
}

@Composable
fun DeadlineRow(
    reminder: Reminder,
    onClick: () -> Unit
) {

    val date =
        effectiveDueDate(
            reminder
        )

    val deadlineTime =
        if (
            reminder.deadlineTime != "None" &&
            reminder.deadlineTime.isNotBlank()
        ) {
            reminder.deadlineTime
        } else {
            reminder.time
        }

    val today =
        LocalDate.now()

    val days =
        if (date != null) {

            ChronoUnit.DAYS
                .between(
                    today,
                    date
                )

        } else {

            0L
        }

    ElevatedCard(

        onClick =
            onClick,

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            horizontal =
                                12.dp,

                            vertical =
                                8.dp
                        ),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            date
                                ?.dayOfMonth
                                ?.toString()
                                ?: "--",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Text(
                        text =
                            date
                                ?.month
                                ?.name
                                ?.take(
                                    3
                                )
                                ?: "",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        reminder.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    maxLines =
                        1,

                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(
                    text =
                        "${deadlineText(days)} • $deadlineTime",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        if (
                            days <= 3
                        ) {

                            CriticalRed

                        } else {

                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        }
                )
            }

            PriorityChip(
                priority =
                    reminder.priority
            )
        }
    }
}

@Composable
fun OngoingReminderRow(
    reminder: Reminder,
    onClick: () -> Unit
) {

    ElevatedCard(

        onClick =
            onClick,

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        16.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .Repeat,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.padding(
                            10.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        reminder.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    text =
                        buildString {
                            append(
                                "${reminder.repeatRule} • Next ${formatReminderDate(reminder.date)} at ${reminder.time}"
                            )

                            if (
                                reminder.repeatUntilDate != "None" &&
                                reminder.repeatUntilDate.isNotBlank()
                            ) {
                                append(
                                    " • Until ${formatReminderDate(reminder.repeatUntilDate)}"
                                )
                            }
                        },

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DashboardProgressCard(
    completedCount: Int
) {

    ElevatedCard(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        18.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape =
                    CircleShape,

                color =
                    SuccessGreen
                        .copy(
                            alpha =
                                0.12f
                        )
            ) {

                Icon(
                    imageVector =
                        Icons
                            .Outlined
                            .CheckCircle,

                    contentDescription =
                        null,

                    tint =
                        SuccessGreen,

                    modifier =
                        Modifier.padding(
                            10.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )

            Column {

                Text(
                    text =
                        "$completedCount completed",

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Text(
                    text =
                        "Your completed reminders are saved in History.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DashboardEmptyCard(
    message: String
) {

    Surface(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(
                    alpha =
                        0.55f
                )
    ) {

        Text(
            text =
                message,

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

            modifier =
                Modifier.padding(
                    18.dp
                )
        )
    }
}

@Composable
fun PriorityChip(
    priority: String
) {

    val color =
        priorityColor(
            priority
        )

    val background =
        color.copy(
            alpha =
                0.12f
        )

    Surface(
        shape =
            RoundedCornerShape(
                50.dp
            ),

        color =
            background
    ) {

        Text(
            text =
                priority,

            style =
                MaterialTheme
                    .typography
                    .labelLarge,

            color =
                color,

            modifier =
                Modifier.padding(
                    horizontal =
                        10.dp,

                    vertical =
                        5.dp
                )
        )
    }
}

@Composable
fun SmallInfoChip(
    text: String,
    icon: ImageVector? = null
) {

    Surface(
        shape =
            RoundedCornerShape(
                50.dp
            ),

        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
    ) {

        Row(
            modifier =
                Modifier.padding(
                    horizontal =
                        10.dp,

                    vertical =
                        5.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (
                icon != null
            ) {

                Icon(
                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(
                            14.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.size(
                            4.dp
                        )
                )
            }

            Text(
                text =
                    text,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }
    }
}

@Composable
fun ScheduleScreen(
    reminders: List<Reminder>,
    onEdit: (Reminder) -> Unit,
    onDone: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onEndRepeat: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {

    val sorted =
        reminders.sortedWith(
            compareBy(
                {
                    parseReminderDate(
                        it.date
                    ) ?: LocalDate.MAX
                },
                {
                    it.time
                }
            )
        )

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 22.dp,
                bottom = 110.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )

    ) {

        item {

            Column {

                Text(
                    text =
                        "Schedule",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )

                Text(
                    text =
                        "Everything coming up, in one place.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        if (
            sorted.isEmpty()
        ) {

            item {

                DashboardEmptyCard(
                    message =
                        "Nothing scheduled yet."
                )
            }

        } else {

            items(
                items =
                    sorted,

                key = {
                    it.id
                }

            ) {
                    reminder ->

                DashboardReminderCard(

                    reminder =
                        reminder,

                    onEdit = {
                        onEdit(
                            reminder
                        )
                    },

                    onDone = {
                        onDone(
                            reminder
                        )
                    },

                    onDelete = {
                        onDelete(
                            reminder
                        )
                    },

                    onEndRepeat = {
                        onEndRepeat(
                            reminder
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    reminders: List<Reminder>,
    actions: List<ReminderAction>,
    allReminders: List<Reminder>,
    onRestore: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {

    val reminderLookup =
        allReminders.associateBy {
            it.id
        }

    val visibleActions =
        actions
            .filter {
                reminderLookup.containsKey(
                    it.reminderId
                )
            }
            .take(50)

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 22.dp,
                bottom = 100.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )

    ) {

        item {

            Column {

                Text(
                    text =
                        "History",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )

                Text(
                    text =
                        "Completed reminders and the actions you took along the way.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        item {

            DashboardSectionHeader(
                title =
                    "Recent activity",
                subtitle =
                    if (visibleActions.isEmpty()) {
                        "Your reminder activity will appear here"
                    } else {
                        "${visibleActions.size} recent action" +
                            if (visibleActions.size == 1) {
                                ""
                            } else {
                                "s"
                            }
                    }
            )
        }

        if (visibleActions.isEmpty()) {

            item {

                DashboardEmptyCard(
                    message =
                        "No reminder activity recorded yet."
                )
            }

        } else {

            items(
                items =
                    visibleActions,
                key = {
                    "action_${it.id}"
                }
            ) { action ->

                val reminder =
                    reminderLookup[
                        action.reminderId
                    ]

                if (reminder != null) {

                    ReminderActionHistoryCard(
                        action =
                            action,
                        reminder =
                            reminder
                    )
                }
            }
        }

        item {

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            DashboardSectionHeader(
                title =
                    "Completed reminders",
                subtitle =
                    if (reminders.isEmpty()) {
                        "Nothing completed yet"
                    } else {
                        "${reminders.size} completed"
                    }
            )
        }

        if (reminders.isEmpty()) {

            item {

                DashboardEmptyCard(
                    message =
                        "Completed reminders will appear here."
                )
            }

        } else {

            items(
                items =
                    reminders,
                key = {
                    "completed_${it.id}"
                }
            ) { reminder ->

                ElevatedCard(

                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            20.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                18.dp
                            )
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons
                                        .Outlined
                                        .CheckCircle,

                                contentDescription =
                                    null,

                                tint =
                                    SuccessGreen
                            )

                            Spacer(
                                modifier =
                                    Modifier.size(
                                        10.dp
                                    )
                            )

                            Column(
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(
                                    text =
                                        reminder.title,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    text =
                                        "${formatReminderDate(reminder.date)} • ${reminder.time}",

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyMedium,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )

                                if (
                                    reminder.snoozeCount > 0
                                ) {

                                    Spacer(
                                        modifier =
                                            Modifier.height(
                                                4.dp
                                            )
                                    )

                                    Text(
                                        text =
                                            "Snoozed ${reminder.snoozeCount} time" +
                                                if (
                                                    reminder.snoozeCount == 1
                                                ) {
                                                    ""
                                                } else {
                                                    "s"
                                                },

                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(
                                    14.dp
                                )
                        )

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp
                                )
                        ) {

                            Button(
                                onClick = {
                                    onRestore(
                                        reminder
                                    )
                                },

                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(
                                    text =
                                        "Restore"
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onDelete(
                                        reminder
                                    )
                                },

                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            ) {

                                Text(
                                    text =
                                        "Delete"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderActionHistoryCard(
    action: ReminderAction,
    reminder: Reminder
) {

    val icon =
        when (action.actionType) {

            "NOTIFIED",
            "FOLLOW_UP_SENT" ->
                Icons.Outlined.NotificationsActive

            "SNOOZED" ->
                Icons.Outlined.Schedule

            "COMPLETED",
            "DONE_TODAY" ->
                Icons.Outlined.CheckCircle

            "RESTORED" ->
                Icons.Outlined.History

            else ->
                Icons.Outlined.WarningAmber
        }

    val tint =
        when (action.actionType) {

            "COMPLETED",
            "DONE_TODAY" ->
                SuccessGreen

            "SNOOZED",
            "KEPT_ACTIVE" ->
                ImportantAmber

            else ->
                MaterialTheme
                    .colorScheme
                    .primary
        }

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalAlignment =
                Alignment.Top
        ) {

            Surface(
                shape =
                    CircleShape,
                color =
                    tint.copy(
                        alpha = 0.12f
                    )
            ) {

                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        null,
                    tint =
                        tint,
                    modifier =
                        Modifier.padding(
                            9.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.size(
                        12.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        reminder.title,
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    maxLines =
                        2,
                    overflow =
                        TextOverflow.Ellipsis
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(
                    text =
                        reminderActionLabel(
                            action
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,
                    fontWeight =
                        FontWeight.SemiBold,
                    color =
                        tint
                )

                reminderActionDetails(
                    action
                )?.let { details ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )

                    Text(
                        text =
                            details,
                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )

                Text(
                    text =
                        formatReminderActionTime(
                            action.timestamp
                        ),
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

private fun reminderActionLabel(
    action: ReminderAction
): String {

    return when (action.actionType) {

        "NOTIFIED" ->
            "Notification sent"

        "FOLLOW_UP_SENT" ->
            "Follow-up sent"

        "SNOOZED" ->
            "Snoozed"

        "KEPT_ACTIVE" ->
            action.details
                ?: "Kept active"

        "COMPLETED" ->
            action.details
                ?: "Completed"

        "DONE_TODAY" ->
            "Done for today"

        "RESTORED" ->
            "Restored"

        else ->
            action.actionType
                .replace(
                    "_",
                    " "
                )
                .lowercase(
                    Locale.UK
                )
                .replaceFirstChar {
                    it.uppercase()
                }
    }
}

private fun reminderActionDetails(
    action: ReminderAction
): String? {

    return when (action.actionType) {

        "KEPT_ACTIVE",
        "COMPLETED" ->
            null

        else ->
            action.details
                ?.takeIf {
                    it.isNotBlank()
                }
    }
}

private fun formatReminderActionTime(
    timestamp: Long
): String {

    return Instant
        .ofEpochMilli(
            timestamp
        )
        .atZone(
            ZoneId.systemDefault()
        )
        .format(
            DateTimeFormatter.ofPattern(
                "d MMM yyyy • HH:mm",
                Locale.UK
            )
        )
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {

    LazyColumn(

        modifier =
            modifier
                .fillMaxSize(),

        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 22.dp,
                bottom = 100.dp
            ),

        verticalArrangement =
            Arrangement.spacedBy(
                16.dp
            )

    ) {

        item {

            Column {

                Text(
                    text =
                        "Settings",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium
                )

                Text(
                    text =
                        "Personalise how your assistant works.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        item {

            SettingsInfoCard(

                icon =
                    Icons
                        .Outlined
                        .NotificationsActive,

                title =
                    "Notifications",

                description =
                    "Reminder alerts use your phone's notification and alarm settings."
            )
        }

        item {

            SettingsInfoCard(

                icon =
                    Icons
                        .Outlined
                        .Schedule,

                title =
                    "Offline first",

                description =
                    "Your reminders are stored locally and continue working without Internet access."
            )
        }

        item {

            SettingsInfoCard(

                icon =
                    Icons
                        .Outlined
                        .History,

                title =
                    "History",

                description =
                    "Completed reminders remain available so you can review or restore them."
            )
        }

        item {

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        20.dp
                    ),

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            18.dp
                        )
                ) {

                    Text(
                        text =
                            "Personal Reminder Assistant",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "Remember. Follow up. Complete.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsInfoCard(
    icon: ImageVector,
    title: String,
    description: String
) {

    ElevatedCard(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    18.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape =
                    CircleShape,

                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {

                Icon(
                    imageVector =
                        icon,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.padding(
                            10.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )

            Column {

                Text(
                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(
                    text =
                        description,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

private fun parseReminderDate(
    value: String
): LocalDate? {

    return try {

        LocalDate.parse(
            value,
            DateTimeFormatter
                .ofPattern(
                    "dd/MM/yyyy"
                )
        )

    } catch (
        exception: Exception
    ) {

        null
    }
}

private fun formatReminderDate(
    value: String
): String {

    val date =
        parseReminderDate(
            value
        )

    return if (
        date != null
    ) {

        date.format(
            DateTimeFormatter
                .ofPattern(
                    "d MMM yyyy",
                    Locale.UK
                )
        )

    } else {

        value
    }
}

private fun relativeReminderDateLabel(
    value: String
): String {

    val date =
        parseReminderDate(
            value
        ) ?: return value

    val days =
        ChronoUnit.DAYS.between(
            LocalDate.now(),
            date
        )

    return when (days) {

        0L ->
            "Today"

        1L ->
            "Tomorrow"

        else ->
            formatReminderDate(
                value
            )
    }
}

private fun recurrenceSummaryText(
    reminder: Reminder
): String {

    val repeatName =
        reminder.repeatRule
            .lowercase(
                Locale.UK
            )

    val repeatUntil =
        reminder.repeatUntilDate

    return if (
        repeatUntil != "None" &&
        repeatUntil.isNotBlank() &&
        parseReminderDate(
            repeatUntil
        ) != null
    ) {

        "Repeats $repeatName until ${formatReminderDate(repeatUntil)}"

    } else {

        "Repeats $repeatName"
    }
}

private fun effectiveDueDate(
    reminder: Reminder
): LocalDate? {

    if (
        reminder.type.equals(
            "Deadline",
            ignoreCase = true
        ) &&
        reminder.deadlineDate != "None" &&
        reminder.deadlineDate.isNotBlank()
    ) {

        val finalDeadline =
            parseReminderDate(
                reminder.deadlineDate
            )

        if (finalDeadline != null) {
            return finalDeadline
        }
    }

    return parseReminderDate(
        reminder.date
    )
}

private fun nextScheduledAlertDateTime(
    reminder: Reminder
): LocalDateTime? {

    val eventDate =
        parseReminderDate(
            reminder.date
        ) ?: return null

    val eventTime =
        try {

            LocalTime.parse(
                reminder.time,
                DateTimeFormatter
                    .ofPattern(
                        "HH:mm"
                    )
            )

        } catch (
            exception: Exception
        ) {

            return null
        }

    val eventDateTime =
        LocalDateTime.of(
            eventDate,
            eventTime
        )

    val now =
        LocalDateTime.now()

    return parseReminderTimings(
        reminder.reminderTiming
    )
        .map { timing ->

            when (timing) {

                "10 minutes before" ->
                    eventDateTime
                        .minusMinutes(
                            10
                        )

                "30 minutes before" ->
                    eventDateTime
                        .minusMinutes(
                            30
                        )

                "1 hour before" ->
                    eventDateTime
                        .minusHours(
                            1
                        )

                "2 hours before" ->
                    eventDateTime
                        .minusHours(
                            2
                        )

                "1 day before" ->
                    eventDateTime
                        .minusDays(
                            1
                        )

                "3 days before" ->
                    eventDateTime
                        .minusDays(
                            3
                        )

                "1 week before" ->
                    eventDateTime
                        .minusWeeks(
                            1
                        )

                else ->
                    eventDateTime
            }
        }
        .filter {
            it.isAfter(
                now
            )
        }
        .minOrNull()
}

private fun formatAlertDateTime(
    value: LocalDateTime
): String {

    val storedDate =
        value
            .toLocalDate()
            .format(
                DateTimeFormatter
                    .ofPattern(
                        "dd/MM/yyyy"
                    )
            )

    val time =
        value
            .toLocalTime()
            .format(
                DateTimeFormatter
                    .ofPattern(
                        "HH:mm"
                    )
            )

    return "${relativeReminderDateLabel(storedDate)} • $time"
}

private fun deadlineText(
    days: Long
): String {

    return when {

        days < 0 -> {

            val overdueDays =
                -days

            if (
                overdueDays == 1L
            ) {
                "Overdue by 1 day"
            } else {
                "Overdue by $overdueDays days"
            }
        }

        days == 0L ->
            "Due today"

        days == 1L ->
            "Due tomorrow"

        else ->
            "$days days left"
    }
}

private fun priorityColor(
    priority: String
): Color {

    return when (
        priority
    ) {

        "Critical" ->
            CriticalRed

        "Important" ->
            ImportantAmber

        else ->
            Color(
                0xFF0B43E6
            )
    }
}
