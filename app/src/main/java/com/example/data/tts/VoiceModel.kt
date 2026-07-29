package com.example.data.tts

enum class TtsEngineType(val displayName: String, val badge: String) {
    PIPER_ONNX("Piper TTS (ONNX)", "Piper ONNX"),
    RHVOICE("RHVoice Engine", "RHVoice")
}

sealed interface VoiceDownloadState {
    object NotDownloaded : VoiceDownloadState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : VoiceDownloadState
    object Downloaded : VoiceDownloadState
    data class Error(val message: String) : VoiceDownloadState
}

data class VoiceModel(
    val id: String,
    val name: String,
    val language: String,
    val languageCode: String,
    val engineType: TtsEngineType,
    val quality: String,
    val sizeMb: Float,
    val modelUrl: String,
    val configUrl: String,
    val sampleText: String = "Hello! This is a preview of bundled offline neural text-to-speech."
) {
    val sizeFormatted: String
        get() = "${sizeMb.toInt()} MB"
}

object VoiceCatalog {
    val sampleVoices = listOf(
        VoiceModel(
            id = "en_US-amy-medium",
            name = "Amy",
            language = "English (US)",
            languageCode = "en_US",
            engineType = TtsEngineType.PIPER_ONNX,
            quality = "Medium",
            sizeMb = 25.0f,
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx",
            configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx.json",
            sampleText = "The journey of a thousand miles begins with a single step."
        ),
        VoiceModel(
            id = "en_US-lessac-high",
            name = "Lessac",
            language = "English (US)",
            languageCode = "en_US",
            engineType = TtsEngineType.PIPER_ONNX,
            quality = "High",
            sizeMb = 18.0f,
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/high/en_US-lessac-high.onnx",
            configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/high/en_US-lessac-high.onnx.json",
            sampleText = "Reading is to the mind what exercise is to the body."
        ),
        VoiceModel(
            id = "en_GB-alba-medium",
            name = "Alba",
            language = "English (UK)",
            languageCode = "en_GB",
            engineType = TtsEngineType.PIPER_ONNX,
            quality = "Medium",
            sizeMb = 22.0f,
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/alba/medium/en_GB-alba-medium.onnx",
            configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_GB/alba/medium/en_GB-alba-medium.onnx.json",
            sampleText = "Good evening! Welcome to your digital bookshelf library."
        ),
        VoiceModel(
            id = "fr_FR-siwis-medium",
            name = "Siwis",
            language = "French",
            languageCode = "fr_FR",
            engineType = TtsEngineType.PIPER_ONNX,
            quality = "Medium",
            sizeMb = 30.0f,
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/medium/fr_FR-siwis-medium.onnx",
            configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/fr/fr_FR/siwis/medium/fr_FR-siwis-medium.onnx.json",
            sampleText = "Bonjour! La lecture enrichit l'esprit et nourrit l'imagination."
        ),
        VoiceModel(
            id = "de_DE-thorsten-medium",
            name = "Thorsten",
            language = "German",
            languageCode = "de_DE",
            engineType = TtsEngineType.PIPER_ONNX,
            quality = "Medium",
            sizeMb = 28.0f,
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium/de_DE-thorsten-medium.onnx",
            configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium/de_DE-thorsten-medium.onnx.json",
            sampleText = "Guten Tag! Willkommen zu Ihrer Offline-Bibliothek."
        ),
        VoiceModel(
            id = "es_ES-dave-medium",
            name = "Dave",
            language = "Spanish",
            languageCode = "es_ES",
            engineType = TtsEngineType.PIPER_ONNX,
            quality = "Medium",
            sizeMb = 27.0f,
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/dave/medium/es_ES-dave-medium.onnx",
            configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/es/es_ES/dave/medium/es_ES-dave-medium.onnx.json",
            sampleText = "¡Hola! Bienvenido a tu lector de libros electrónicos favorito."
        ),
        VoiceModel(
            id = "it_IT-riccardo-x_low",
            name = "Riccardo",
            language = "Italian",
            languageCode = "it_IT",
            engineType = TtsEngineType.PIPER_ONNX,
            quality = "Fast HD",
            sizeMb = 16.0f,
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/it/it_IT/riccardo/x_low/it_IT-riccardo-x_low.onnx",
            configUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/it/it_IT/riccardo/x_low/it_IT-riccardo-x_low.onnx.json",
            sampleText = "Ciao! La lettura è la chiave del sapere e dell'immaginazione."
        ),
        VoiceModel(
            id = "rhvoice-en-alan",
            name = "Alan",
            language = "English (RHVoice)",
            languageCode = "en_US",
            engineType = TtsEngineType.RHVOICE,
            quality = "Neural Fast",
            sizeMb = 12.0f,
            modelUrl = "https://raw.githubusercontent.com/RHVoice/RHVoice/master/data/voices/alan/voice.info",
            configUrl = "https://raw.githubusercontent.com/RHVoice/RHVoice/master/data/voices/alan/voice.config",
            sampleText = "RHVoice provides lightweight, responsive offline speech synthesis."
        ),
        VoiceModel(
            id = "rhvoice-eo-spiketo",
            name = "Spiketo",
            language = "Esperanto (RHVoice)",
            languageCode = "eo",
            engineType = TtsEngineType.RHVOICE,
            quality = "Natural",
            sizeMb = 10.0f,
            modelUrl = "https://raw.githubusercontent.com/RHVoice/RHVoice/master/data/voices/spiketo/voice.info",
            configUrl = "https://raw.githubusercontent.com/RHVoice/RHVoice/master/data/voices/spiketo/voice.config",
            sampleText = "Saluton! RHVoice estas rapida kaj tute senretas voĉosintezilo."
        ),
        VoiceModel(
            id = "rhvoice-ru-evgeniy",
            name = "Evgeniy",
            language = "Russian (RHVoice)",
            languageCode = "ru_RU",
            engineType = TtsEngineType.RHVOICE,
            quality = "Natural HD",
            sizeMb = 14.0f,
            modelUrl = "https://raw.githubusercontent.com/RHVoice/RHVoice/master/data/voices/evgeniy/voice.info",
            configUrl = "https://raw.githubusercontent.com/RHVoice/RHVoice/master/data/voices/evgeniy/voice.config",
            sampleText = "Здравствуйте! Чтение открывает новые горизонты."
        )
    )
}
