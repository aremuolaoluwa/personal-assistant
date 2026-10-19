package com.josepharemu.reminderassistant
import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

val reminderFormDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")

val reminderFormTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

fun isReminderDateTimeOnOrAfter(
    laterDate: String,
    laterTime: String,
    earlierDate: String,
    earlierTime: String
): Boolean {

    return try {

        val later =
            LocalDateTime.of(
                LocalDate.parse(
                    laterDate,
                    reminderFormDateFormatter
                ),
                LocalTime.parse(
                    laterTime,
                    reminderFormTimeFormatter
                )
            )

        val earlier =
            LocalDateTime.of(
                LocalDate.parse(
                    earlierDate,
                    reminderFormDateFormatter
                ),
                LocalTime.parse(
                    earlierTime,
                    reminderFormTimeFormatter
                )
            )

        !later.isBefore(earlier)

    } catch (exception: Exception) {

        false
    }
}

fun isReminderDateOnOrAfter(
    laterDate: String,
    earlierDate: String
): Boolean {

    return try {

        val later =
            LocalDate.parse(
                laterDate,
                reminderFormDateFormatter
            )

        val earlier =
            LocalDate.parse(
                earlierDate,
                reminderFormDateFormatter
            )

        !later.isBefore(earlier)

    } catch (exception: Exception) {

        false
    }
}

@Composable
fun AddReminderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val database =
        remember {
            ReminderDatabase.getDatabase(context)
        }

    val coroutineScope =
        rememberCoroutineScope()

    var title by remember {
        mutableStateOf("")
    }

    var notes by remember {
        mutableStateOf("")
    }

    var selectedType by remember {
        mutableStateOf("Reminder")
    }

    var selectedDate by remember {
        mutableStateOf("")
    }

    var selectedTime by remember {
        mutableStateOf("")
    }

    var selectedPriority by remember {
        mutableStateOf("Normal")
    }

    var selectedCategory by remember {
        mutableStateOf("Personal")
    }

    var selectedRepeatRule by remember {
        mutableStateOf("None")
    }

    var selectedRepeatUntilDate by remember {
        mutableStateOf("")
    }

    var selectedDeadlineDate by remember {
        mutableStateOf("")
    }

    var selectedDeadlineTime by remember {
        mutableStateOf("")
    }

    var selectedReminderTimings by remember {
        mutableStateOf(
            setOf("At time of event")
        )
    }

    var automaticEscalationEnabled by remember {
        mutableStateOf(false)
    }

    val calendar =
        Calendar.getInstance()

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                Toast.makeText(
                    context,
                    "Notification permission granted. Tap Save Reminder again.",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                Toast.makeText(
                    context,
                    "Notifications must be allowed for reminders to alert you.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 40.dp
            )
    ) {

        ReminderFormHeader(
            title = "New reminder",
            subtitle = "Create something you do not want to forget.",
            onBack = onBack
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        ReminderFormSection(
            icon =
                Icons.Outlined.Description,
            title =
                "Details",
            subtitle =
                "What do you need to remember?"
        ) {

            FormLabel(
                text = "Title"
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                placeholder = {
                    Text(
                        text =
                            "e.g. Submit scholarship application"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true,
                shape =
                    RoundedCornerShape(14.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            FormLabel(
                text = "Type"
            )

            SimpleDropdown(
                selectedValue =
                    selectedType,
                options = listOf(
                    "Event",
                    "Deadline",
                    "Task",
                    "Reminder"
                ),
                onValueSelected = {
                    selectedType = it
                }
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            FormLabel(
                text = "Priority"
            )

            SimpleDropdown(
                selectedValue =
                    selectedPriority,
                options = listOf(
                    "Normal",
                    "Important",
                    "Critical"
                ),
                onValueSelected = {
                    selectedPriority = it
                }
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            FormLabel(
                text = "Category"
            )

            SimpleDropdown(
                selectedValue =
                    selectedCategory,
                options = listOf(
                    "Personal",
                    "Work",
                    "Education",
                    "Applications",
                    "Appointments",
                    "Travel",
                    "Finance",
                    "Birthdays",
                    "Other"
                ),
                onValueSelected = {
                    selectedCategory = it
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        ReminderFormSection(
            icon =
                Icons.Outlined.Schedule,
            title =
                "Schedule",
            subtitle =
                when (selectedType) {
                    "Event" ->
                        "Set the actual date and time of the event."

                    "Deadline" ->
                        "Set when the reminder schedule starts and the actual final deadline."

                    "Task" ->
                        "Set when the task is due."

                    else ->
                        "Set when this reminder is scheduled."
                }
        ) {

            FormLabel(
                text =
                    when (selectedType) {
                        "Event" ->
                            "Event date"

                        "Deadline" ->
                            "Reminder schedule date"

                        "Task" ->
                            "Task date"

                        else ->
                            "Reminder date"
                    }
            )

            FormPickerButton(
                icon =
                    Icons.Outlined.CalendarMonth,
                text =
                    selectedDate,
                emptyText =
                    "Choose date",
                onClick = {

                    DatePickerDialog(
                        context,
                        { _, year, month, day ->

                            selectedDate =
                                "%02d/%02d/%04d".format(
                                    day,
                                    month + 1,
                                    year
                                )
                        },
                        calendar.get(
                            Calendar.YEAR
                        ),
                        calendar.get(
                            Calendar.MONTH
                        ),
                        calendar.get(
                            Calendar.DAY_OF_MONTH
                        )
                    ).show()
                }
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            FormLabel(
                text =
                    when (selectedType) {
                        "Event" ->
                            "Event time"

                        "Deadline" ->
                            "Reminder schedule time"

                        "Task" ->
                            "Task time"

                        else ->
                            "Reminder time"
                    }
            )

            FormPickerButton(
                icon =
                    Icons.Outlined.Timer,
                text =
                    selectedTime,
                emptyText =
                    "Choose time",
                onClick = {

                    TimePickerDialog(
                        context,
                        { _, hour, minute ->

                            selectedTime =
                                "%02d:%02d".format(
                                    hour,
                                    minute
                                )
                        },
                        calendar.get(
                            Calendar.HOUR_OF_DAY
                        ),
                        calendar.get(
                            Calendar.MINUTE
                        ),
                        true
                    ).show()
                }
            )

            if (
                selectedType ==
                "Deadline"
            ) {

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )

                Text(
                    text =
                        "Final deadline",
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "This is the actual date and time the work must be completed.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                FormLabel(
                    text =
                        "Deadline date"
                )

                FormPickerButton(
                    icon =
                        Icons.Outlined.CalendarMonth,
                    text =
                        selectedDeadlineDate,
                    emptyText =
                        "Choose final deadline",
                    onClick = {

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->

                                selectedDeadlineDate =
                                    "%02d/%02d/%04d".format(
                                        day,
                                        month + 1,
                                        year
                                    )

                                if (
                                    ReminderScheduler
                                        .isRecurring(
                                            selectedRepeatRule
                                        ) &&
                                    selectedRepeatUntilDate
                                        .isBlank()
                                ) {

                                    selectedRepeatUntilDate =
                                        selectedDeadlineDate
                                }
                            },
                            calendar.get(
                                Calendar.YEAR
                            ),
                            calendar.get(
                                Calendar.MONTH
                            ),
                            calendar.get(
                                Calendar.DAY_OF_MONTH
                            )
                        ).show()
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                FormLabel(
                    text =
                        "Deadline time"
                )

                FormPickerButton(
                    icon =
                        Icons.Outlined.Timer,
                    text =
                        selectedDeadlineTime,
                    emptyText =
                        "Choose deadline time",
                    onClick = {

                        TimePickerDialog(
                            context,
                            { _, hour, minute ->

                                selectedDeadlineTime =
                                    "%02d:%02d".format(
                                        hour,
                                        minute
                                    )
                            },
                            calendar.get(
                                Calendar.HOUR_OF_DAY
                            ),
                            calendar.get(
                                Calendar.MINUTE
                            ),
                            true
                        ).show()
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        ReminderFormSection(
            icon =
                Icons.Outlined.Repeat,
            title =
                "Repeat",
            subtitle =
                "Choose whether this reminder should return."
        ) {

            FormLabel(
                text = "Repeat"
            )

            SimpleDropdown(
                selectedValue =
                    selectedRepeatRule,
                options = listOf(
                    "None",
                    "Daily",
                    "Weekly",
                    "Monthly",
                    "Yearly"
                ),
                onValueSelected = {

                    selectedRepeatRule =
                        it

                    if (
                        it == "None"
                    ) {

                        selectedRepeatUntilDate =
                            ""

                    } else if (
                        selectedType ==
                        "Deadline" &&
                        selectedDeadlineDate
                            .isNotBlank() &&
                        selectedRepeatUntilDate
                            .isBlank()
                    ) {

                        selectedRepeatUntilDate =
                            selectedDeadlineDate
                    }
                }
            )

            if (
                ReminderScheduler
                    .isRecurring(
                        selectedRepeatRule
                    )
            ) {

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )

                FormLabel(
                    text =
                        "Repeat until"
                )

                FormPickerButton(
                    icon =
                        Icons.Outlined.CalendarMonth,
                    text =
                        selectedRepeatUntilDate,
                    emptyText =
                        if (
                            selectedType ==
                            "Deadline"
                        ) {
                            "Defaults to final deadline"
                        } else {
                            "No end date"
                        },
                    onClick = {

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->

                                selectedRepeatUntilDate =
                                    "%02d/%02d/%04d".format(
                                        day,
                                        month + 1,
                                        year
                                    )
                            },
                            calendar.get(
                                Calendar.YEAR
                            ),
                            calendar.get(
                                Calendar.MONTH
                            ),
                            calendar.get(
                                Calendar.DAY_OF_MONTH
                            )
                        ).show()
                    }
                )

                if (
                    selectedRepeatUntilDate
                        .isNotBlank()
                ) {

                    TextButton(
                        onClick = {

                            selectedRepeatUntilDate =
                                ""
                        }
                    ) {

                        Text(
                            text =
                                "Clear repeat end date"
                        )
                    }
                }

                Text(
                    text =
                        if (
                            selectedType ==
                            "Deadline"
                        ) {
                            "For deadlines, repeating reminders will normally stop on the final deadline."
                        } else {
                            "Leave this empty if the reminder should continue indefinitely."
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

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        ReminderFormSection(
            icon =
                Icons.Outlined.NotificationsActive,
            title =
                "Notifications",
            subtitle =
                "You can receive more than one alert."
        ) {

            ReminderTimingSelector(
                selectedTimings =
                    selectedReminderTimings,
                onSelectionChanged = {
                    selectedReminderTimings =
                        it
                }
            )

            if (
                selectedType ==
                "Deadline"
            ) {

                Spacer(
                    modifier =
                        Modifier.height(22.dp)
                )

                AutomaticEscalationToggle(
                    enabled =
                        automaticEscalationEnabled,
                    priority =
                        selectedPriority,
                    onEnabledChange = {
                        automaticEscalationEnabled =
                            it
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        ReminderFormSection(
            icon =
                Icons.Outlined.Description,
            title =
                "Notes",
            subtitle =
                "Add any information you may need later."
        ) {

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                placeholder = {
                    Text(
                        text =
                            "Optional notes"
                    )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                shape =
                    RoundedCornerShape(14.dp)
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(
            onClick = {

                when {

                    title.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Please enter a title",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    selectedDate.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Please choose a date",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    selectedTime.isBlank() -> {

                        Toast.makeText(
                            context,
                            "Please choose a time",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    selectedReminderTimings
                        .isEmpty() -> {

                        Toast.makeText(
                            context,
                            "Please select at least one reminder time",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    selectedType ==
                            "Deadline" &&
                            selectedDeadlineDate
                                .isBlank() -> {

                        Toast.makeText(
                            context,
                            "Please choose the final deadline date",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    selectedType ==
                            "Deadline" &&
                            selectedDeadlineTime
                                .isBlank() -> {

                        Toast.makeText(
                            context,
                            "Please choose the final deadline time",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    selectedType ==
                            "Deadline" &&
                            !isReminderDateTimeOnOrAfter(
                                selectedDeadlineDate,
                                selectedDeadlineTime,
                                selectedDate,
                                selectedTime
                            ) -> {

                        Toast.makeText(
                            context,
                            "The final deadline cannot be before the first reminder.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    ReminderScheduler
                        .isRecurring(
                            selectedRepeatRule
                        ) &&
                            selectedRepeatUntilDate
                                .isNotBlank() &&
                            !isReminderDateOnOrAfter(
                                selectedRepeatUntilDate,
                                selectedDate
                            ) -> {

                        Toast.makeText(
                            context,
                            "Repeat Until cannot be before the first reminder date.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    selectedType ==
                            "Deadline" &&
                            selectedRepeatUntilDate
                                .isNotBlank() &&
                            !isReminderDateOnOrAfter(
                                selectedDeadlineDate,
                                selectedRepeatUntilDate
                            ) -> {

                        Toast.makeText(
                            context,
                            "Repeat Until cannot be after the final deadline.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES
                                .TIRAMISU &&
                            ContextCompat
                                .checkSelfPermission(
                                    context,
                                    Manifest.permission
                                        .POST_NOTIFICATIONS
                                ) !=
                            PackageManager
                                .PERMISSION_GRANTED -> {

                        notificationPermissionLauncher
                            .launch(
                                Manifest.permission
                                    .POST_NOTIFICATIONS
                            )
                    }

                    Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.S &&
                            !(
                                    context
                                        .getSystemService(
                                            Context.ALARM_SERVICE
                                        ) as AlarmManager
                                    )
                                .canScheduleExactAlarms() -> {

                        Toast.makeText(
                            context,
                            "Please allow this app to set alarms and reminders, then return and tap Save again.",
                            Toast.LENGTH_LONG
                        ).show()

                        val intent =
                            Intent(
                                Settings
                                    .ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                            ).apply {

                                data =
                                    Uri.parse(
                                        "package:${context.packageName}"
                                    )
                            }

                        context.startActivity(
                            intent
                        )
                    }

                    else -> {

                        val recurring =
                            ReminderScheduler
                                .isRecurring(
                                    selectedRepeatRule
                                )

                        val finalDeadlineDate =
                            if (
                                selectedType ==
                                "Deadline"
                            ) {
                                selectedDeadlineDate
                            } else {
                                "None"
                            }

                        val finalDeadlineTime =
                            if (
                                selectedType ==
                                "Deadline"
                            ) {
                                selectedDeadlineTime
                            } else {
                                "None"
                            }

                        val repeatUntil =
                            if (recurring) {

                                when {

                                    selectedRepeatUntilDate
                                        .isNotBlank() -> {

                                        selectedRepeatUntilDate
                                    }

                                    selectedType ==
                                            "Deadline" -> {

                                        selectedDeadlineDate
                                    }

                                    else -> {

                                        "None"
                                    }
                                }

                            } else {

                                "None"
                            }

                        val anchorDate =
                            if (recurring) {
                                selectedDate
                            } else {
                                "None"
                            }

                        val reminder =
                            Reminder(
                                title =
                                    title.trim(),
                                type =
                                    selectedType,
                                date =
                                    selectedDate,
                                time =
                                    selectedTime,
                                priority =
                                    selectedPriority,
                                category =
                                    selectedCategory,
                                reminderTiming =
                                    serializeReminderTimings(
                                        selectedReminderTimings
                                    ),
                                notes =
                                    notes.trim(),
                                repeatRule =
                                    selectedRepeatRule,
                                repeatAnchorDate =
                                    anchorDate,
                                deadlineDate =
                                    finalDeadlineDate,
                                deadlineTime =
                                    finalDeadlineTime,
                                repeatUntilDate =
                                    repeatUntil,
                                escalationEnabled =
                                    selectedType ==
                                        "Deadline" &&
                                        automaticEscalationEnabled
                            )

                        coroutineScope.launch {

                            val newId =
                                database
                                    .reminderDao()
                                    .insertReminder(
                                        reminder
                                    )

                            val savedReminder =
                                reminder.copy(
                                    id =
                                        newId.toInt()
                                )

                            val scheduled =
                                ReminderScheduler
                                    .scheduleReminder(
                                        context,
                                        savedReminder
                                    )

                            if (scheduled) {

                                Toast.makeText(
                                    context,
                                    "Reminder saved and scheduled",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    context,
                                    "Reminder saved, but no future notification could be scheduled.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            onBack()
                        }
                    }
                }
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            shape =
                RoundedCornerShape(16.dp)
        ) {

            Text(
                text = "Save reminder"
            )
        }
    }
}

@Composable
fun ReminderFormHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        IconButton(
            onClick = onBack
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.ArrowBack,
                contentDescription =
                    "Back"
            )
        }

        Spacer(
            modifier =
                Modifier.size(6.dp)
        )

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text = subtitle,
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

@Composable
fun ReminderFormSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(22.dp),
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
                Modifier.padding(18.dp)
        ) {

            Row {

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    modifier =
                        Modifier.size(10.dp)
                )

                Column {

                    Text(
                        text = title,
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Text(
                        text = subtitle,
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

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            content()
        }
    }
}

@Composable
fun FormLabel(
    text: String
) {

    Text(
        text = text,
        style =
            MaterialTheme
                .typography
                .labelLarge,
        modifier =
            Modifier.padding(
                bottom = 7.dp
            )
    )
}

@Composable
fun FormPickerButton(
    icon: ImageVector,
    text: String,
    emptyText: String,
    onClick: () -> Unit
) {

    OutlinedButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp),
        shape =
            RoundedCornerShape(14.dp)
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier =
                Modifier.size(19.dp)
        )

        Spacer(
            modifier =
                Modifier.size(8.dp)
        )

        Text(
            text =
                if (
                    text.isBlank()
                ) {
                    emptyText
                } else {
                    text
                }
        )
    }
}

@Composable
fun AutomaticEscalationToggle(
    enabled: Boolean,
    priority: String,
    onEnabledChange: (Boolean) -> Unit
) {

    val description =
        when (priority) {

            "Critical" ->
                "Adds intensive deadline alerts: 1 week, 3 days, 1 day, 2 hours, 1 hour, 30 minutes, 10 minutes and at the deadline."

            "Important" ->
                "Adds balanced deadline alerts: 3 days, 1 day, 2 hours, 30 minutes and at the deadline."

            else ->
                "Adds gentle deadline alerts: 1 day, 1 hour and at the deadline."
        }

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    "Automatic escalation",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
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

        Spacer(
            modifier =
                Modifier.size(14.dp)
        )

        Switch(
            checked =
                enabled,
            onCheckedChange =
                onEnabledChange
        )
    }
}

@Composable
fun SimpleDropdown(
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            shape =
                RoundedCornerShape(14.dp)
        ) {

            Text(
                text = selectedValue
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {
                        Text(
                            text = option
                        )
                    },
                    onClick = {

                        onValueSelected(
                            option
                        )

                        expanded = false
                    }
                )
            }
        }
    }
}
