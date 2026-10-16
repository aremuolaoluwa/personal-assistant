package com.josepharemu.reminderassistant

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
fun AddReminderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val database = remember {
        ReminderDatabase.getDatabase(context)
    }

    val coroutineScope = rememberCoroutineScope()

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

    var selectedReminderTiming by remember {
        mutableStateOf("At time of event")
    }

    val calendar = Calendar.getInstance()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            text = "Add Reminder",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // TITLE
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
            placeholder = {
                Text(text = "What do you need to remember?")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // TYPE
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

        // DATE
        Text(
            text = "Date",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = {

                val datePicker = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->

                        selectedDate =
                            "%02d/%02d/%04d".format(
                                dayOfMonth,
                                month + 1,
                                year
                            )
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )

                datePicker.show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedDate.isEmpty()) {
                    "Choose Date"
                } else {
                    selectedDate
                }
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // TIME
        Text(
            text = "Time",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = {

                val timePicker = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->

                        selectedTime =
                            "%02d:%02d".format(
                                hourOfDay,
                                minute
                            )
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )

                timePicker.show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (selectedTime.isEmpty()) {
                    "Choose Time"
                } else {
                    selectedTime
                }
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // PRIORITY
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

        // CATEGORY
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

        // REMINDER TIMING
        Text(
            text = "Remind Me",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        SimpleDropdown(
            selectedValue = selectedReminderTiming,
            options = listOf(
                "At time of event",
                "10 minutes before",
                "30 minutes before",
                "1 hour before",
                "2 hours before",
                "1 day before",
                "3 days before",
                "1 week before"
            ),
            onValueSelected = {
                selectedReminderTiming = it
            }
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // NOTES
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
            placeholder = {
                Text(text = "Add any useful information")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // SAVE BUTTON
        Button(
            onClick = {

                if (title.isBlank()) {

                    Toast.makeText(
                        context,
                        "Please enter a title",
                        Toast.LENGTH_SHORT
                    ).show()

                } else if (selectedDate.isBlank()) {

                    Toast.makeText(
                        context,
                        "Please choose a date",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    val reminder = Reminder(
                        title = title.trim(),
                        type = selectedType,
                        date = selectedDate,
                        time = selectedTime,
                        priority = selectedPriority,
                        category = selectedCategory,
                        reminderTiming = selectedReminderTiming,
                        notes = notes.trim()
                    )

                    coroutineScope.launch {

                        database
                            .reminderDao()
                            .insertReminder(reminder)

                        Toast.makeText(
                            context,
                            "Reminder saved",
                            Toast.LENGTH_SHORT
                        ).show()

                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Save Reminder")
        }

        Spacer(
            modifier = Modifier.height(24.dp)
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
        modifier = Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = selectedValue)
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
                        Text(text = option)
                    },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}