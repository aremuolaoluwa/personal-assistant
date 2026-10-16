package com.josepharemu.reminderassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.josepharemu.reminderassistant.ui.theme.PersonalReminderAssistantTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PersonalReminderAssistantTheme {

                var currentScreen by remember {
                    mutableStateOf("home")
                }

                var selectedReminder by remember {
                    mutableStateOf<Reminder?>(null)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    when (currentScreen) {

                        "home" -> {

                            HomeScreen(
                                onAddReminder = {
                                    currentScreen = "addReminder"
                                },

                                onOpenCompleted = {
                                    currentScreen = "completed"
                                },

                                onEditReminder = { reminder ->

                                    selectedReminder = reminder

                                    currentScreen = "editReminder"
                                },

                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        "addReminder" -> {

                            AddReminderScreen(
                                onBack = {
                                    currentScreen = "home"
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        "editReminder" -> {

                            selectedReminder?.let { reminder ->

                                EditReminderScreen(
                                    reminder = reminder,

                                    onBack = {

                                        selectedReminder = null

                                        currentScreen = "home"
                                    },

                                    modifier =
                                        Modifier.padding(innerPadding)
                                )
                            }
                        }

                        "completed" -> {

                            CompletedScreen(
                                onBack = {
                                    currentScreen = "home"
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onAddReminder: () -> Unit,
    onOpenCompleted: () -> Unit,
    onEditReminder: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val database = remember {
        ReminderDatabase.getDatabase(context)
    }

    val coroutineScope = rememberCoroutineScope()

    val reminders by database
        .reminderDao()
        .getActiveReminders()
        .collectAsState(initial = emptyList())

    val today =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        ).format(Date())

    val todayReminders = reminders.filter {
        it.date == today
    }

    val importantReminders = reminders.filter {
        it.date != today &&
                (
                        it.priority == "Important" ||
                                it.priority == "Critical"
                        )
    }

    val upcomingReminders = reminders.filter {
        it.date != today &&
                it.priority == "Normal"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            text = "Personal Reminder Assistant",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Today",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (todayReminders.isEmpty()) {

            Text(
                text = "No reminders for today"
            )

        } else {

            todayReminders.forEach { reminder ->

                ReminderCard(
                    reminder = reminder,

                    onEdit = {
                        onEditReminder(reminder)
                    },

                    onDone = {

                        ReminderScheduler.cancelReminder(
                            context,
                            reminder.id
                        )

                        coroutineScope.launch {

                            database
                                .reminderDao()
                                .updateReminder(
                                    reminder.copy(
                                        isCompleted = true
                                    )
                                )
                        }
                    },

                    onDelete = {

                        ReminderScheduler.cancelReminder(
                            context,
                            reminder.id
                        )

                        coroutineScope.launch {

                            database
                                .reminderDao()
                                .deleteReminder(reminder)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Important",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (importantReminders.isEmpty()) {

            Text(
                text = "Nothing important yet"
            )

        } else {

            importantReminders.forEach { reminder ->

                ReminderCard(
                    reminder = reminder,

                    onEdit = {
                        onEditReminder(reminder)
                    },

                    onDone = {

                        ReminderScheduler.cancelReminder(
                            context,
                            reminder.id
                        )

                        coroutineScope.launch {

                            database
                                .reminderDao()
                                .updateReminder(
                                    reminder.copy(
                                        isCompleted = true
                                    )
                                )
                        }
                    },

                    onDelete = {

                        ReminderScheduler.cancelReminder(
                            context,
                            reminder.id
                        )

                        coroutineScope.launch {

                            database
                                .reminderDao()
                                .deleteReminder(reminder)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Coming Up",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (upcomingReminders.isEmpty()) {

            Text(
                text = "No upcoming items"
            )

        } else {

            upcomingReminders.forEach { reminder ->

                ReminderCard(
                    reminder = reminder,

                    onEdit = {
                        onEditReminder(reminder)
                    },

                    onDone = {

                        ReminderScheduler.cancelReminder(
                            context,
                            reminder.id
                        )

                        coroutineScope.launch {

                            database
                                .reminderDao()
                                .updateReminder(
                                    reminder.copy(
                                        isCompleted = true
                                    )
                                )
                        }
                    },

                    onDelete = {

                        ReminderScheduler.cancelReminder(
                            context,
                            reminder.id
                        )

                        coroutineScope.launch {

                            database
                                .reminderDao()
                                .deleteReminder(reminder)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddReminder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "+ Add Reminder")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenCompleted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Completed")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${reminder.type} • ${reminder.priority}"
            )

            Text(
                text = if (reminder.time.isBlank()) {
                    reminder.date
                } else {
                    "${reminder.date} at ${reminder.time}"
                }
            )

            if (reminder.category.isNotBlank()) {

                Text(
                    text = "Category: ${reminder.category}"
                )
            }

            if (reminder.notes.isNotBlank()) {

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = reminder.notes
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Edit")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Done")
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Delete")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {

    PersonalReminderAssistantTheme {

        HomeScreen(
            onAddReminder = {},
            onOpenCompleted = {},
            onEditReminder = {}
        )
    }
}