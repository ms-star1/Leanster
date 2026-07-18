package com.beispiel.ridetracker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.*

class VoiceCoach(private val context: Context) {

    enum class CoachEvent { NEW_PB, NEAR_PB, SESSION_START, SESSION_END, GHOST_AHEAD, GHOST_BEHIND }

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("voice_tts_enabled", false)
        set(v) = prefs.edit().putBoolean("voice_tts_enabled", v).apply()

    var beepsEnabled: Boolean
        get() = prefs.getBoolean("voice_beeps_enabled", true)
        set(v) = prefs.edit().putBoolean("voice_beeps_enabled", v).apply()

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.15f)
            }
        }
    }

    fun trigger(event: CoachEvent, lean: Float = 0f) {
        scope.launch {
            when (event) {
                CoachEvent.NEW_PB -> {
                    playPattern(listOf(900 to 150, 1200 to 150, 1500 to 150), gapMs = 50)
                    speak("New personal best, ${lean.toInt()} degrees")
                }
                CoachEvent.NEAR_PB -> {
                    playPattern(listOf(1200 to 100, 0 to 100, 1200 to 100), gapMs = 0)
                    speak("${lean.toInt()} degrees off your best")
                }
                CoachEvent.SESSION_START -> {
                    playPattern(listOf(900 to 120, 1200 to 120, 1500 to 120), gapMs = 40)
                    speak("Session started")
                }
                CoachEvent.SESSION_END -> {
                    playPattern(listOf(1500 to 120, 1200 to 120, 900 to 120), gapMs = 40)
                    speak("Session ended")
                }
                CoachEvent.GHOST_AHEAD -> {
                    playBeep(1500, 200)
                    speak("Ahead of your ghost")
                }
                CoachEvent.GHOST_BEHIND -> {
                    playBeep(900, 200)
                    speak("Ghost is pulling away")
                }
            }
        }
    }

    private fun speak(text: String) {
        if (ttsEnabled && ttsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private suspend fun playPattern(tones: List<Pair<Int, Int>>, gapMs: Int) {
        if (!beepsEnabled) return
        for ((freq, dur) in tones) {
            if (freq > 0) playBeep(freq, dur)
            if (gapMs > 0) delay(dur.toLong() + gapMs)
        }
    }

    private fun playBeep(frequencyHz: Int, durationMs: Int) {
        if (!beepsEnabled) return
        val sampleRate = 44100
        val numSamples = sampleRate * durationMs / 1000
        val buffer = ShortArray(numSamples)

        // Sine wave with short fade in/out (10ms) to avoid clicks
        val fadeSamples = minOf(sampleRate * 10 / 1000, numSamples / 4)
        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i.toDouble() * frequencyHz / sampleRate
            val amplitude = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i > numSamples - fadeSamples -> (numSamples - i).toDouble() / fadeSamples
                else -> 1.0
            }
            buffer[i] = (sin(angle) * amplitude * 0.85 * Short.MAX_VALUE).toInt().toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.setVolume(AudioTrack.getMaxVolume())
            // Request audio focus to temporarily duck other audio
            track.play()
            Thread.sleep(durationMs.toLong() + 50)
            track.stop()
            track.release()
        } catch (_: Exception) { /* audio unavailable */ }
    }

    fun shutdown() {
        scope.cancel()
        tts?.shutdown()
        tts = null
    }
}
