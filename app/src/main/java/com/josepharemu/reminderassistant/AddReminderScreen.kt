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
import androidx.core.content.ContextCompat
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

    var selectedReminderTimings by remember {
        mutableStateOf(
            setOf("At time of event")
        )
    }

    val calendar =
        Calendar.getInstance()

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
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
            .padding(24.dp)
    ) {

        TextButton(
            onClick = onBack
        ) {
            Text(text = "← Back")
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text = "Add Reminder",
            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Text(
            text = "Title",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = title,

            onValueChange = {
                title = it
            },

            placeholder = {
                Text(
                    text =
                        "What do you need to remember?"
                )
            },

            modifier =
                Modifier.fillMaxWidth(),

            singleLine = true
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "Type",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
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
                Modifier.height(20.dp)
        )

        Text(
            text = "Date",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = {

                DatePickerDialog(
                    context,

                    { _, year, month, day ->

                        selectedDate =
                            "%02d/%02d/%04d"
                                .format(
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

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    if (
                        selectedDate.isBlank()
                    ) {
                        "Choose Date"
                    } else {
                        selectedDate
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "Time",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        OutlinedButton(
            onClick = {

                TimePickerDialog(
                    context,

                    { _, hour, minute ->

                        selectedTime =
                            "%02d:%02d"
                                .format(
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

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    if (
                        selectedTime.isBlank()
                    ) {
                        "Choose Time"
                    } else {
                        selectedTime
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "Priority",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
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
                Modifier.height(20.dp)
        )

        Text(
            text = "Category",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
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

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "Remind Me",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Text(
            text =
                "You can select more than one."
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        ReminderTimingSelector(
            selectedTimings =
                selectedReminderTimings,

            onSelectionChanged = {
                selectedReminderTimings = it
            }
        )

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(
            text = "Notes",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = notes,

            onValueChange = {
                notes = it
            },

            placeholder = {
                Text(
                    text =
                        "Add any useful information"
                )
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(
            modifier =
                Modifier.height(32.dp)
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
                                    notes.trim()
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
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Save Reminder"
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
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

    androidx.compose.foundation.layout.Box(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick = {
                expanded = true
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = selectedValue
            )
        }

        androidx.compose.material3.DropdownMenu(
            expanded = expanded,

            onDismissRequest = {
                expanded = false
            }
        ) {

            options.forEach { option ->

                androidx.compose.material3.DropdownMenuItem(
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