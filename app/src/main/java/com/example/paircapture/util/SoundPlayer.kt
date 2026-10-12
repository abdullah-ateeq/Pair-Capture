package com.example.paircapture.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class GameSound { REVEAL, MATCH, MISMATCH, CPU_TURN, VICTORY, DEFEAT }

/**
 * Tiny synthesized sound effects (soft sine blips), so the app ships with no audio assets.
 * Playback runs on a single background thread and failures are silently ignored.
 */
class SoundPlayer {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val buffers: Map<GameSound, ShortArray> by lazy {
        mapOf(
            GameSound.REVEAL to tones(880.0 to 45),
            GameSound.MATCH to tones(660.0 to 70, 990.0 to 110),
            GameSound.MISMATCH to tones(392.0 to 90, 330.0 to 110),
            GameSound.CPU_TURN to tones(523.0 to 50),
            GameSound.VICTORY to tones(523.0 to 90, 659.0 to 90, 784.0 to 90, 1047.0 to 180),
            GameSound.DEFEAT to tones(440.0 to 120, 370.0 to 120, 294.0 to 200)
        )
    }

    fun play(sound: GameSound) {
        if (executor.isShutdown) return
        executor.execute {
            runCatching {
                val data = buffers.getValue(sound)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(data.size * 2)
                    .build()
                try {
                    track.write(data, 0, data.size)
                    track.play()
                    Thread.sleep(data.size * 1000L / SAMPLE_RATE + 30)
                } finally {
                    track.release()
                }
            }
        }
    }

    fun release() {
        executor.shutdownNow()
    }

    private fun tones(vararg notes: Pair<Double, Int>): ShortArray {
        val samples = ArrayList<Short>()
        for ((frequency, durationMs) in notes) {
            val count = SAMPLE_RATE * durationMs / 1000
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = (i / (SAMPLE_RATE * 0.006)).coerceAtMost(1.0)
                val decay = exp(-4.0 * i / count)
                val value = sin(2 * PI * frequency * t) * attack * decay * VOLUME
                samples += (value * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return samples.toShortArray()
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val VOLUME = 0.22
    }
}
