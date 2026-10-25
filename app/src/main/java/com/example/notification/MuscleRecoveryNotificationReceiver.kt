package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MuscleRecoveryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("MuscleRecoveryReceiver", "Received broadcast action: $action")

        when (action) {
            MuscleRecoveryNotificationHelper.ACTION_MUSCLE_RECOVERED -> {
                val muscleName = intent.getStringExtra(MuscleRecoveryNotificationHelper.EXTRA_MUSCLE_NAME) ?: "Мʼязи"
                val recoveryHours = intent.getIntExtra(MuscleRecoveryNotificationHelper.EXTRA_RECOVERY_HOURS, 48)
                MuscleRecoveryNotificationHelper.showRecoveryNotification(
                    context = context,
                    muscleName = muscleName,
                    recoveryHours = recoveryHours
                )
            }
            MuscleRecoveryNotificationHelper.ACTION_MARK_READ -> {
                val notifId = intent.getIntExtra(MuscleRecoveryNotificationHelper.EXTRA_NOTIFICATION_ID, -1)
                Log.d("MuscleRecoveryReceiver", "Marking notification $notifId as read")
                if (notifId != -1) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.cancel(notifId)
                    AppNotificationRepository.markAsRead(context, notifId)
                }
            }
            MuscleRecoveryNotificationHelper.ACTION_MARK_ALL_READ -> {
                Log.d("MuscleRecoveryReceiver", "Marking all notifications as read")
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancelAll()
                AppNotificationRepository.markAllAsRead(context)
            }
        }
    }
}
