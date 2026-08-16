package com.example.core.playback

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sin

class MorsePlaybackEngine(private val context: Context) {

    private val cameraManager by lazy { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    private var cameraId: String? = null

    init {
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            // Camera not available or no flash
        }
    }

    @Volatile
    private var isPlaying = false

    fun stop() {
        isPlaying = false
        setTorch(false)
        vibrator.cancel()
    }

    suspend fun play(
        morseCode: String,
        wpm: Int,
        pitchHz: Int,
        enableAudio: Boolean,
        enableHaptic: Boolean,
        enableFlash: Boolean,
        onPlaybackStateChanged: (Boolean) -> Unit
    ) {
        if (isPlaying) return
        isPlaying = true
        onPlaybackStateChanged(true)

        withContext(Dispatchers.Default) {
            // Timing based on WPM (Words Per Minute). Standard formula: DOT = 1.2 / WPM (seconds) -> 1200 / WPM ms
            val dotDurationMs = (1200 / wpm).toLong()
            val dashDurationMs = dotDurationMs * 3
            val interElementGapMs = dotDurationMs
            val shortGapMs = dotDurationMs * 3
            val mediumGapMs = dotDurationMs * 7

            val audioTrack = if (enableAudio) createAudioTrack() else null
            audioTrack?.play()

            try {
                for (char in morseCode) {
                    if (!isPlaying || !isActive) break

                    when (char) {
                        '.' -> {
                            playSignal(dotDurationMs, pitchHz, enableAudio, audioTrack, enableHaptic, enableFlash)
                            delay(interElementGapMs)
                        }
                        '-' -> {
                            playSignal(dashDurationMs, pitchHz, enableAudio, audioTrack, enableHaptic, enableFlash)
                            delay(interElementGapMs)
                        }
                        ' ' -> {
                            // Single space in morse array (between letters) is usually handled by splitting,
                            // but our string format might be ".- .-". The space between '.-' and '.-' is short gap.
                            delay(shortGapMs - interElementGapMs)
                        }
                        '/' -> {
                            // Word separator
                            delay(mediumGapMs - interElementGapMs)
                        }
                    }
                }
            } finally {
                stop()
                audioTrack?.stop()
                audioTrack?.release()
                onPlaybackStateChanged(false)
            }
        }
    }

    private suspend fun playSignal(
        durationMs: Long,
        pitchHz: Int,
        enableAudio: Boolean,
        audioTrack: AudioTrack?,
        enableHaptic: Boolean,
        enableFlash: Boolean
    ) {
        if (enableFlash) setTorch(true)
        if (enableHaptic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
        if (enableAudio && audioTrack != null) {
            val sampleRate = 44100
            val numSamples = (durationMs * sampleRate / 1000).toInt()
            val sample = ShortArray(numSamples)
            val freq = pitchHz.toDouble()

            for (i in 0 until numSamples) {
                val value = sin(2 * Math.PI * i / (sampleRate / freq))
                sample[i] = (value * Short.MAX_VALUE).toInt().toShort()
            }
            // Apply fade in/out to avoid clicking
            val fadeSamples = (0.01 * sampleRate).toInt().coerceAtMost(numSamples / 2) // 10ms fade
            for (i in 0 until fadeSamples) {
                val multiplier = i.toDouble() / fadeSamples
                sample[i] = (sample[i] * multiplier).toInt().toShort()
                sample[numSamples - 1 - i] = (sample[numSamples - 1 - i] * multiplier).toInt().toShort()
            }

            audioTrack.write(sample, 0, numSamples)
        } else {
            delay(durationMs)
        }
        if (enableFlash) setTorch(false)
    }

    private fun setTorch(on: Boolean) {
        try {
            cameraId?.let {
                cameraManager.setTorchMode(it, on)
            }
        } catch (e: Exception) {
            // Ignore if camera is busy or unavailable
        }
    }

    private fun createAudioTrack(): AudioTrack {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }
}
