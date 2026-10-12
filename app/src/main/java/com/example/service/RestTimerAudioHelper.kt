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

enum class RestTimerSound(val id: String, val titleUk: String, val descriptionUk: String, val iconEmoji: String) {
    GONG("gong", "Боксерський гонг", "Класичний спортивний дзвін початку раунду", "🔔"),
    DIGITAL_BEEP("digital_beep", "Цифровий таймер", "Енергійний потрійний біп секундоміра", "⏱️"),
    WHISTLE("whistle", "Свисток тренера", "Подвійний спортивний свисток рефері", "📢"),
    CHIME("chime", "М'який передзвін", "Мелодійний приємний акорд", "✨"),
    SYSTEM_NOTIFICATION("system", "Системний сигнал", "Стандартне системне сповіщення Android", "📱"),
    VIBRATE_ONLY("vibrate_only", "Тільки вібрація", "Без звуку, лише подвійна тактильна вібрація", "📳")
}

object RestTimerAudioHelper {
    private const val PREFS_NAME = "apex_rest_timer_prefs"
    private const val KEY_SOUND = "selected_rest_sound"

    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun getSelectedSound(context: Context): RestTimerSound {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val soundId = prefs.getString(KEY_SOUND, RestTimerSound.GONG.id)
        return RestTimerSound.values().find { it.id == soundId } ?: RestTimerSound.GONG
    }

    fun setSelectedSound(context: Context, sound: RestTimerSound) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOUND, sound.id)
            .apply()
    }

    fun playTick(context: Context) {
        val selected = getSelectedSound(context)
        vibrate(context, 35)
        if (selected != RestTimerSound.VIBRATE_ONLY) {
            audioScope.launch {
                playPcmTone(frequency = 880.0, durationMs = 60, amplitude = 0.4)
            }
        }
    }

    fun playFinishSound(context: Context) {
        val selected = getSelectedSound(context)
        // Always vibrate on finish
        vibrate(context, 400)

        when (selected) {
            RestTimerSound.GONG -> audioScope.launch { playGong() }
            RestTimerSound.DIGITAL_BEEP -> audioScope.launch { playDigitalBeeps() }
            RestTimerSound.WHISTLE -> audioScope.launch { playWhistle() }
            RestTimerSound.CHIME -> audioScope.launch { playChime() }
            RestTimerSound.SYSTEM_NOTIFICATION -> playSystemNotification(context)
            RestTimerSound.VIBRATE_ONLY -> { /* Vibration only */ }
        }
    }

    fun playPreviewSound(context: Context, sound: RestTimerSound) {
        vibrate(context, 150)
        when (sound) {
            RestTimerSound.GONG -> audioScope.launch { playGong() }
            RestTimerSound.DIGITAL_BEEP -> audioScope.launch { playDigitalBeeps() }
            RestTimerSound.WHISTLE -> audioScope.launch { playWhistle() }
            RestTimerSound.CHIME -> audioScope.launch { playChime() }
            RestTimerSound.SYSTEM_NOTIFICATION -> playSystemNotification(context)
            RestTimerSound.VIBRATE_ONLY -> { /* Vibrate already called */ }
        }
    }

    private fun playSystemNotification(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (_: Exception) {
            audioScope.launch { playDigitalBeeps() }
        }
    }

    private fun playGong() {
        val sampleRate = 44100
        val durationMs = 1200
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-3.5 * t)
            // Superposition of resonant bell harmonics
            val s1 = sin(2.0 * Math.PI * 220.0 * t) * 0.45
            val s2 = sin(2.0 * Math.PI * 330.0 * t) * 0.30
            val s3 = sin(2.0 * Math.PI * 440.0 * t) * 0.15
            val s4 = sin(2.0 * Math.PI * 660.0 * t) * 0.10
            val sample = (s1 + s2 + s3 + s4) * decay
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playDigitalBeeps() {
        val sampleRate = 44100
        val durationMs = 500
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val tMs = (i * 1000) / sampleRate
            val inBeep = (tMs in 0..80) || (tMs in 140..220) || (tMs in 280..420)
            val freq = if (tMs in 280..420) 1200.0 else 1000.0
            if (inBeep) {
                val t = i.toDouble() / sampleRate
                val sample = sin(2.0 * Math.PI * freq * t) * 0.7
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            } else {
                buffer[i] = 0
            }
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playWhistle() {
        val sampleRate = 44100
        val durationMs = 450
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val tMs = (i * 1000) / sampleRate
            val inBlast = (tMs in 0..140) || (tMs in 200..420)
            if (inBlast) {
                val t = i.toDouble() / sampleRate
                // Frequency modulation trill at 35Hz
                val instantFreq = 2600.0 + 200.0 * sin(2.0 * Math.PI * 35.0 * t)
                val sample = sin(2.0 * Math.PI * instantFreq * t) * 0.65
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            } else {
                buffer[i] = 0
            }
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playChime() {
        val sampleRate = 44100
        val durationMs = 1000
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val decay = Math.exp(-3.0 * t)
            // C6 (1046Hz), E6 (1318Hz), G6 (1568Hz)
            val s1 = sin(2.0 * Math.PI * 1046.5 * t) * 0.4
            val s2 = sin(2.0 * Math.PI * 1318.5 * t) * 0.35
            val s3 = sin(2.0 * Math.PI * 1568.0 * t) * 0.25
            val sample = (s1 + s2 + s3) * decay
            buffer[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
    }

    private fun playPcmTone(frequency: Double, durationMs: Int, amplitude: Double = 0.5) {
        val sampleRate = 44100
        val totalSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val sample = sin(2.0 * Math.PI * frequency * t) * amplitude
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        writeAndPlayPcm(buffer, sampleRate)
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
                .setUsage(AudioAttributes.USAGE_ALARM)
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

            // Release after playing duration
            audioScope.launch {
                val durationMs = (buffer.size * 1000L / sampleRate) + 200L
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
