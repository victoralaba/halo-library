package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tts.PiperEngine
import com.example.data.tts.RhVoiceEngine
import com.example.data.tts.TtsEngineType
import com.example.data.tts.TtsManager
import com.example.data.tts.VoiceDownloadState
import com.example.data.tts.VoiceModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSettingsScreen(
    ttsManager: TtsManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by ttsManager.playbackState.collectAsState()
    val downloadStates by ttsManager.downloadStates.collectAsState()

    var selectedEngineFilter by remember { mutableStateOf<TtsEngineType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var testText by remember { mutableStateOf("Reading is to the mind what exercise is to the body.") }

    val allVoices = remember { ttsManager.getAllCatalogVoices() }

    val filteredVoices = remember(allVoices, selectedEngineFilter, searchQuery) {
        allVoices.filter { voice ->
            val matchesEngine = selectedEngineFilter == null || voice.engineType == selectedEngineFilter
            val matchesQuery = searchQuery.isBlank() ||
                    voice.name.contains(searchQuery, ignoreCase = true) ||
                    voice.language.contains(searchQuery, ignoreCase = true) ||
                    voice.quality.contains(searchQuery, ignoreCase = true)
            matchesEngine && matchesQuery
        }
    }

    val downloadedCount = remember(downloadStates) {
        downloadStates.values.count { it is VoiceDownloadState.Downloaded }
    }

    val totalDownloadedMb = remember(downloadStates) {
        ttsManager.voiceDownloader.getTotalDownloadedMb()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bundled TTS Voice Engines", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("tts_settings_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Engine Status Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Embedded Offline Synthesizers",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Piper TTS & RHVoice Native Engines Bundled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "100% Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Piper ONNX Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(PiperEngine.ENGINE_VERSION, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column {
                                Text("RHVoice Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(RhVoiceEngine.ENGINE_VERSION, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Live Preview Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tts_preview_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Test & Live Preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            label = { Text("Preview Sample Text") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (playbackState.isPlaying) {
                                    ttsManager.stop()
                                } else {
                                    ttsManager.previewVoice(testText)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_voice_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (playbackState.isPlaying) "Stop Preview" else "Test Selected Voice (${playbackState.selectedVoiceName})")
                        }
                    }
                }
            }

            // Storage Summary & Filters Header
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Downloadable Voice Catalog",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$downloadedCount Installed (${String.format("%.1f", totalDownloadedMb)} MB)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Engines ship bundled with the app. Tap 'Download' to fetch `.onnx` model files directly to app storage for 100% offline synthesis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Engine Filter Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedEngineFilter == null,
                            onClick = { selectedEngineFilter = null },
                            label = { Text("All Engines (${allVoices.size})", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedEngineFilter == TtsEngineType.PIPER_ONNX,
                            onClick = { selectedEngineFilter = TtsEngineType.PIPER_ONNX },
                            label = { Text("Piper ONNX", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedEngineFilter == TtsEngineType.RHVOICE,
                            onClick = { selectedEngineFilter = TtsEngineType.RHVOICE },
                            label = { Text("RHVoice", fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by language or voice name...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_voice_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Voice Catalog Cards List
            items(filteredVoices) { voice ->
                val state = downloadStates[voice.id] ?: VoiceDownloadState.NotDownloaded
                val isSelected = playbackState.selectedVoiceId == voice.id

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${voice.language} - ${voice.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Quality: ${voice.quality} • ${voice.sizeFormatted}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                color = if (voice.engineType == TtsEngineType.PIPER_ONNX)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = voice.engineType.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (voice.engineType == TtsEngineType.PIPER_ONNX)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when (state) {
                            is VoiceDownloadState.NotDownloaded -> {
                                Button(
                                    onClick = { ttsManager.downloadVoice(voice) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("download_voice_${voice.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Download Voice (${voice.sizeFormatted})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            is VoiceDownloadState.Downloading -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Downloading voice model... ${(state.progress * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { ttsManager.cancelDownload(voice.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel download", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { state.progress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                                    )
                                }
                            }

                            is VoiceDownloadState.Downloaded -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { ttsManager.setSelectedVoice(voice.id) },
                                        enabled = !isSelected,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("select_voice_${voice.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = if (isSelected) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        else ButtonDefaults.filledTonalButtonColors(),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isSelected) "Active Voice" else "Select Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            ttsManager.setSelectedVoice(voice.id)
                                            ttsManager.previewVoice(voice.sampleText)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test", fontSize = 12.sp)
                                    }

                                    IconButton(
                                        onClick = { ttsManager.deleteVoice(voice.id) },
                                        modifier = Modifier.testTag("delete_voice_${voice.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete voice model",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            is VoiceDownloadState.Error -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Error: ${state.message}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { ttsManager.downloadVoice(voice) }) {
                                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Speech Parameters (Speed & Pitch)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Audio Synthesis Parameters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Rate
                        Text(
                            text = "Speech Rate: ${String.format("%.2fx", playbackState.speechRate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = playbackState.speechRate,
                            onValueChange = { ttsManager.setSpeechRate(it) },
                            valueRange = 0.5f..2.5f,
                            steps = 7,
                            modifier = Modifier.testTag("speech_rate_slider")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pitch
                        Text(
                            text = "Voice Pitch / Tone: ${String.format("%.2fx", playbackState.speechPitch)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = playbackState.speechPitch,
                            onValueChange = { ttsManager.setSpeechPitch(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 5,
                            modifier = Modifier.testTag("speech_pitch_slider")
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
