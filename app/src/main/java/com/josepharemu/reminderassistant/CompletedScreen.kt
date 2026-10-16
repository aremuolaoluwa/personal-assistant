package com.josepharemu.reminderassistant

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CompletedScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    val database = remember {
        ReminderDatabase.getDatabase(context)
    }

    val coroutineScope = rememberCoroutineScope()

    val completedReminders by database
        .reminderDao()
        .getCompletedReminders()
        .collectAsState(initial = emptyList())

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

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Completed",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (completedReminders.isEmpty()) {

            Text(
                text = "No completed reminders yet."
            )

        } else {

            completedReminders.forEach { reminder ->

                CompletedReminderCard(
                    reminder = reminder,

                    onRestore = {

                        val restoredReminder =
                            reminder.copy(
                                isCompleted = false
                            )

                        coroutineScope.launch {

                            database
                                .reminderDao()
                                .updateReminder(restoredReminder)

                            ReminderScheduler
                                .scheduleReminder(
                                    context,
                                    restoredReminder
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

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CompletedReminderCard(
    reminder: Reminder,
    onRestore: () -> Unit,
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

                Text(text = reminder.notes)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Restore")
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