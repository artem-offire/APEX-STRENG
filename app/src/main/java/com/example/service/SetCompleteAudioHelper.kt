package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class SetCompletionSound(
    val id: String,
    val titleUk: String,
    val descriptionUk: String,
    val iconEmoji: String
) {
    SUCCESS_CHIME("chime", "Мажорний передзвін", "Приємний дзвінкий акорд успішного підходу", "🔔"),
    SHORT_BEEP("beep", "Швидкий біп", "Енергійний чіткий електронний клік секундоміра", "⚡"),
    VICTORY_FANFARE("victory", "Тріумф / Бонус", "Енергійний мажорний перелив завершення сету", "🌟"),
    BOXING_BELL("bell", "Боксерський гонг", "Дзвінкий металевий сигнал завершення раунду", "🥊"),
    HEAVY_IRON("iron", "Залізний бас", "Глибокий низький тон опускання штанги", "🏋️"),
    SYSTEM_NOTIFICATION("system", "Системний сигнал", "Стандартний звук сповіщення пристрою", "📱"),
    VIBRATE_ONLY("vibrate_only", "Тільки вібрація", "Без звуку, чіткий подвійний тактильний відгук", "📳")
}

object SetCompleteAudioHelper {
    private const val PREFS_NAME = "apex_set_complete_prefs"
    private const val KEY_SOUND = "selected_set_sound"

    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun getSelectedSound(context: Context): SetCompletionSound {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val soundId = prefs.getString(KEY_SOUND, SetCompletionSound.SUCCESS_CHIME.id)
        return SetCompletionSound.values().find { it.id == soundId } ?: SetCompletionSound.SUCCESS_CHIME
    }

    fun setSelectedSound(context: Context, sound: SetCompletionSound) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOUND, sound.id)
            .apply()
    }

    fun playSetCompleteSound(context: Context) {
        val selected = getSelectedSound(context)
        // Satisfying tactile feedback on set completion
        vibrate(context, 75)

        when (selected) {
            SetCompletionSound.SUCCESS_CHIME -> audioScope.launch { playSuccessChime() }
            SetCompletionSound.SHORT_BEEP -> audioScope.launch { playShortBeep() }
            SetCompletionSound.VICTORY_FANFARE -> audioScope.launch { playVictoryFanfare() }
            SetCompletionSound.BOXING_BELL -> audioScope.launch { playBoxingBell() }
            SetCompletionSound.HEAVY_IRON -> audioScope.launch { playHeavyIron() }
            SetCompletionSound.SYSTEM_NOTIFICATION -> playSystemNotification(context)
            SetCompletionSound.VIBRATE_ONLY -> { /* Vibration already handled */ }
        }
    }

    fun playPreviewSound(context: Context, sound: SetCompletionSound) {
        vibrate(context, 75)
        when (sound) {
            SetCompletionSound.SUCCESS_CHIME -> audioScope.launch { playSuccessChime() }
            SetCompletionSound.SHORT_BEEP -> audioScope.launch { playShortBeep() }
            SetCompletionSound.VICTORY_FANFARE -> audioScope.launch { playVictoryFanfare() }
            SetCompletionSound.BOXING_BELL -> audioScope.launch { playBoxingBell() }
            SetCompletionSound.HEAVY_IRON -> audioScope.launch { playHeavyIron() }
            SetCompletionSound.SYSTEM_NOTIFICATION -> playSystemNotification(context)
            SetCompletionSound.VIBRATE_ONLY -> { /* Vibration already called */ }
        }
    }

    private fun playSuccessChime() {
        val sampleRate = 44100
        val durationMs = 400
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-6.0 * t)
            // Bright harmonic chord: E6 (1318Hz) and B6 (1975Hz)
            val s1 = sin(2.0 * Math.PI * 1318.5 * t) * 0.5
            val s2 = sin(2.0 * Math.PI * 1975.5 * t) * 0.4
            val s3 = sin(2.0 * Math.PI * 2637.0 * t) * 0.2
            val sample = (s1 + s2 + s3) * decay
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playShortBeep() {
        val sampleRate = 44100
        val durationMs = 160
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-8.0 * t)
            // Fast upward chirp: 1100Hz -> 1760Hz
            val freq = 1100.0 + (660.0 * (i.toDouble() / totalSamples))
            val sample = sin(2.0 * Math.PI * freq * t) * 0.7 * decay
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playVictoryFanfare() {
        val sampleRate = 44100
        val durationMs = 360
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        val noteDuration = totalSamples / 3
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = (i / noteDuration).coerceIn(0, 2)
            // G5 (784Hz) -> C6 (1046Hz) -> E6 (1318Hz)
            val freq = when (noteIdx) {
                0 -> 783.99
                1 -> 1046.50
                else -> 1318.51
            }
            val localT = (i % noteDuration).toDouble() / sampleRate
            val decay = Math.exp(-4.5 * localT)
            val sample = sin(2.0 * Math.PI * freq * t) * 0.65 * decay
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playBoxingBell() {
        val sampleRate = 44100
        val durationMs = 600
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-5.0 * t)
            val s1 = sin(2.0 * Math.PI * 880.0 * t) * 0.55
            val s2 = sin(2.0 * Math.PI * 1320.0 * t) * 0.35
            val s3 = sin(2.0 * Math.PI * 1760.0 * t) * 0.20
            val sample = (s1 + s2 + s3) * decay
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playHeavyIron() {
        val sampleRate = 44100
        val durationMs = 350
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-7.0 * t)
            // Deep resonant punch: 130Hz fundamental + 260Hz overtone
            val s1 = sin(2.0 * Math.PI * 130.81 * t) * 0.7
            val s2 = sin(2.0 * Math.PI * 261.63 * t) * 0.4
            val sample = (s1 + s2) * decay
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playSystemNotification(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (_: Exception) {
            audioScope.launch { playSuccessChime() }
        }
    }

    private fun writeAndPlayPcm(buffer: ShortArray, sampleRate: Int) {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, buffer.size * 2)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            track.write(buffer, 0, buffer.size)
            track.play()

            audioScope.launch {
                val durationMs = (buffer.size * 1000L / sampleRate) + 150L
                kotlinx.coroutines.delay(durationMs)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
