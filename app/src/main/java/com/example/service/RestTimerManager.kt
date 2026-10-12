package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RestTimerManager {
    private val _isRestActive = MutableStateFlow(false)
    val isRestActive: StateFlow<Boolean> = _isRestActive.asStateFlow()

    private val _restSecondsLeft = MutableStateFlow(0)
    val restSecondsLeft: StateFlow<Int> = _restSecondsLeft.asStateFlow()

    private val _restTotalSeconds = MutableStateFlow(0)
    val restTotalSeconds: StateFlow<Int> = _restTotalSeconds.asStateFlow()

    var targetEndRealtimeMillis: Long = 0L
        private set

    fun updateState(isActive: Boolean, secondsLeft: Int, totalSeconds: Int) {
        _isRestActive.value = isActive
        _restSecondsLeft.value = secondsLeft
        _restTotalSeconds.value = totalSeconds
    }

    fun start(context: Context, totalSeconds: Int) {
        val safeSeconds = totalSeconds.coerceAtLeast(5)
        _restTotalSeconds.value = safeSeconds
        _restSecondsLeft.value = safeSeconds
        _isRestActive.value = true
        targetEndRealtimeMillis = SystemClock.elapsedRealtime() + safeSeconds * 1000L

        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_START
            putExtra(RestTimerService.EXTRA_SECONDS, safeSeconds)
        }
        startServiceSafe(context, intent)
    }

    fun adjust(context: Context, deltaSeconds: Int) {
        if (!_isRestActive.value) return
        val now = SystemClock.elapsedRealtime()
        val currentRemaining = ((targetEndRealtimeMillis - now) / 1000L).toInt()
        val newRemaining = (currentRemaining + deltaSeconds).coerceIn(1, 600)

        targetEndRealtimeMillis = now + newRemaining * 1000L
        _restSecondsLeft.value = newRemaining
        _restTotalSeconds.value = maxOf(_restTotalSeconds.value, newRemaining)

        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_ADJUST
            putExtra(RestTimerService.EXTRA_DELTA, 0)
        }
        startServiceSafe(context, intent)
    }

    fun cancel(context: Context) {
        _isRestActive.value = false
        _restSecondsLeft.value = 0
        targetEndRealtimeMillis = 0L

        try {
            context.stopService(Intent(context, RestTimerService::class.java))
        } catch (_: Exception) {}
    }

    private fun startServiceSafe(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }
}
