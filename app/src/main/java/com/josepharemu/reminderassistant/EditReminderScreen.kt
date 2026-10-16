package com.josepharemu.reminderassistant

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun EditReminderScreen(
    reminder: Reminder,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context =
        LocalContext.current

    val database =
        remember {
            ReminderDatabase
                .getDatabase(context)
        }

    val coroutineScope =
        rememberCoroutineScope()

    var title
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.title
                )
            }

    var notes
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.notes
                )
            }

    var selectedType
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.type
                )
            }

    var selectedDate
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.date
                )
            }

    var selectedTime
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.time
                )
            }

    var selectedPriority
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.priority
                )
            }

    var selectedCategory
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.category
                )
            }

    var selectedRepeatRule
            by remember(reminder.id) {
                mutableStateOf(
                    reminder.repeatRule
                )
            }

    var selectedRepeatUntilDate
            by remember(reminder.id) {

                mutableStateOf(
                    if (
                        reminder.repeatUntilDate ==
                        "None"
                    ) {
                        ""
                    } else {
                        reminder.repeatUntilDate
                    }
                )
            }

    var selectedDeadlineDate
            by remember(reminder.id) {

                mutableStateOf(
                    when {

                        reminder.deadlineDate !=
                                "None" -> {

                            reminder.deadlineDate
                        }

                        reminder.type ==
                                "Deadline" -> {

                            reminder.date
                        }

                        else -> {

                            ""
                        }
                    }
                )
            }

    var selectedDeadlineTime
            by remember(reminder.id) {

                mutableStateOf(
                    when {

                        reminder.deadlineTime !=
                                "None" -> {

                            reminder.deadlineTime
                        }

                        reminder.type ==
                                "Deadline" -> {

                            reminder.time
                        }

                        else -> {

                            ""
                        }
                    }
                )
            }

    var selectedReminderTimings
            by remember(reminder.id) {

                mutableStateOf(
                    parseReminderTimings(
                        reminder.reminderTiming
                    )
                )
            }

    val calendar =
        Calendar.getInstance()

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
            title =
                "Edit reminder",
            subtitle =
                "Update the reminder without creating a new one.",
            onBack =
                onBack
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
                "Update the reminder information."
        ) {

            FormLabel(
                text = "Title"
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
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
                if (
                    selectedType ==
                    "Deadline"
                ) {
                    "Manage the next reminder and final deadline."
                } else {
                    "Update when this reminder should happen."
                }
        ) {

            FormLabel(
                text =
                    if (
                        selectedType ==
                        "Deadline"
                    ) {
                        "Next reminder date"
                    } else {
                        "Date"
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
                    if (
                        selectedType ==
                        "Deadline"
                    ) {
                        "Next reminder time"
                    } else {
                        "Time"
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
                        "The dashboard will eventually use this date to show how much time remains.",
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
                "Control how long this reminder continues."
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
                "Choose one or more alert times."
        ) {

            ReminderTimingSelector(
                selectedTimings =
                    selectedReminderTimings,
                onSelectionChanged = {
                    selectedReminderTimings =
                        it
                }
            )
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
                "Keep useful information with this reminder."
        ) {

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
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
                            "The final deadline cannot be before the next reminder.",
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
                            "Repeat Until cannot be before the next reminder date.",
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

                    else -> {

                        val recurring =
                            ReminderScheduler
                                .isRecurring(
                                    selectedRepeatRule
                                )

                        val updatedDeadlineDate =
                            if (
                                selectedType ==
                                "Deadline"
                            ) {
                                selectedDeadlineDate
                            } else {
                                "None"
                            }

                        val updatedDeadlineTime =
                            if (
                                selectedType ==
                                "Deadline"
                            ) {
                                selectedDeadlineTime
                            } else {
                                "None"
                            }

                        val updatedRepeatUntil =
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

                        val updatedAnchorDate =
                            when {

                                !recurring -> {

                                    "None"
                                }

                                !ReminderScheduler
                                    .isRecurring(
                                        reminder.repeatRule
                                    ) -> {

                                    selectedDate
                                }

                                selectedDate !=
                                        reminder.date -> {

                                    selectedDate
                                }

                                selectedRepeatRule !=
                                        reminder.repeatRule -> {

                                    selectedDate
                                }

                                reminder.repeatAnchorDate !=
                                        "None" -> {

                                    reminder.repeatAnchorDate
                                }

                                else -> {

                                    selectedDate
                                }
                            }

                        val updatedReminder =
                            reminder.copy(
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
                                    updatedAnchorDate,
                                deadlineDate =
                                    updatedDeadlineDate,
                                deadlineTime =
                                    updatedDeadlineTime,
                                repeatUntilDate =
                                    updatedRepeatUntil
                            )

                        coroutineScope.launch {

                            ReminderScheduler
                                .cancelReminder(
                                    context,
                                    reminder.id
                                )

                            database
                                .reminderDao()
                                .updateReminder(
                                    updatedReminder
                                )

                            val scheduled =
                                ReminderScheduler
                                    .scheduleReminder(
                                        context,
                                        updatedReminder
                                    )

                            if (scheduled) {

                                Toast.makeText(
                                    context,
                                    "Reminder updated and scheduled",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    context,
                                    "Reminder updated, but no future notification could be scheduled.",
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
                text =
                    "Save changes"
            )
        }
    }
}