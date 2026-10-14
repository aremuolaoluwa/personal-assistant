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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.josepharemu.reminderassistant.ui.theme.PersonalReminderAssistantTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            PersonalReminderAssistantTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    HomeScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {

    Column(
        modifier = modifier
            .fillMaxSize()
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

        Text(
            text = "No reminders for today"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Important",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Nothing important yet"
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Coming Up",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "No upcoming items"
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                // We will make this button work shortly.
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "+ Add Reminder")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {

    PersonalReminderAssistantTheme {
        HomeScreen()
    }
}