package com.josepharemu.reminderassistant

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UpcomingReminderWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

        val pendingResult =
            goAsync()

        val scope =
            CoroutineScope(
                SupervisorJob() +
                    Dispatchers.IO
            )

        scope.launch {

            try {

                UpcomingReminderWidgetUpdater
                    .update(
                        context = context,
                        appWidgetManager =
                            appWidgetManager,
                        appWidgetIds =
                            appWidgetIds
                    )

            } finally {

                pendingResult.finish()
            }
        }
    }
}
