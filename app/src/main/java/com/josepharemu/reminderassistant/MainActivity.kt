package com.josepharemu.reminderassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.josepharemu.reminderassistant.ui.theme.PersonalReminderAssistantTheme
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

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    when (currentScreen) {

                        "home" -> {
                            HomeScreen(
                                onAddReminder = {
                                    currentScreen = "addReminder"
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
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    onAddReminder: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val database = remember {
        ReminderDatabase.getDatabase(context)
    }

    val reminders by database
        .reminderDao()
        .getActiveReminders()
        .collectAsState(initial = emptyList())

    val today = SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.getDefault()
    ).format(Date())

    val todayReminders = reminders.filter {
        it.date == today
    }

    val importantReminders = reminders.filter {
        it.date != today &&
                (it.priority == "Important" || it.priority == "Critical")
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

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        // TODAY
        Text(
            text = "Today",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (todayReminders.isEmpty()) {

            Text(
                text = "No reminders for today"
            )

        } else {

            todayReminders.forEach { reminder ->

                ReminderCard(reminder)

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // IMPORTANT
        Text(
            text = "Important",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (importantReminders.isEmpty()) {

            Text(
                text = "Nothing important yet"
            )

        } else {

            importantReminders.forEach { reminder ->

                ReminderCard(reminder)

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // COMING UP
        Text(
            text = "Coming Up",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (upcomingReminders.isEmpty()) {

            Text(
                text = "No upcoming items"
            )

        } else {

            upcomingReminders.forEach { reminder ->

                ReminderCard(reminder)

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Button(
            onClick = onAddReminder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "+ Add Reminder"
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder
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

            Spacer(
                modifier = Modifier.height(4.dp)
            )

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

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = reminder.notes
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {

    PersonalReminderAssistantTheme {
        HomeScreen(
            onAddReminder = {}
        )
    }
}