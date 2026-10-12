package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.AudioHapticFeedback
import kotlinx.coroutines.*

class RestTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private lateinit var audioHaptic: AudioHapticFeedback
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_TIMER_ID = "apex_rest_timer_channel"
        const val CHANNEL_COMPLETE_ID = "apex_rest_complete_channel"
        const val NOTIFICATION_ID_TIMER = 1001
        const val NOTIFICATION_ID_COMPLETE = 1002

        const val ACTION_START = "com.example.action.START_TIMER"
        const val ACTION_ADJUST = "com.example.action.ADJUST_TIMER"
        const val ACTION_CANCEL = "com.example.action.CANCEL_TIMER"

        const val EXTRA_SECONDS = "extra_seconds"
        const val EXTRA_DELTA = "extra_delta"
    }

    override fun onCreate() {
        super.onCreate()
        audioHaptic = AudioHapticFeedback(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()

        // CRITICAL: Call startForeground immediately in onCreate to satisfy Android OS requirements
        val initialSeconds = RestTimerManager.restSecondsLeft.value.let { if (it > 0) it else 60 }
        val initialTotal = RestTimerManager.restTotalSeconds.value.let { if (it > 0) it else 60 }
        val notification = buildTimerNotification(initialSeconds, initialTotal)
        runStartForegroundSafe(notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_START -> {
                val seconds = intent.getIntExtra(EXTRA_SECONDS, 60)
                startRestCountdown(seconds)
            }
            ACTION_ADJUST -> {
                val delta = intent.getIntExtra(EXTRA_DELTA, 0)
                if (delta != 0) {
                    RestTimerManager.adjust(this, delta)
                } else {
                    adjustRestCountdown()
                }
            }
            ACTION_CANCEL -> {
                cancelRestCountdown()
            }
            else -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun runStartForegroundSafe(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID_TIMER,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID_TIMER, notification)
            }
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID_TIMER, notification)
            } catch (_: Exception) {}
        }
    }

    private fun startRestCountdown(seconds: Int) {
        timerJob?.cancel()

        val notification = buildTimerNotification(seconds, seconds)
        runStartForegroundSafe(notification)

        timerJob = serviceScope.launch {
            var lastBeepSecond = -1
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val targetTime = RestTimerManager.targetEndRealtimeMillis
                val remainingMillis = targetTime - now
                val remainingSeconds = (remainingMillis / 1000L).toInt().coerceAtLeast(0)

                RestTimerManager.updateState(
                    isActive = remainingSeconds > 0,
                    secondsLeft = remainingSeconds,
                    totalSeconds = RestTimerManager.restTotalSeconds.value
                )

                if (remainingSeconds <= 0) {
                    onTimerFinished()
                    break
                }

                // Audio tick on 3, 2, 1
                if (remainingSeconds in 1..3 && remainingSeconds != lastBeepSecond) {
                    lastBeepSecond = remainingSeconds
                    RestTimerAudioHelper.playTick(this@RestTimerService)
                }

                // Update notification text every second
                notificationManager.notify(
                    NOTIFICATION_ID_TIMER,
                    buildTimerNotification(remainingSeconds, RestTimerManager.restTotalSeconds.value)
                )

                delay(1000L)
            }
        }
    }

    private fun adjustRestCountdown() {
        val now = SystemClock.elapsedRealtime()
        val target = RestTimerManager.targetEndRealtimeMillis
        val remainingSeconds = ((target - now) / 1000L).toInt().coerceAtLeast(0)
        val total = RestTimerManager.restTotalSeconds.value

        notificationManager.notify(
            NOTIFICATION_ID_TIMER,
            buildTimerNotification(remainingSeconds, total)
        )
    }

    private fun cancelRestCountdown() {
        timerJob?.cancel()
        RestTimerManager.updateState(isActive = false, secondsLeft = 0, totalSeconds = 0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerFinished() {
        RestTimerManager.updateState(isActive = false, secondsLeft = 0, totalSeconds = 0)
        RestTimerAudioHelper.playFinishSound(this)

        // Show High Priority Alert Notification
        showCompletionNotification()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildTimerNotification(
        secondsLeft: Int,
        totalSeconds: Int
    ): Notification {
        val minutes = secondsLeft / 60
        val secs = secondsLeft % 60
        val timeString = String.format("%02d:%02d", minutes, secs)

        // PendingIntent to return to app
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: +30 sec
        val add30Intent = Intent(this, RestTimerService::class.java).apply {
            action = ACTION_ADJUST
            putExtra(EXTRA_DELTA, 30)
        }
        val add30Pending = PendingIntent.getService(
            this,
            1,
            add30Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Skip / Finish rest
        val skipIntent = Intent(this, RestTimerService::class.java).apply {
            action = ACTION_CANCEL
        }
        val skipPending = PendingIntent.getService(
            this,
            2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_TIMER_ID)
            .setContentTitle("⏱ Відпочинок: $timeString")
            .setContentText("Залишилося $timeString • Натисніть щоб повернутися")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(totalSeconds, totalSeconds - secondsLeft, false)
            .setContentIntent(contentIntent)
            .addAction(0, "+30 сек", add30Pending)
            .addAction(0, "Пропустити", skipPending)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun showCompletionNotification() {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_COMPLETE_ID)
            .setContentTitle("🔔 Відпочинок завершено!")
            .setContentText("Час для наступного підходу. До нових рекордів!")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Таймер відпочинку",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Показує зворотний відлік відпочинку між підходами"
                setShowBadge(false)
            }

            val completeChannel = NotificationChannel(
                CHANNEL_COMPLETE_ID,
                "Завершення відпочинку",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Сповіщає звуком та вібрацією про завершення відпочинку"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(completeChannel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
