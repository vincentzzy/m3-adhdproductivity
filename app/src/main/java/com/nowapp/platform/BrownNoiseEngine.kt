package com.nowapp.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.random.Random

/**
 * Procedurally synthesised brown noise, streamed into an AudioTrack.
 *
 * There are no bundled audio assets: the sound is generated sample by sample by
 * a leaky integrator over white noise, which is what brown noise is. It fades in
 * over about half a second so starting it is never a jolt.
 */
class BrownNoiseEngine {

    private companion object {
        const val SAMPLE_RATE = 44_100
    }

    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var track: AudioTrack? = null

    val isRunning: Boolean get() = running

    fun start() {
        if (running) return
        running = true
        worker = thread(start = true, name = "brown-noise") { renderLoop() }
    }

    fun stop() {
        if (!running) return
        running = false
        runCatching { worker?.join(800) }
        worker = null
        track = null
    }

    fun release() = stop()

    private fun renderLoop() {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        val audio = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: run {
            running = false
            return
        }

        track = audio
        runCatching { audio.play() }

        val random = Random(System.nanoTime())
        val buffer = ShortArray(minBuffer / 2)
        var brown = 0f
        var gain = 0f

        while (running) {
            for (i in buffer.indices) {
                val white = random.nextFloat() * 2f - 1f
                brown = (brown + 0.02f * white) / 1.02f
                val sample = (brown * 3.2f).coerceIn(-1f, 1f) * gain
                buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
            // ~0.5s fade-in, then steady.
            if (gain < 1f) gain = (gain + 0.01f).coerceAtMost(1f)
            runCatching { audio.write(buffer, 0, buffer.size) }
        }

        runCatching { audio.stop() }
        runCatching { audio.release() }
    }
}
