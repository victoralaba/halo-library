package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.local.AppDatabase
import com.example.data.repository.BookRepository
import com.example.ui.screens.HighlightsScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.TtsSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.flow.MutableStateFlow

enum class Screen {
    LIBRARY,
    READER,
    HIGHLIGHTS,
    TTS_SETTINGS
}

class MainActivity : ComponentActivity() {

    private val libraryViewModel: LibraryViewModel by viewModels()
    private val readerViewModel: ReaderViewModel by viewModels()

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

            val externalUri by externalUriState.collectAsState()

            LaunchedEffect(externalUri) {
                val uri = externalUri
                if (uri != null) {
                    libraryViewModel.importBookAndGetId(uri) { bookId ->
                        activeBookId = bookId
                        currentScreen = Screen.READER
                        externalUriState.value = null
                    }
                }
            }

            val db = remember { AppDatabase.getDatabase(applicationContext) }
            val repository = remember {
                BookRepository(
                    context = applicationContext,
                    bookDao = db.bookDao(),
                    highlightDao = db.highlightDao(),
                    bookmarkDao = db.bookmarkDao()
                )
            }
            val allHighlights by repository.allHighlights.collectAsState(initial = emptyList())

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        Screen.LIBRARY -> {
                            LibraryScreen(
                                viewModel = libraryViewModel,
                                onBookClick = { bookId ->
                                    activeBookId = bookId
                                    currentScreen = Screen.READER
                                },
                                onNavigateToHighlights = {
                                    currentScreen = Screen.HIGHLIGHTS
                                },
                                onNavigateToTtsSettings = {
                                    currentScreen = Screen.TTS_SETTINGS
                                }
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
                                        currentScreen = Screen.TTS_SETTINGS
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

                        Screen.TTS_SETTINGS -> {
                            TtsSettingsScreen(
                                ttsManager = readerViewModel.ttsManager,
                                onNavigateBack = {
                                    currentScreen = if (activeBookId != null) Screen.READER else Screen.LIBRARY
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
