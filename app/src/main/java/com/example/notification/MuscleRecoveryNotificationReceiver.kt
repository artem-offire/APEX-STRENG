package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MuscleRecoveryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("MuscleRecoveryReceiver", "Received broadcast action: $action")

        if (action == MuscleRecoveryNotificationHelper.ACTION_MUSCLE_RECOVERED) {
            val muscleName = intent.getStringExtra(MuscleRecoveryNotificationHelper.EXTRA_MUSCLE_NAME) ?: "Мʼязи"
            val recoveryHours = intent.getIntExtra(MuscleRecoveryNotificationHelper.EXTRA_RECOVERY_HOURS, 48)
            MuscleRecoveryNotificationHelper.showRecoveryNotification(
                context = context,
                muscleName = muscleName,
                recoveryHours = recoveryHours
            )
        }
    }
}
