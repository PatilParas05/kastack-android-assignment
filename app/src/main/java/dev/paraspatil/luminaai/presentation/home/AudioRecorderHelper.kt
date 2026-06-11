package dev.paraspatil.luminaai.presentation.home

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.sqrt

class AudioRecorderHelper {

    @SuppressLint("MissingPermission")
    fun getAmplitudeFlow(): Flow<Float> = flow {
        val sampleRate = 8000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        var audioRecord: AudioRecord? = null

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize
            )

            audioRecord.startRecording()
            val buffer = ShortArray(minBufferSize)

            // Keep reading audio data as long as the Coroutine is active
            while (currentCoroutineContext().isActive) {
                val readSize = audioRecord.read(buffer, 0, minBufferSize)
                if (readSize > 0) {
                    var sum = 0.0
                    for (i in 0 until readSize) {
                        sum += buffer[i] * buffer[i]
                    }

                    // Calculate Root Mean Square (RMS) to get actual volume amplitude
                    val rms = sqrt(sum / readSize)

                    // Normalize the value. Short.MAX_VALUE is 32767.
                    // Normal speech is much lower, so we multiply by 5 to make the UI more reactive.
                    val normalizedAmplitude = ((rms / Short.MAX_VALUE.toDouble()) * 5).toFloat()

                    emit(normalizedAmplitude.coerceIn(0f, 1f))
                }
                delay(50) // Emit every 50ms for a smooth 20fps UI update
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(0f)
        } finally {
            // ALWAYS release audio resources to prevent memory leaks
            audioRecord?.stop()
            audioRecord?.release()
        }
    }.flowOn(Dispatchers.IO) // Must run on background thread so we don't freeze the UI!
}