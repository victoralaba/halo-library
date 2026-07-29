package com.example.data.tts

import android.content.Context
import android.util.Log
import java.io.File

class RhVoiceEngine(private val context: Context) {

    companion object {
        private const val TAG = "RhVoiceEngine"
        val ENGINE_VERSION = "1.8.0 (Bundled RHVoice Native)"
    }

    private val synthPlayer = AudioTrackSynthPlayer()

    fun isReady(): Boolean = true

    fun speak(
        text: String,
        voiceModel: VoiceModel,
        voiceDataFile: File?,
        speechRate: Float,
        speechPitch: Float,
        onStart: () -> Unit,
        onComplete: () -> Unit
    ) {
        Log.d(TAG, "RHVoice synthesis starting for voice ${voiceModel.id} [Data File: ${voiceDataFile?.name}]")

        synthPlayer.playText(
            text = text,
            voiceModel = voiceModel,
            modelFile = voiceDataFile,
            configFile = null,
            speechRate = speechRate,
            speechPitch = speechPitch,
            onStart = onStart,
            onComplete = onComplete
        )
    }

    fun stop() {
        synthPlayer.stop()
    }

    fun pause() {
        synthPlayer.pause()
    }

    fun resume() {
        synthPlayer.resume()
    }
}
