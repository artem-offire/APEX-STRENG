package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.engine.MuscleGroupRecoveryStatus
import com.example.engine.RecoveryState
import com.example.model.MuscleGroup

object MuscleRecoveryNotificationHelper {
    const val CHANNEL_ID = "apex_muscle_recovery_channel"
    const val CHANNEL_NAME = "Відновлення м'язів"
    const val ACTION_MUSCLE_RECOVERED = "com.example.apex.ACTION_MUSCLE_RECOVERED"
    const val EXTRA_MUSCLE_NAME = "extra_muscle_name"
    const val EXTRA_RECOVERY_HOURS = "extra_recovery_hours"

    const val ACTION_MARK_READ = "com.example.apex.ACTION_MARK_READ"
    const val ACTION_MARK_ALL_READ = "com.example.apex.ACTION_MARK_ALL_READ"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    private const val PREFS_NAME = "apex_muscle_notifications"
    private const val KEY_NOTIFICATIONS_ENABLED = "recovery_push_enabled"

    fun isRecoveryNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setRecoveryNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Сповіщення про повне відновлення м'язових груп (інтервал 48-72 год)"
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showRecoveryNotification(
        context: Context,
        muscleName: String,
        recoveryHours: Int
    ) {
        if (!isRecoveryNotificationEnabled(context)) return

        // Check POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("MuscleRecoveryHelper", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        createNotificationChannel(context)

        val notificationId = 2000 + (muscleName.hashCode() % 1000).let { if (it < 0) -it else it }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            muscleName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val markReadIntent = Intent(context, MuscleRecoveryNotificationReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notificationTitle = "💪 Мʼязи відновилися: $muscleName!"
        val notificationMessage = "Минуло $recoveryHours год фізіологічного інтервалу. Суперкомпенсація досягнута — час для нового тренування!"

        // Save to in-app history repository
        AppNotificationRepository.addNotification(
            context = context,
            item = AppNotificationItem(
                id = "${muscleName}_${System.currentTimeMillis()}",
                notificationId = notificationId,
                title = notificationTitle,
                message = notificationMessage,
                muscleGroup = muscleName,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Група «$muscleName» повністю відновилася після навантаження (минув науковий інтервал $recoveryHours год). Синтез білка та енергетичні депо відновлені. Оптимальний час для тренування!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "✓ Прочитано",
                markReadPendingIntent
            )
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, notification)
    }

    fun scheduleMuscleRecoveryAlarms(
        context: Context,
        statuses: List<MuscleGroupRecoveryStatus>
    ) {
        if (!isRecoveryNotificationEnabled(context)) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nowMs = System.currentTimeMillis()

        for (status in statuses) {
            if (status.state != RecoveryState.RECOVERED &&
                status.nextOptimalWorkoutTimeMs != null &&
                status.nextOptimalWorkoutTimeMs > nowMs
            ) {
                val intent = Intent(context, MuscleRecoveryNotificationReceiver::class.java).apply {
                    action = ACTION_MUSCLE_RECOVERED
                    putExtra(EXTRA_MUSCLE_NAME, status.muscleGroup.displayNameUk)
                    putExtra(EXTRA_RECOVERY_HOURS, status.recoveryHoursNeeded)
                }

                val requestCode = 5000 + status.muscleGroup.ordinal
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            status.nextOptimalWorkoutTimeMs,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            status.nextOptimalWorkoutTimeMs,
                            pendingIntent
                        )
                    }
                } catch (e: SecurityException) {
                    // In case exact alarm permission isn't granted on newer OS
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        status.nextOptimalWorkoutTimeMs,
                        pendingIntent
                    )
                } catch (e: Exception) {
                    Log.e("MuscleRecoveryHelper", "Failed to schedule alarm for ${status.muscleGroup}", e)
                }
            }
        }
    }

    fun sendTestNotification(context: Context, testMuscle: MuscleGroup = MuscleGroup.CHEST) {
        showRecoveryNotification(
            context = context,
            muscleName = testMuscle.displayNameUk,
            recoveryHours = testMuscle.let {
                when (it) {
                    MuscleGroup.CHEST, MuscleGroup.SHOULDERS -> 60
                    MuscleGroup.BACK, MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES -> 72
                    else -> 48
                }
            }
        )
    }
}
