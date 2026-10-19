package com.josepharemu.reminderassistant

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StillWorkingReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val notificationId =
            intent.getIntExtra(
                "NOTIFICATION_ID",
                0
            )

        if (notificationId == 0) {
            return
        }

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        notificationManager.cancel(
            notificationId
        )
    }
}
