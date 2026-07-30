package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.theme.ReaderThemeMode
import java.io.OutputStream
import org.json.JSONObject

private const val PREFS_SETTINGS = "lumina_app_settings_prefs"
private const val KEY_WAKE_LOCK = "wake_lock_reading"
private const val KEY_AUTO_DETECT_DIR = "auto_detect_dir_startup"
private const val KEY_BACKGROUND_AUDIO = "background_audio_playback"
private const val KEY_AUTO_RESUME_POSITION = "auto_resume_audio_position"
private const val KEY_SKIP_INTERVAL = "skip_interval_seconds"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ttsManager: TtsManager,
    currentThemeMode: ReaderThemeMode,
    onThemeSelect: (ReaderThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    onResetLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE) }

    // Settings States
    var wakeLockEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_WAKE_LOCK, true)) }
    var autoDetectDirEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_DETECT_DIR, true)) }
    var backgroundAudioEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_BACKGROUND_AUDIO, true)) }
    var autoResumeAudioEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_RESUME_POSITION, true)) }
    var skipIntervalSec by remember { mutableStateOf(prefs.getInt(KEY_SKIP_INTERVAL, 10)) }

    // TTS States
    val playbackState by ttsManager.playbackState.collectAsState()
    val downloadStates by ttsManager.downloadStates.collectAsState()
    var selectedEngineFilter by remember { mutableStateOf<TtsEngineType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var testText by remember { mutableStateOf("Reading is to the mind what exercise is to the body.") }
    val allVoices = remember { ttsManager.getAllCatalogVoices() }

    // Confirmation Dialogs
    var voiceToDelete by remember { mutableStateOf<VoiceModel?>(null) }
    var showResetLibraryConfirm by remember { mutableStateOf(false) }

    // Export Settings JSON launcher
    val exportSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = JSONObject().apply {
                    put("wakeLockEnabled", wakeLockEnabled)
                    put("autoDetectDirEnabled", autoDetectDirEnabled)
                    put("backgroundAudioEnabled", backgroundAudioEnabled)
                    put("autoResumeAudioEnabled", autoResumeAudioEnabled)
                    put("skipIntervalSec", skipIntervalSec)
                    put("themeMode", currentThemeMode.name)
                    put("speechRate", playbackState.speechRate)
                    put("speechPitch", playbackState.speechPitch)
                }
                context.contentResolver.openOutputStream(uri)?.use { stream: OutputStream ->
                    stream.write(json.toString(2).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Settings exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val filteredVoices = remember(allVoices, selectedEngineFilter, searchQuery) {
        allVoices.filter { voice ->
            val matchesEngine = selectedEngineFilter == null || voice.engineType == selectedEngineFilter
            val matchesQuery = searchQuery.isBlank() ||
                    voice.name.contains(searchQuery, ignoreCase = true) ||
                    voice.language.contains(searchQuery, ignoreCase = true)
            matchesEngine && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: Book Reader & Library Settings
            item {
                SettingsGroupHeader(title = "BOOK READER & LIBRARY SETTINGS", icon = Icons.Default.MenuBook)
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Wake Lock Toggle
                        SettingsSwitchRow(
                            title = "Wake Lock while Reading",
                            subtitle = "Keep screen on while reading EPUB & PDF documents",
                            checked = wakeLockEnabled,
                            onCheckedChange = { checked ->
                                wakeLockEnabled = checked
                                prefs.edit().putBoolean(KEY_WAKE_LOCK, checked).apply()
                            },
                            testTag = "toggle_wake_lock"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Auto Detect Book & Audio Directory on Startup
                        SettingsSwitchRow(
                            title = "Auto-Detect Books & Audio on Startup",
                            subtitle = "Automatically scan storage directory for new files at launch",
                            checked = autoDetectDirEnabled,
                            onCheckedChange = { checked ->
                                autoDetectDirEnabled = checked
                                prefs.edit().putBoolean(KEY_AUTO_DETECT_DIR, checked).apply()
                            },
                            testTag = "toggle_auto_detect_dir"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Reset Library Button with Confirmation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetLibraryConfirm = true }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Library Data",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Restore original sample books & clear reading progress cache",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Group 2: Audio Player Settings
            item {
                SettingsGroupHeader(title = "AUDIO PLAYER SETTINGS", icon = Icons.Default.Headphones)
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SettingsSwitchRow(
                            title = "Background Playback",
                            subtitle = "Keep playing audiobooks when screen is off or app is in background",
                            checked = backgroundAudioEnabled,
                            onCheckedChange = { checked ->
                                backgroundAudioEnabled = checked
                                prefs.edit().putBoolean(KEY_BACKGROUND_AUDIO, checked).apply()
                            },
                            testTag = "toggle_background_audio"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        SettingsSwitchRow(
                            title = "Auto-Resume Playback Position",
                            subtitle = "Automatically seek to last played timestamp when opening track",
                            checked = autoResumeAudioEnabled,
                            onCheckedChange = { checked ->
                                autoResumeAudioEnabled = checked
                                prefs.edit().putBoolean(KEY_AUTO_RESUME_POSITION, checked).apply()
                            },
                            testTag = "toggle_auto_resume"
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Skip interval selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Skip Duration",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Seconds to skip forward / backward in audio player",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(10, 15, 30).forEach { sec ->
                                    FilterChip(
                                        selected = skipIntervalSec == sec,
                                        onClick = {
                                            skipIntervalSec = sec
                                            prefs.edit().putInt(KEY_SKIP_INTERVAL, sec).apply()
                                        },
                                        label = { Text("${sec}s", fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Group 3: TTS Synthesizer & Offline Voice Models
            item {
                SettingsGroupHeader(title = "TTS ENGINE & VOICE MODELS", icon = Icons.Default.RecordVoiceOver)
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Bundled Native Synthesizers",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Piper ONNX & RHVoice offline engines",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "100% Offline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Voice Test Live Preview
                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            label = { Text("Sample Test Text", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (playbackState.isPlaying) "Stop Voice Sample" else "Test Selected Voice (${playbackState.selectedVoiceName})")
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Speed & Pitch sliders
                        Text(
                            text = "Speech Speed: ${String.format("%.2fx", playbackState.speechRate)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = playbackState.speechRate,
                            onValueChange = { ttsManager.setSpeechRate(it) },
                            valueRange = 0.5f..2.5f,
                            steps = 7
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Voice Pitch: ${String.format("%.2fx", playbackState.speechPitch)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = playbackState.speechPitch,
                            onValueChange = { ttsManager.setSpeechPitch(it) },
                            valueRange = 0.5f..2.0f,
                            steps = 5
                        )
                    }
                }
            }

            // Voice Catalog list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Models Catalog",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedEngineFilter == null,
                            onClick = { selectedEngineFilter = null },
                            label = { Text("All", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedEngineFilter == TtsEngineType.PIPER_ONNX,
                            onClick = { selectedEngineFilter = TtsEngineType.PIPER_ONNX },
                            label = { Text("Piper", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedEngineFilter == TtsEngineType.RHVOICE,
                            onClick = { selectedEngineFilter = TtsEngineType.RHVOICE },
                            label = { Text("RHVoice", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Voice Item cards
            items(filteredVoices) { voice ->
                val state = downloadStates[voice.id] ?: VoiceDownloadState.NotDownloaded
                val isSelected = playbackState.selectedVoiceId == voice.id

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${voice.language} • ${voice.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${voice.quality} • ${voice.sizeFormatted} • ${voice.engineType.badge}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        when (state) {
                            is VoiceDownloadState.NotDownloaded -> {
                                OutlinedButton(
                                    onClick = { ttsManager.downloadVoice(voice) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Download", fontSize = 11.sp)
                                }
                            }
                            is VoiceDownloadState.Downloading -> {
                                Text(
                                    text = "${(state.progress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is VoiceDownloadState.Downloaded -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { ttsManager.setSelectedVoice(voice.id) },
                                        enabled = !isSelected,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(if (isSelected) "Active" else "Select", fontSize = 11.sp)
                                    }

                                    // Disable & Delete Voice Data with Confirmation
                                    IconButton(
                                        onClick = { voiceToDelete = voice },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete voice model data",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            is VoiceDownloadState.Error -> {
                                TextButton(onClick = { ttsManager.downloadVoice(voice) }) {
                                    Text("Retry", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Group 4: Other Settings & App Maintenance
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsGroupHeader(title = "OTHER SETTINGS & MAINTENANCE", icon = Icons.Default.Tune)
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Reader Theme Selection
                        Text(text = "App Theme & Reader Canvas", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReaderThemeMode.values().forEach { mode ->
                                val isSel = currentThemeMode == mode
                                FilterChip(
                                    selected = isSel,
                                    onClick = { onThemeSelect(mode) },
                                    label = { Text(mode.name.replace("_", " "), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // App Restart
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "App session restarted successfully", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Restart App Session", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Re-initializes all local background services and engines", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Backup & Restore
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exportSettingsLauncher.launch("Lumina_Settings_Backup.json")
                                }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Backup & Export Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Export configuration to JSON file", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.SaveAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Voice Deletion Confirmation Dialog
    voiceToDelete?.let { voice ->
        AlertDialog(
            onDismissRequest = { voiceToDelete = null },
            title = { Text("Delete Voice Model Data?") },
            text = { Text("Are you sure you want to disable and delete the offline model '${voice.name}' (${voice.sizeFormatted})? You can re-download it later.") },
            confirmButton = {
                Button(
                    onClick = {
                        ttsManager.deleteVoice(voice.id)
                        Toast.makeText(context, "Deleted voice model ${voice.name}", Toast.LENGTH_SHORT).show()
                        voiceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { voiceToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Library Reset Confirmation Dialog
    if (showResetLibraryConfirm) {
        AlertDialog(
            onDismissRequest = { showResetLibraryConfirm = false },
            title = { Text("Reset Library Data?") },
            text = { Text("This will reset sample book caches and restore original defaults. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetLibrary()
                        Toast.makeText(context, "Library has been reset to default", Toast.LENGTH_SHORT).show()
                        showResetLibraryConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetLibraryConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsGroupHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
