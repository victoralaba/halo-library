package com.example.data.tts

import android.content.Context
import android.util.Log
import java.io.File

class PiperEngine(private val context: Context) {

    companion object {
        private const val TAG = "PiperEngine"
        val ENGINE_VERSION = "1.2.0 (Bundled ONNX Runtime)"
    }

    private val synthPlayer = AudioTrackSynthPlayer()

    fun isReady(): Boolean = true

    fun speak(
        text: String,
        voiceModel: VoiceModel,
        modelFile: File?,
        configFile: File?,
        speechRate: Float,
        speechPitch: Float,
        onStart: () -> Unit,
        onComplete: () -> Unit
    ) {
        Log.d(TAG, "Piper ONNX synthesis starting for voice ${voiceModel.id} [Model File: ${modelFile?.name}]")

        synthPlayer.playText(
            text = text,
            voiceModel = voiceModel,
            modelFile = modelFile,
            configFile = configFile,
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
