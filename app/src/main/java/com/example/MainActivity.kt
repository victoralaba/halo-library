package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.repository.BookRepository
import com.example.ui.components.FullAudioPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.SidebarDrawerContent
import com.example.ui.screens.AudioPlayerScreen
import com.example.ui.screens.HighlightsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MindDraftScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.ReadingStatsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import com.example.ui.viewmodel.ReadingStatsViewModel
import com.example.data.repository.ReadingStatsRepository
import com.example.data.audio.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    LIBRARY,
    AUDIO_PLAYER,
    READER,
    HIGHLIGHTS,
    SETTINGS,
    TTS_SETTINGS,
    MIND_DRAFT,
    READING_STATS
}

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val readerViewModel: ReaderViewModel by viewModels()
    private val audioPlayerViewModel: AudioPlayerViewModel by viewModels()
    private val readingStatsViewModel: ReadingStatsViewModel by viewModels()

    private val externalUriState = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val libraryUiState by libraryViewModel.uiState.collectAsState()
            val isDarkTheme = when (libraryUiState.themeMode) {
                ReaderThemeMode.DARK_OBSIDIAN, ReaderThemeMode.OLED_NIGHT -> true
                else -> false
            }

            var currentScreen by remember { mutableStateOf(Screen.LIBRARY) }
            var activeBookId by remember { mutableStateOf<Long?>(null) }
            var showSuggestionDialog by remember { mutableStateOf(false) }
            var suggestionInputText by remember { mutableStateOf("") }

            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            val externalUri by externalUriState.collectAsState()

            LaunchedEffect(externalUri) {
                val uri = externalUri
                if (uri != null) {
                    val type = contentResolver.getType(uri) ?: ""
                    if (type.contains("audio") || uri.path?.endsWith(".mp3") == true || uri.path?.endsWith(".m4a") == true) {
                        audioPlayerViewModel.importAudioFiles(listOf(uri))
                        currentScreen = Screen.AUDIO_PLAYER
                    } else {
                        libraryViewModel.importBookAndGetId(uri) { bookId ->
                            activeBookId = bookId
                            currentScreen = Screen.READER
                            externalUriState.value = null
                        }
                    }
                }
            }

            val db = remember { AppDatabase.getDatabase(applicationContext) }
            val repository = remember {
                BookRepository(
                    context = applicationContext,
                    bookDao = db.bookDao(),
                    highlightDao = db.highlightDao(),
                    bookmarkDao = db.bookmarkDao(),
                    readingPositionDao = db.readingPositionDao()
                )
            }
            val allHighlights by repository.allHighlights.collectAsState(initial = emptyList())

            LaunchedEffect(Unit) {
                val statsRepo = ReadingStatsRepository.getInstance(
                    context = applicationContext,
                    bookDao = db.bookDao(),
                    audioTrackDao = db.audioTrackDao()
                )
                AudioPlayerManager.getInstance(applicationContext).setStatsRepository(statsRepo)

                val prefs = applicationContext.getSharedPreferences("lumina_app_settings_prefs", android.content.Context.MODE_PRIVATE)
                val autoScan = prefs.getBoolean("auto_detect_dir_startup", true)
                if (autoScan) {
                    val folderPrefs = applicationContext.getSharedPreferences("lumina_scan_folders_prefs", android.content.Context.MODE_PRIVATE)
                    val folderUris = folderPrefs.getStringSet("scan_folders_set", emptySet())?.toList() ?: emptyList()
                    if (folderUris.isNotEmpty()) {
                        com.example.data.parser.StorageScanner.scanFolders(
                            context = applicationContext,
                            folderUris = folderUris,
                            bookRepository = repository,
                            audioRepository = audioPlayerViewModel.repository
                        )
                    }
                }
            }

            // Audio Player States
            val audioUiState by audioPlayerViewModel.uiState.collectAsState()
            val currentAudioTrack by audioPlayerViewModel.currentTrack.collectAsState()
            val isAudioPlaying by audioPlayerViewModel.isPlaying.collectAsState()
            val audioCurrentPositionMs by audioPlayerViewModel.currentPositionMs.collectAsState()
            val audioDurationMs by audioPlayerViewModel.durationMs.collectAsState()
            val audioPlaybackSpeed by audioPlayerViewModel.playbackSpeed.collectAsState()
            val audioSleepTimerRemainingSec by audioPlayerViewModel.sleepTimerRemainingSec.collectAsState()
            val audioPlaylist by audioPlayerViewModel.playlist.collectAsState()
            val audioRepeatMode by audioPlayerViewModel.repeatMode.collectAsState()
            val audioIsShuffle by audioPlayerViewModel.isShuffle.collectAsState()
            val audioBookmarks by audioPlayerViewModel.activeTrackBookmarks.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true, // Swipe right to open, swipe left/click outside to close
                    drawerContent = {
                        SidebarDrawerContent(
                            currentScreen = currentScreen,
                            onNavigate = { targetScreen ->
                                currentScreen = targetScreen
                                scope.launch { drawerState.close() }
                            },
                            onCloseDrawer = {
                                scope.launch { drawerState.close() }
                            },
                            themeMode = libraryUiState.themeMode,
                            onThemeSelect = { nextMode ->
                                libraryViewModel.setThemeMode(nextMode)
                            },
                            onOpenSuggestionDialog = {
                                scope.launch { drawerState.close() }
                                showSuggestionDialog = true
                            }
                        )
                    }
                ) {
                    Scaffold(
                        bottomBar = {
                            Column {
                                // Persistent Audio Mini-Player Bar
                                MiniPlayerBar(
                                    currentTrack = currentAudioTrack,
                                    isPlaying = isAudioPlaying,
                                    currentPositionMs = audioCurrentPositionMs,
                                    durationMs = audioDurationMs,
                                    onTogglePlayPause = { audioPlayerViewModel.togglePlayPause() },
                                    onSkipForward = { audioPlayerViewModel.skipForward(10) },
                                    onExpand = { audioPlayerViewModel.setExpandedPlayerVisible(true) }
                                )

                                // Main Bottom Navigation Bar when on Library, Audio, or Mind Draft tab
                                if (currentScreen == Screen.LIBRARY || currentScreen == Screen.AUDIO_PLAYER || currentScreen == Screen.MIND_DRAFT) {
                                    NavigationBar(
                                        modifier = Modifier.testTag("main_bottom_nav")
                                    ) {
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.LIBRARY,
                                            onClick = { currentScreen = Screen.LIBRARY },
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentScreen == Screen.LIBRARY) Icons.Default.MenuBook else Icons.Outlined.MenuBook,
                                                    contentDescription = "E-Books"
                                                )
                                            },
                                            label = { Text("E-Books") },
                                            modifier = Modifier.testTag("nav_tab_ebooks")
                                        )

                                        NavigationBarItem(
                                            selected = currentScreen == Screen.AUDIO_PLAYER,
                                            onClick = { currentScreen = Screen.AUDIO_PLAYER },
                                            icon = {
                                                Icon(
                                                    imageVector = if (currentScreen == Screen.AUDIO_PLAYER) Icons.Default.Headphones else Icons.Outlined.Headphones,
                                                    contentDescription = "Audio Player"
                                                )
                                            },
                                            label = { Text("Audio Player") },
                                            modifier = Modifier.testTag("nav_tab_audio")
                                        )
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            when (currentScreen) {
                                Screen.LIBRARY -> {
                                    LibraryScreen(
                                        viewModel = libraryViewModel,
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onBookClick = { bookId ->
                                            activeBookId = bookId
                                            currentScreen = Screen.READER
                                        },
                                        onNavigateToHighlights = {
                                            currentScreen = Screen.HIGHLIGHTS
                                        },
                                        onNavigateToTtsSettings = {
                                            currentScreen = Screen.SETTINGS
                                        }
                                    )
                                }

                                Screen.AUDIO_PLAYER -> {
                                    AudioPlayerScreen(
                                        viewModel = audioPlayerViewModel,
                                        onOpenDrawer = { scope.launch { drawerState.open() } }
                                    )
                                }

                                Screen.MIND_DRAFT -> {
                                    MindDraftScreen(
                                        onOpenDrawer = { scope.launch { drawerState.open() } },
                                        onNavigateBack = { currentScreen = Screen.LIBRARY }
                                    )
                                }

                                Screen.READER -> {
                                    activeBookId?.let { bookId ->
                                        ReaderScreen(
                                            viewModel = readerViewModel,
                                            bookId = bookId,
                                            onNavigateBack = {
                                                currentScreen = Screen.LIBRARY
                                            },
                                            onNavigateToHighlights = {
                                                currentScreen = Screen.HIGHLIGHTS
                                            },
                                            onNavigateToTtsSettings = {
                                                currentScreen = Screen.SETTINGS
                                            }
                                        )
                                    }
                                }

                                Screen.HIGHLIGHTS -> {
                                    HighlightsScreen(
                                        highlights = allHighlights,
                                        onDeleteHighlight = { highlight ->
                                            readerViewModel.deleteHighlight(highlight)
                                        },
                                        onNavigateBack = {
                                            currentScreen = if (activeBookId != null) Screen.READER else Screen.LIBRARY
                                        }
                                    )
                                }

                                Screen.READING_STATS -> {
                                    ReadingStatsScreen(
                                        viewModel = readingStatsViewModel,
                                        onNavigateBack = { currentScreen = Screen.LIBRARY }
                                    )
                                }

                                Screen.SETTINGS, Screen.TTS_SETTINGS -> {
                                    SettingsScreen(
                                        ttsManager = readerViewModel.ttsManager,
                                        currentThemeMode = libraryUiState.themeMode,
                                        onThemeSelect = { mode ->
                                            libraryViewModel.setThemeMode(mode)
                                        },
                                        onResetLibrary = {
                                            libraryViewModel.resetLibrary()
                                        },
                                        onNavigateBack = {
                                            currentScreen = if (activeBookId != null) Screen.READER else Screen.LIBRARY
                                        },
                                        bookRepository = repository,
                                        audioRepository = audioPlayerViewModel.repository,
                                        audioPlayerViewModel = audioPlayerViewModel
                                    )
                                }
                            }
                        }

                        // Expanded Full Screen Audio Player Sheet
                        if (audioUiState.isExpandedPlayerVisible) {
                            FullAudioPlayerSheet(
                                currentTrack = currentAudioTrack,
                                isPlaying = isAudioPlaying,
                                currentPositionMs = audioCurrentPositionMs,
                                durationMs = audioDurationMs,
                                playbackSpeed = audioPlaybackSpeed,
                                sleepTimerRemainingSec = audioSleepTimerRemainingSec,
                                playlist = audioPlaylist,
                                repeatMode = audioRepeatMode,
                                isShuffle = audioIsShuffle,
                                bookmarks = audioBookmarks,
                                onDismiss = { audioPlayerViewModel.setExpandedPlayerVisible(false) },
                                onTogglePlayPause = { audioPlayerViewModel.togglePlayPause() },
                                onSeekTo = { audioPlayerViewModel.seekTo(it) },
                                onSkipForward = { audioPlayerViewModel.skipForward(it) },
                                onSkipBackward = { audioPlayerViewModel.skipBackward(it) },
                                onPlayNext = { audioPlayerViewModel.playNext() },
                                onPlayPrevious = { audioPlayerViewModel.playPrevious() },
                                onSetPlaybackSpeed = { audioPlayerViewModel.setPlaybackSpeed(it) },
                                onToggleRepeatMode = { audioPlayerViewModel.toggleRepeatMode() },
                                onToggleShuffle = { audioPlayerViewModel.toggleShuffle() },
                                onSetSleepTimerMinutes = { audioPlayerViewModel.setSleepTimerMinutes(it) },
                                onCancelSleepTimer = { audioPlayerViewModel.cancelSleepTimer() },
                                onAddBookmark = { note -> audioPlayerViewModel.addBookmark(note) },
                                onDeleteBookmark = { bm -> audioPlayerViewModel.deleteBookmark(bm) },
                                onPlayTrackFromList = { track -> audioPlayerViewModel.playTrack(track, audioPlaylist) }
                            )
                        }

                        // Feature Suggestion / Feedback Dialog
                        if (showSuggestionDialog) {
                            AlertDialog(
                                onDismissRequest = { showSuggestionDialog = false },
                                title = { Text("Submit Feature Suggestion", style = MaterialTheme.typography.titleLarge) },
                                text = {
                                    Column {
                                        Text(
                                            text = "What feature, layout tweak, or integration would you love to see next in Lumina?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedTextField(
                                            value = suggestionInputText,
                                            onValueChange = { suggestionInputText = it },
                                            placeholder = { Text("e.g. Reading Goals Widget, Audio Equalizer Preset, Cloud Sync...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 3
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (suggestionInputText.isNotBlank()) {
                                                Toast.makeText(context, "Thank you! Your suggestion has been recorded.", Toast.LENGTH_LONG).show()
                                                suggestionInputText = ""
                                                showSuggestionDialog = false
                                            }
                                        }
                                    ) {
                                        Text("Submit")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showSuggestionDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data
        if ((action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) && data != null) {
            externalUriState.value = data
        }
    }
}
