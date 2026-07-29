package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BookMetadataDialog
import com.example.ui.components.ThemeSelectorSheet
import com.example.ui.components.TtsControlBar
import com.example.ui.theme.ReaderThemeConfig
import com.example.ui.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    bookId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToHighlights: () -> Unit,
    onNavigateToTtsSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showMetadataDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var selectedSnippetForNote by remember { mutableStateOf("") }
    var customNoteInput by remember { mutableStateOf("") }
    var selectedHighlightColor by remember { mutableStateOf("#FFEB3B") }
    var drawerTab by remember { mutableStateOf(0) } // 0: Chapters, 1: Bookmarks

    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId)
    }

    val currentThemeColors = ReaderThemeConfig.getColors(uiState.themeMode)

    val fontStyle = when (uiState.fontFamily) {
        "Serif" -> FontFamily.Serif
        "SansSerif" -> FontFamily.SansSerif
        else -> FontFamily.Monospace
    }

    // Auto-scroll to active sentence being spoken by TTS
    LaunchedEffect(uiState.activeSentenceIndex) {
        if (uiState.activeSentenceIndex >= 0) {
            scope.launch {
                listState.animateScrollToItem(uiState.activeSentenceIndex)
            }
        }
    }

    // Extract sentences for active chapter
    val currentChapter = uiState.epubData?.chapters?.getOrNull(uiState.currentChapterIndex)
    val allSentencesInChapter = remember(currentChapter) {
        currentChapter?.paragraphs?.flatMap { para ->
            para.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        } ?: emptyList()
    }

    val isCurrentLocationBookmarked = remember(uiState.bookmarks, uiState.currentChapterIndex, uiState.currentPageIndex) {
        uiState.bookmarks.any { it.chapterIndex == uiState.currentChapterIndex && it.pageIndex == uiState.currentPageIndex }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = currentThemeColors.surfaceColor,
                drawerContentColor = currentThemeColors.textColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .padding(16.dp)
                ) {
                    TabRow(
                        selectedTabIndex = drawerTab,
                        containerColor = Color.Transparent,
                        contentColor = currentThemeColors.accentColor
                    ) {
                        Tab(
                            selected = drawerTab == 0,
                            onClick = { drawerTab = 0 },
                            text = { Text("Chapters", color = currentThemeColors.textColor) },
                            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Chapters", tint = currentThemeColors.textColor) }
                        )
                        Tab(
                            selected = drawerTab == 1,
                            onClick = { drawerTab = 1 },
                            text = { Text("Bookmarks (${uiState.bookmarks.size})", color = currentThemeColors.textColor) },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks", tint = currentThemeColors.textColor) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = currentThemeColors.textColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (drawerTab == 0) {
                        val chapters = uiState.epubData?.chapters ?: emptyList()
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(chapters) { idx, chap ->
                                val isSelected = idx == uiState.currentChapterIndex
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectChapter(idx)
                                            scope.launch { drawerState.close() }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) currentThemeColors.accentColor.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = chap.title,
                                        modifier = Modifier.padding(12.dp),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) currentThemeColors.accentColor else currentThemeColors.textColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        val bookmarks = uiState.bookmarks
                        if (bookmarks.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No bookmarks saved yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = currentThemeColors.textColor.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(bookmarks) { _, bm ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.jumpToBookmark(bm)
                                                scope.launch { drawerState.close() }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = currentThemeColors.backgroundColor.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = bm.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = currentThemeColors.textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (bm.previewText.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = bm.previewText,
                                                        fontSize = 12.sp,
                                                        color = currentThemeColors.textColor.copy(alpha = 0.7f),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteBookmark(bm) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove bookmark",
                                                    tint = currentThemeColors.textColor.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.book?.title ?: "Reading...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentThemeColors.textColor
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.stopTts()
                                onNavigateBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = currentThemeColors.textColor
                            )
                        }
                    },
                    actions = {
                        // Book Details & Metadata Dialog Trigger
                        IconButton(onClick = { showMetadataDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Book Details & Metadata",
                                tint = currentThemeColors.textColor
                            )
                        }

                        // Table of Contents Drawer
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Table of Contents & Bookmarks",
                                tint = currentThemeColors.textColor
                            )
                        }

                        // Bookmark Toggle Button
                        IconButton(
                            onClick = {
                                val currentTitle = currentChapter?.title ?: "Page ${uiState.currentPageIndex + 1}"
                                viewModel.toggleBookmarkForCurrentLocation(
                                    defaultTitle = currentTitle,
                                    previewText = allSentencesInChapter.firstOrNull() ?: ""
                                )
                            }
                        ) {
                            Icon(
                                imageVector = if (isCurrentLocationBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isCurrentLocationBookmarked) "Remove Bookmark" else "Add Bookmark",
                                tint = if (isCurrentLocationBookmarked) currentThemeColors.accentColor else currentThemeColors.textColor
                            )
                        }

                        // Saved Highlights Shortcut
                        IconButton(onClick = onNavigateToHighlights) {
                            Icon(
                                imageVector = Icons.Outlined.FormatQuote,
                                contentDescription = "View Highlights",
                                tint = currentThemeColors.textColor
                            )
                        }

                        // TTS Engine Settings Shortcut
                        IconButton(onClick = onNavigateToTtsSettings) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "TTS Voice Settings",
                                tint = currentThemeColors.textColor
                            )
                        }

                        // Appearance / Typography Settings
                        IconButton(onClick = { showThemeSheet = true }) {
                            Icon(
                                imageVector = Icons.Outlined.FormatSize,
                                contentDescription = "Appearance",
                                tint = currentThemeColors.textColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = currentThemeColors.backgroundColor
                    )
                )
            },
            bottomBar = {
                TtsControlBar(
                    isPlaying = uiState.isTtsPlaying,
                    isBuffering = uiState.isTtsBuffering,
                    activeTextSnippet = if (uiState.activeSentenceIndex in allSentencesInChapter.indices)
                        allSentencesInChapter[uiState.activeSentenceIndex] else "",
                    speechRate = uiState.speechRate,
                    statusMessage = uiState.ttsStatus,
                    onPlayPauseToggle = {
                        viewModel.toggleTtsPlayback(allSentencesInChapter, uiState.activeSentenceIndex.coerceAtLeast(0))
                    },
                    onSkipPrevious = viewModel::skipTtsPrevious,
                    onSkipNext = viewModel::skipTtsNext,
                    onStop = viewModel::stopTts,
                    onRateChange = viewModel::setSpeechRate,
                    onOpenTtsSettings = onNavigateToTtsSettings
                )
            },
            containerColor = currentThemeColors.backgroundColor
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(currentThemeColors.backgroundColor)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = currentThemeColors.accentColor
                    )
                } else if (uiState.book?.fileType.equals("PDF", ignoreCase = true) && uiState.pdfCurrentBitmap != null) {
                    // PDF Reader Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(currentThemeColors.surfaceColor)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = uiState.pdfCurrentBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page ${uiState.currentPageIndex + 1}",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PDF Page Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.selectPdfPage(uiState.currentPageIndex - 1) },
                                enabled = uiState.currentPageIndex > 0
                            ) {
                                Icon(
                                    Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Page",
                                    tint = currentThemeColors.textColor
                                )
                            }

                            Text(
                                text = "Page ${uiState.currentPageIndex + 1} of ${uiState.pdfPageCount}",
                                color = currentThemeColors.textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            IconButton(
                                onClick = { viewModel.selectPdfPage(uiState.currentPageIndex + 1) },
                                enabled = uiState.currentPageIndex < uiState.pdfPageCount - 1
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Next Page",
                                    tint = currentThemeColors.textColor
                                )
                            }
                        }
                    }
                } else {
                    // EPUB / Text Reading Mode with Real-Time Sentence Tracking
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        // Chapter Title Header
                        Text(
                            text = currentChapter?.title ?: "Chapter 1",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontStyle,
                            color = currentThemeColors.textColor,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Divider(color = currentThemeColors.textColor.copy(alpha = 0.15f))

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sentences List with Real-Time Glowing Highlight Pill & Tap-to-Speak
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 120.dp)
                        ) {
                            itemsIndexed(allSentencesInChapter) { index, sentenceText ->
                                val isSentenceActive = index == uiState.activeSentenceIndex

                                val animatedBgColor by animateColorAsState(
                                    targetValue = if (isSentenceActive)
                                        currentThemeColors.highlightGlowColor else Color.Transparent,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "sentenceHighlightGlow"
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            viewModel.playFromSentence(allSentencesInChapter, index)
                                        }
                                        .testTag("sentence_item_$index"),
                                    color = animatedBgColor,
                                    tonalElevation = if (isSentenceActive) 4.dp else 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sentenceText,
                                            fontSize = uiState.fontSizeSp.sp,
                                            lineHeight = (uiState.fontSizeSp * uiState.lineHeightMultiplier).sp,
                                            fontFamily = fontStyle,
                                            color = if (isSentenceActive)
                                                currentThemeColors.textHighlightColor else currentThemeColors.textColor,
                                            fontWeight = if (isSentenceActive) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Quick Highlight & Note Action Button
                                        IconButton(
                                            onClick = {
                                                selectedSnippetForNote = sentenceText
                                                showAddNoteDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.FormatQuote,
                                                contentDescription = "Save Highlight",
                                                tint = currentThemeColors.textColor.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Appearance Bottom Sheet
    if (showThemeSheet) {
        ThemeSelectorSheet(
            currentThemeMode = uiState.themeMode,
            fontSizeSp = uiState.fontSizeSp,
            lineHeightMultiplier = uiState.lineHeightMultiplier,
            fontFamily = uiState.fontFamily,
            onThemeSelected = viewModel::setThemeMode,
            onFontSizeChange = viewModel::setFontSize,
            onLineHeightChange = viewModel::setLineHeight,
            onFontFamilyChange = viewModel::setFontFamily,
            onDismissRequest = { showThemeSheet = false }
        )
    }

    // Save Highlight / Add Note Dialog
    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Save Quote & Note") },
            text = {
                Column {
                    Text(
                        text = "\"$selectedSnippetForNote\"",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Highlight Color:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("#FFEB3B", "#A7F3D0", "#FBCFE8", "#BAE6FD").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (selectedHighlightColor == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    )
                                    .clickable { selectedHighlightColor = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customNoteInput,
                        onValueChange = { customNoteInput = it },
                        label = { Text("Add personal note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addHighlight(
                            snippet = selectedSnippetForNote,
                            colorHex = selectedHighlightColor,
                            note = customNoteInput
                        )
                        showAddNoteDialog = false
                        customNoteInput = ""
                    }
                ) {
                    Text("Save Quote")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMetadataDialog && uiState.book != null) {
        BookMetadataDialog(
            book = uiState.book!!,
            onDismiss = { showMetadataDialog = false },
            onReadBook = { showMetadataDialog = false },
            onToggleFavorite = { viewModel.toggleFavorite() }
        )
    }
}
