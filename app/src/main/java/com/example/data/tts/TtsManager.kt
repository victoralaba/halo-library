package com.example.data.tts

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TtsPlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentSentenceIndex: Int = -1,
    val currentParagraphIndex: Int = -1,
    val activeTextSnippet: String = "",
    val selectedVoiceId: String = "en_US-amy-medium",
    val selectedVoiceName: String = "Amy (English US)",
    val selectedVoiceEngine: TtsEngineType = TtsEngineType.PIPER_ONNX,
    val selectedVoice: String = "Amy (English US)", // Compatibility field
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val statusMessage: String = "",
    val isVoiceDownloaded: Boolean = false
)

class TtsManager(private val context: Context) {
    companion object {
        private const val TAG = "TtsManager"
        private const val PREF_SELECTED_VOICE = "selected_tts_voice_id"
        private const val PREF_SPEECH_RATE = "tts_speech_rate"
        private const val PREF_SPEECH_PITCH = "tts_speech_pitch"
    }

    private val prefs = context.getSharedPreferences("lumina_tts_prefs", Context.MODE_PRIVATE)

    val piperEngine = PiperEngine(context)
    val rhVoiceEngine = RhVoiceEngine(context)
    val voiceDownloader = VoiceDownloader(context)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playbackState = MutableStateFlow(TtsPlaybackState())
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    val downloadStates: StateFlow<Map<String, VoiceDownloadState>> = voiceDownloader.downloadStates

    private var sentenceList: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var paragraphIndex: Int = 0

    private var onSentenceHighlightChanged: ((sentenceIndex: Int, text: String) -> Unit)? = null
    private var onPlaybackFinished: (() -> Unit)? = null

    init {
        val savedVoiceId = prefs.getString(PREF_SELECTED_VOICE, "en_US-amy-medium") ?: "en_US-amy-medium"
        val savedRate = prefs.getFloat(PREF_SPEECH_RATE, 1.0f)
        val savedPitch = prefs.getFloat(PREF_SPEECH_PITCH, 1.0f)

        setSelectedVoice(savedVoiceId)
        setSpeechRate(savedRate)
        setSpeechPitch(savedPitch)

        // Monitor downloads to update active voice status
        scope.launch {
            voiceDownloader.downloadStates.collect { states ->
                val currentVoiceId = _playbackState.value.selectedVoiceId
                val isDownloaded = states[currentVoiceId] is VoiceDownloadState.Downloaded
                _playbackState.update { it.copy(isVoiceDownloaded = isDownloaded) }
            }
        }
    }

    fun getAllCatalogVoices(): List<VoiceModel> {
        return VoiceCatalog.sampleVoices
    }

    fun getSelectedVoiceModel(): VoiceModel {
        val voiceId = _playbackState.value.selectedVoiceId
        return VoiceCatalog.sampleVoices.firstOrNull { it.id == voiceId }
            ?: VoiceCatalog.sampleVoices.first()
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.5f)
        prefs.edit().putFloat(PREF_SPEECH_RATE, clamped).apply()
        _playbackState.update { it.copy(speechRate = clamped) }
    }

    fun setSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(PREF_SPEECH_PITCH, clamped).apply()
        _playbackState.update { it.copy(speechPitch = clamped) }
    }

    fun setSelectedVoice(voiceId: String) {
        val model = VoiceCatalog.sampleVoices.firstOrNull { it.id == voiceId }
            ?: VoiceCatalog.sampleVoices.firstOrNull { it.name.equals(voiceId, ignoreCase = true) }
            ?: VoiceCatalog.sampleVoices.first()

        prefs.edit().putString(PREF_SELECTED_VOICE, model.id).apply()
        val isDownloaded = voiceDownloader.isVoiceDownloaded(model.id)

        _playbackState.update {
            it.copy(
                selectedVoiceId = model.id,
                selectedVoiceName = "${model.name} (${model.language})",
                selectedVoiceEngine = model.engineType,
                selectedVoice = "${model.name} (${model.language})",
                isVoiceDownloaded = isDownloaded,
                statusMessage = if (isDownloaded) "Ready (Bundled ${model.engineType.badge})" else "Voice model not downloaded yet"
            )
        }
    }

    fun downloadVoice(voice: VoiceModel) {
        voiceDownloader.downloadVoice(voice)
    }

    fun cancelDownload(voiceId: String) {
        voiceDownloader.cancelDownload(voiceId)
    }

    fun deleteVoice(voiceId: String) {
        voiceDownloader.deleteVoice(voiceId)
        if (_playbackState.value.selectedVoiceId == voiceId) {
            _playbackState.update { it.copy(isVoiceDownloaded = false, statusMessage = "Voice deleted") }
        }
    }

    fun playSentences(
        sentences: List<String>,
        startIndex: Int = 0,
        paraIndex: Int = 0,
        onHighlight: (sentenceIndex: Int, text: String) -> Unit,
        onFinished: () -> Unit
    ) {
        if (sentences.isEmpty()) return

        stop()

        this.sentenceList = sentences
        this.currentIndex = startIndex.coerceIn(0, sentences.size - 1)
        this.paragraphIndex = paraIndex
        this.onSentenceHighlightChanged = onHighlight
        this.onPlaybackFinished = onFinished

        speakCurrentSentence()
    }

    private fun speakCurrentSentence() {
        if (currentIndex >= sentenceList.size) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    statusMessage = "Playback completed"
                )
            }
            onPlaybackFinished?.invoke()
            return
        }

        val textToSpeak = sentenceList[currentIndex].trim()
        if (textToSpeak.isBlank()) {
            currentIndex++
            speakCurrentSentence()
            return
        }

        val selectedModel = getSelectedVoiceModel()
        val isDownloaded = voiceDownloader.isVoiceDownloaded(selectedModel.id)

        _playbackState.update {
            it.copy(
                isPlaying = true,
                isBuffering = false,
                currentSentenceIndex = currentIndex,
                currentParagraphIndex = paragraphIndex,
                activeTextSnippet = textToSpeak,
                statusMessage = if (isDownloaded)
                    "Speaking via ${selectedModel.engineType.badge} offline"
                else
                    "Synthesizing preview (download ${selectedModel.name} for full model)"
            )
        }

        onSentenceHighlightChanged?.invoke(currentIndex, textToSpeak)

        val currentState = _playbackState.value
        val modelFile = voiceDownloader.getModelFile(selectedModel.id)
        val configFile = voiceDownloader.getConfigFile(selectedModel.id)

        if (selectedModel.engineType == TtsEngineType.PIPER_ONNX) {
            piperEngine.speak(
                text = textToSpeak,
                voiceModel = selectedModel,
                modelFile = if (isDownloaded) modelFile else null,
                configFile = if (isDownloaded) configFile else null,
                speechRate = currentState.speechRate,
                speechPitch = currentState.speechPitch,
                onStart = {
                    _playbackState.update { it.copy(isBuffering = false) }
                },
                onComplete = {
                    advanceToNextSentence()
                }
            )
        } else {
            rhVoiceEngine.speak(
                text = textToSpeak,
                voiceModel = selectedModel,
                voiceDataFile = if (isDownloaded) modelFile else null,
                speechRate = currentState.speechRate,
                speechPitch = currentState.speechPitch,
                onStart = {
                    _playbackState.update { it.copy(isBuffering = false) }
                },
                onComplete = {
                    advanceToNextSentence()
                }
            )
        }
    }

    private fun advanceToNextSentence() {
        if (_playbackState.value.isPlaying) {
            currentIndex++
            speakCurrentSentence()
        }
    }

    fun pause() {
        piperEngine.pause()
        rhVoiceEngine.pause()
        _playbackState.update { it.copy(isPlaying = false, isBuffering = false) }
    }

    fun resume() {
        if (sentenceList.isNotEmpty() && currentIndex < sentenceList.size) {
            piperEngine.resume()
            rhVoiceEngine.resume()
            _playbackState.update { it.copy(isPlaying = true) }
        }
    }

    fun skipNext() {
        if (sentenceList.isNotEmpty() && currentIndex < sentenceList.size - 1) {
            stopEnginePlayback()
            currentIndex++
            speakCurrentSentence()
        }
    }

    fun skipPrevious() {
        if (sentenceList.isNotEmpty() && currentIndex > 0) {
            stopEnginePlayback()
            currentIndex--
            speakCurrentSentence()
        }
    }

    private fun stopEnginePlayback() {
        piperEngine.stop()
        rhVoiceEngine.stop()
    }

    fun stop() {
        stopEnginePlayback()
        _playbackState.update {
            it.copy(
                isPlaying = false,
                isBuffering = false,
                currentSentenceIndex = -1,
                activeTextSnippet = "",
                statusMessage = "Stopped"
            )
        }
    }

    fun previewVoice(sampleText: String = "Hello! This is a preview of the selected offline voice engine.") {
        stop()
        val selectedModel = getSelectedVoiceModel()
        val isDownloaded = voiceDownloader.isVoiceDownloaded(selectedModel.id)

        _playbackState.update {
            it.copy(
                isPlaying = true,
                activeTextSnippet = sampleText,
                statusMessage = "Testing ${selectedModel.name} voice preview..."
            )
        }

        val currentState = _playbackState.value
        val modelFile = voiceDownloader.getModelFile(selectedModel.id)
        val configFile = voiceDownloader.getConfigFile(selectedModel.id)

        if (selectedModel.engineType == TtsEngineType.PIPER_ONNX) {
            piperEngine.speak(
                text = sampleText,
                voiceModel = selectedModel,
                modelFile = if (isDownloaded) modelFile else null,
                configFile = if (isDownloaded) configFile else null,
                speechRate = currentState.speechRate,
                speechPitch = currentState.speechPitch,
                onStart = {},
                onComplete = {
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            activeTextSnippet = "",
                            statusMessage = "Preview finished"
                        )
                    }
                }
            )
        } else {
            rhVoiceEngine.speak(
                text = sampleText,
                voiceModel = selectedModel,
                voiceDataFile = if (isDownloaded) modelFile else null,
                speechRate = currentState.speechRate,
                speechPitch = currentState.speechPitch,
                onStart = {},
                onComplete = {
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            activeTextSnippet = "",
                            statusMessage = "Preview finished"
                        )
                    }
                }
            )
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
