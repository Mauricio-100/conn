package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.sin
import kotlin.math.PI

object CatSoundPlayer {
    private const val TAG = "CatSoundPlayer"

    /**
     * Generates and plays a highly realistic, cute kitten "meow" sound using real-time
     * additive frequency-modulation synthesis through AudioTrack.
     */
    fun playCuteMeow() {
        Thread {
            var audioTrack: AudioTrack? = null
            try {
                val sampleRate = 22050
                val duration = 0.75f // seconds
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)

                var phase = 0.0
                var vibratoPhase = 0.0

                for (i in 0 until numSamples) {
                    val t = i.toFloat() / sampleRate

                    // Cute kitten meow pitch envelope (frequency sweep):
                    // Starts at ~380Hz, climbs up to ~780Hz (high-pitched "ee" sound),
                    // then curves down softly to ~300Hz ("ow" sound).
                    val baseFreq = if (t < 0.2f) {
                        // Rapid rise
                        380.0 + (780.0 - 380.0) * (t / 0.2f)
                    } else if (t < 0.45f) {
                        // High plateau with slight peak
                        780.0 + 40.0 * sin((t - 0.2) / 0.25 * PI / 2.0)
                    } else {
                        // Gentle fall
                        820.0 - (820.0 - 320.0) * ((t - 0.45f) / (duration - 0.45f))
                    }

                    // Kitten pitch has a slight tiny vibrato (tremble) around 7Hz for realism
                    val vibratoFreq = 7.0
                    val vibratoDepth = 8.0 // Hz
                    val freq = baseFreq + vibratoDepth * sin(vibratoPhase)
                    vibratoPhase += 2.0 * PI * vibratoFreq / sampleRate

                    // Waveform synthesis:
                    // A real cat vocalization is rich in harmonics.
                    // We blend the fundamental sine wave with a 2nd harmonic, 3rd harmonic, and some soft noise/resonance.
                    val fundamental = sin(phase)
                    val secondHarmonic = 0.3 * sin(2.0 * phase)
                    val thirdHarmonic = 0.15 * sin(3.0 * phase)
                    val fourthHarmonic = 0.08 * sin(4.0 * phase)

                    // Combine and normalize harmonics
                    var signal = fundamental + secondHarmonic + thirdHarmonic + fourthHarmonic

                    // Advance phase
                    phase += 2.0 * PI * freq / sampleRate

                    // Amplitude Envelope (attack-decay-sustain-release):
                    // Quick attack (fade in), steady sustain, long natural release (fade out).
                    val amp: Double = if (t < 0.08f) {
                        (t / 0.08f).toDouble() // Attack
                    } else if (t < 0.35f) {
                        1.0 // Sustain
                    } else {
                        // Smooth exponential-like decay for a sweet fading tail
                        val progress = (t - 0.35f) / (duration - 0.35f)
                        1.0 - progress.toDouble()
                    }

                    // Apply amplitude and scale to 16-bit PCM range (max 32767)
                    buffer[i] = (signal * amp * 10000.0).toInt().coerceIn(-32768, 32767).toShort()
                }

                // Construct and configure AudioTrack
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
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
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                // Write synthetic audio to the buffer, play, and await release
                audioTrack.write(buffer, 0, numSamples)
                audioTrack.play()

                Thread.sleep((duration * 1000).toLong() + 50)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing synthetic meow", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (ex: Exception) {
                    // Quietly ignore
                }
            }
        }.start()
    }
}
