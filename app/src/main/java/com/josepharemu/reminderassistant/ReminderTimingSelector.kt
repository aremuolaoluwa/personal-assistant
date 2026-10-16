package com.josepharemu.reminderassistant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

val reminderTimingOptions = listOf(
    "1 week before",
    "3 days before",
    "1 day before",
    "2 hours before",
    "1 hour before",
    "30 minutes before",
    "10 minutes before",
    "At time of event"
)

fun parseReminderTimings(
    value: String
): Set<String> {

    val timings = value
        .split("|")
        .map {
            it.trim()
        }
        .filter {
            it.isNotBlank()
        }
        .toSet()

    return if (timings.isEmpty()) {
        setOf("At time of event")
    } else {
        timings
    }
}

fun serializeReminderTimings(
    selectedTimings: Set<String>
): String {

    return reminderTimingOptions
        .filter {
            it in selectedTimings
        }
        .joinToString("|")
}

@Composable
fun ReminderTimingSelector(
    selectedTimings: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        reminderTimingOptions.forEach { timing ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = timing in selectedTimings,

                    onCheckedChange = { checked ->

                        val updatedTimings =
                            selectedTimings.toMutableSet()

                        if (checked) {
                            updatedTimings.add(timing)
                        } else {
                            updatedTimings.remove(timing)
                        }

                        onSelectionChanged(
                            updatedTimings.toSet()
                        )
                    }
                )

                Text(
                    text = timing
                )
            }
        }
    }
}