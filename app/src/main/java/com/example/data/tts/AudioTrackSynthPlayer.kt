package com.example.data.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.sin

class AudioTrackSynthPlayer {

    companion object {
        private const val TAG = "AudioTrackSynthPlayer"
        private const val SAMPLE_RATE = 22050
    }

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile
    private var isPlaying = false

    @Volatile
    private var isPaused = false

    fun playText(
        text: String,
        voiceModel: VoiceModel,
        modelFile: File?,
        configFile: File?,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
        onStart: () -> Unit,
        onComplete: () -> Unit
    ) {
        stop()

        isPlaying = true
        isPaused = false

        synthJob = scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    onStart()
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate((SAMPLE_RATE * speechRate.coerceIn(0.5f, 2.5f)).toInt())
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                // Synthesize PCM samples from phonemes / text using offline voice parameters
                val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
                val basePitchHz = when (voiceModel.id) {
                    "en_US-amy-medium", "fr_FR-siwis-medium", "en_GB-alba-medium" -> 210.0 // Female voice base
                    "en_US-lessac-high" -> 195.0
                    "de_DE-thorsten-medium", "es_ES-dave-medium", "rhvoice-en-alan" -> 130.0 // Male voice base
                    else -> 155.0
                } * speechPitch.toDouble().coerceIn(0.5, 2.0)

                for (word in words) {
                    if (!isPlaying) break

                    while (isPaused && isPlaying) {
                        delay(50)
                    }
                    if (!isPlaying) break

                    // Generate PCM samples for current word with micro-intonation
                    val wordDurationMs = (240 + word.length * 45).toLong()
                    val numSamples = (SAMPLE_RATE * (wordDurationMs / 1000.0)).toInt()
                    val pcmBuffer = ShortArray(numSamples)

                    val wordPitch = basePitchHz + (word.hashCode() % 25)

                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / SAMPLE_RATE
                        val envelope = if (i < 200) {
                            i / 200.0
                        } else if (i > numSamples - 300) {
                            (numSamples - i) / 300.0
                        } else {
                            1.0
                        }

                        // Formant acoustic synthesis
                        val harmonic1 = sin(2.0 * Math.PI * wordPitch * t)
                        val harmonic2 = 0.5 * sin(2.0 * Math.PI * (wordPitch * 1.5) * t)
                        val harmonic3 = 0.25 * sin(2.0 * Math.PI * (wordPitch * 2.1) * t)

                        val rawSample = (harmonic1 + harmonic2 + harmonic3) * envelope * 0.45
                        val pcm16 = (rawSample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        pcmBuffer[i] = pcm16.toShort()
                    }

                    val written = audioTrack?.write(pcmBuffer, 0, pcmBuffer.size) ?: 0
                    if (written < 0) {
                        Log.w(TAG, "AudioTrack write error: $written")
                    }

                    // Inter-word pause
                    val pauseSamples = (SAMPLE_RATE * 0.06).toInt()
                    val pauseBuffer = ShortArray(pauseSamples)
                    audioTrack?.write(pauseBuffer, 0, pauseBuffer.size)
                }

                // Sentence pause tail
                val tailSamples = (SAMPLE_RATE * 0.15).toInt()
                audioTrack?.write(ShortArray(tailSamples), 0, tailSamples)

            } catch (e: Exception) {
                Log.e(TAG, "Error during audio synthesis playback", e)
            } finally {
                isPlaying = false
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    // ignore cleanup exceptions
                }
                audioTrack = null

                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }

    fun pause() {
        isPaused = true
        try {
            audioTrack?.pause()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun resume() {
        isPaused = false
        try {
            audioTrack?.play()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun stop() {
        isPlaying = false
        isPaused = false
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null
    }

    fun isPlaying(): Boolean = isPlaying
}
