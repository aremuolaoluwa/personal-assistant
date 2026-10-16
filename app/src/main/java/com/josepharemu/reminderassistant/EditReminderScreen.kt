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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

    val context = LocalContext.current

    val database = remember {
        ReminderDatabase.getDatabase(context)
    }

    val coroutineScope =
        rememberCoroutineScope()

    var title by remember(reminder.id) {
        mutableStateOf(
            reminder.title
        )
    }

    var notes by remember(reminder.id) {
        mutableStateOf(
            reminder.notes
        )
    }

    var selectedType by remember(reminder.id) {
        mutableStateOf(
            reminder.type
        )
    }

    var selectedDate by remember(reminder.id) {
        mutableStateOf(
            reminder.date
        )
    }

    var selectedTime by remember(reminder.id) {
        mutableStateOf(
            reminder.time
        )
    }

    var selectedPriority by remember(reminder.id) {
        mutableStateOf(
            reminder.priority
        )
    }

    var selectedCategory by remember(reminder.id) {
        mutableStateOf(
            reminder.category
        )
    }

    var selectedRepeatRule by remember(reminder.id) {
        mutableStateOf(
            reminder.repeatRule
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

    val calendar = Calendar.getInstance()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp)
    ) {

        TextButton(
            onClick = onBack
        ) {
            Text(text = "← Back")
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Edit Reminder",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Title",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Type",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        SimpleDropdown(
            selectedValue = selectedType,
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
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Date",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
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
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = selectedDate
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Time",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
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
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    if (selectedTime.isBlank()) {
                        "Choose Time"
                    } else {
                        selectedTime
                    }
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Priority",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        SimpleDropdown(
            selectedValue = selectedPriority,
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
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Category",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        SimpleDropdown(
            selectedValue = selectedCategory,
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

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Repeat",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        SimpleDropdown(
            selectedValue = selectedRepeatRule,
            options = listOf(
                "None",
                "Daily",
                "Weekly",
                "Monthly",
                "Yearly"
            ),
            onValueSelected = {
                selectedRepeatRule = it
            }
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Remind Me",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "You can select more than one."
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        ReminderTimingSelector(
            selectedTimings =
                selectedReminderTimings,
            onSelectionChanged = {
                selectedReminderTimings = it
            }
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Notes",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = notes,
            onValueChange = {
                notes = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(
            modifier = Modifier.height(32.dp)
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

                    selectedReminderTimings.isEmpty() -> {

                        Toast.makeText(
                            context,
                            "Please select at least one reminder time",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {

                        val updatedReminder =
                            reminder.copy(
                                title = title.trim(),
                                type = selectedType,
                                date = selectedDate,
                                time = selectedTime,
                                priority = selectedPriority,
                                category = selectedCategory,
                                reminderTiming =
                                    serializeReminderTimings(
                                        selectedReminderTimings
                                    ),
                                notes = notes.trim(),
                                repeatRule = selectedRepeatRule
                            )

                        coroutineScope.launch {

                            /*
                             * Cancel every alarm belonging
                             * to the old version first.
                             */
                            ReminderScheduler
                                .cancelReminder(
                                    context,
                                    reminder.id
                                )

                            /*
                             * Save the edited reminder.
                             */
                            database
                                .reminderDao()
                                .updateReminder(
                                    updatedReminder
                                )

                            /*
                             * Schedule all selected
                             * reminder times again.
                             */
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
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Save Changes"
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}