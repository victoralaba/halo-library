package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.parser.*
import com.example.data.repository.BookRepository
import com.example.data.repository.ReadingStatsRepository
import com.example.data.tts.TtsManager
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

data class ReaderUiState(
    val book: BookEntity? = null,
    val epubData: ParsedEpub? = null,
    val currentChapterIndex: Int = 0,
    val currentPageIndex: Int = 0,
    val pdfPageCount: Int = 0,
    val pdfCurrentBitmap: Bitmap? = null,
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.5f,
    val fontFamily: String = "Serif", // "Serif", "SansSerif", "Monospace"
    val themeMode: ReaderThemeMode = ReaderThemeMode.DARK_OBSIDIAN,
    val isTtsViewMode: Boolean = false, // false = Normal Doc View, true = TTS Audio View
    val isImmersiveMode: Boolean = false, // Fullscreen reading mode (hides bars when reading)
    val isTtsPlaying: Boolean = false,
    val isTtsBuffering: Boolean = false,
    val activeSentenceIndex: Int = -1,
    val selectedVoice: String = "Default Natural Voice",
    val speechRate: Float = 1.0f,
    val ttsStatus: String = "",
    val highlights: List<HighlightEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BookRepository(
        context = application,
        bookDao = db.bookDao(),
        highlightDao = db.highlightDao(),
        bookmarkDao = db.bookmarkDao(),
        readingPositionDao = db.readingPositionDao()
    )

    val ttsManager = TtsManager(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val statsRepository = ReadingStatsRepository.getInstance(
        context = application,
        bookDao = db.bookDao(),
        audioTrackDao = db.audioTrackDao()
    )

    private var readingTimerJob: kotlinx.coroutines.Job? = null

    private fun startReadingTimeTracker() {
        readingTimerJob?.cancel()
        readingTimerJob = viewModelScope.launch {
            while (coroutineContext.isActive) {
                kotlinx.coroutines.delay(5000L) // Record +5s of active reading time every 5 seconds
                statsRepository.addEbookReadingTime(5L)
            }
        }
    }

    private var activeBookId: Long = -1

    init {
        viewModelScope.launch {
            ttsManager.playbackState.collect { playback ->
                _uiState.update { state ->
                    state.copy(
                        isTtsPlaying = playback.isPlaying,
                        isTtsBuffering = playback.isBuffering,
                        activeSentenceIndex = playback.currentSentenceIndex,
                        selectedVoice = playback.selectedVoice,
                        speechRate = playback.speechRate,
                        ttsStatus = playback.statusMessage
                    )
                }
            }
        }
    }

    fun loadBook(bookId: Long) {
        activeBookId = bookId
        startReadingTimeTracker()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val book = repository.getBookById(bookId)
            if (book == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Book not found") }
                return@launch
            }

            // Fetch last saved position from Room database
            val savedPosition = repository.getSavedPosition(book.filePath, book.id)
            val initialChapter = savedPosition?.chapterIndex ?: book.lastReadChapter
            val initialPage = savedPosition?.pageIndex ?: book.lastReadPage

            _uiState.update {
                it.copy(
                    book = book,
                    currentChapterIndex = initialChapter,
                    currentPageIndex = initialPage
                )
            }

            // Load highlights and bookmarks flows
            launch {
                repository.getHighlightsForBook(bookId).collect { hList ->
                    _uiState.update { it.copy(highlights = hList) }
                }
            }

            launch {
                repository.getBookmarksForBook(bookId).collect { bList ->
                    _uiState.update { it.copy(bookmarks = bList) }
                }
            }

            if (book.fileType.equals("PDF", ignoreCase = true)) {
                loadPdfContent(book)
            } else {
                loadEpubContent(book)
            }
        }
    }

    private fun loadEpubContent(book: BookEntity) {
        val parsed = if (book.isAsset) {
            when (book.filePath) {
                "sample_art_of_war.epub" -> SampleBooksProvider.getArtOfWar()
                "sample_pride_and_prejudice.epub" -> SampleBooksProvider.getPrideAndPrejudice()
                "sample_lumina_guide.pdf" -> SampleBooksProvider.getLuminaGuideText()
                else -> EpubParser.parseEpubFromAsset(getApplication(), book.filePath)
            }
        } else {
            EpubParser.parseEpubFromUri(getApplication(), Uri.parse(book.filePath))
        }

        if (parsed != null && parsed.chapters.isNotEmpty()) {
            val safeChapter = book.lastReadChapter.coerceIn(0, parsed.chapters.size - 1)
            _uiState.update {
                it.copy(
                    epubData = parsed,
                    currentChapterIndex = safeChapter,
                    isLoading = false
                )
            }
        } else {
            // Fallback to sample text if parser fails
            val sampleFallback = SampleBooksProvider.getLuminaGuideText()
            _uiState.update {
                it.copy(
                    epubData = sampleFallback,
                    currentChapterIndex = 0,
                    isLoading = false
                )
            }
        }
    }

    private fun loadPdfContent(book: BookEntity) {
        val uri = Uri.parse(book.filePath)
        val pageCount = PdfHelper.getPageCount(
            context = getApplication(),
            fileUri = uri,
            isAsset = book.isAsset,
            assetName = if (book.isAsset) book.filePath else null
        ).let { if (it <= 0) 5 else it }

        val safePage = book.lastReadPage.coerceIn(0, pageCount - 1)
        val bitmap = PdfHelper.renderPageBitmap(
            context = getApplication(),
            fileUri = uri,
            isAsset = book.isAsset,
            assetName = if (book.isAsset) book.filePath else null,
            pageIndex = safePage
        )

        // Also prepare fallback EPUB structure for text/TTS narration if PDF
        val pdfGuideData = SampleBooksProvider.getLuminaGuideText()

        _uiState.update {
            it.copy(
                pdfPageCount = pageCount,
                currentPageIndex = safePage,
                pdfCurrentBitmap = bitmap,
                epubData = pdfGuideData,
                isLoading = false
            )
        }
    }

    fun selectChapter(chapterIndex: Int) {
        val state = _uiState.value
        val epub = state.epubData ?: return
        if (chapterIndex in epub.chapters.indices) {
            ttsManager.stop()
            _uiState.update { it.copy(currentChapterIndex = chapterIndex, activeSentenceIndex = -1) }
            saveReadingProgress(chapterIndex, 0, epub.chapters.size)
        }
    }

    fun selectPdfPage(pageIndex: Int) {
        val state = _uiState.value
        val book = state.book ?: return
        val safePage = pageIndex.coerceIn(0, state.pdfPageCount - 1)

        ttsManager.stop()

        val bitmap = PdfHelper.renderPageBitmap(
            context = getApplication(),
            fileUri = Uri.parse(book.filePath),
            isAsset = book.isAsset,
            assetName = if (book.isAsset) book.filePath else null,
            pageIndex = safePage
        )

        _uiState.update { it.copy(currentPageIndex = safePage, pdfCurrentBitmap = bitmap, activeSentenceIndex = -1) }
        saveReadingProgress(0, safePage, state.pdfPageCount)
    }

    private fun saveReadingProgress(chapterIndex: Int, pageIndex: Int, total: Int) {
        val book = _uiState.value.book ?: return
        viewModelScope.launch {
            repository.saveReadingPosition(book.filePath, book.id, chapterIndex, pageIndex, total)
        }
    }

    fun toggleTtsPlayback(sentences: List<String>, startSentenceIndex: Int = 0) {
        val state = _uiState.value
        if (state.isTtsPlaying) {
            ttsManager.pause()
        } else {
            if (sentences.isEmpty()) return
            ttsManager.playSentences(
                sentences = sentences,
                startIndex = startSentenceIndex,
                onHighlight = { index, _ ->
                    _uiState.update { it.copy(activeSentenceIndex = index) }
                },
                onFinished = {
                    _uiState.update { it.copy(activeSentenceIndex = -1) }
                }
            )
        }
    }

    fun playFromSentence(sentences: List<String>, index: Int) {
        ttsManager.playSentences(
            sentences = sentences,
            startIndex = index,
            onHighlight = { sentIdx, _ ->
                _uiState.update { it.copy(activeSentenceIndex = sentIdx) }
            },
            onFinished = {
                _uiState.update { it.copy(activeSentenceIndex = -1) }
            }
        )
    }

    fun pauseTts() = ttsManager.pause()
    fun resumeTts() = ttsManager.resume()
    fun stopTts() = ttsManager.stop()
    fun skipTtsNext() = ttsManager.skipNext()
    fun skipTtsPrevious() = ttsManager.skipPrevious()

    fun setSpeechRate(rate: Float) = ttsManager.setSpeechRate(rate)
    fun setSelectedVoice(voiceName: String) = ttsManager.setSelectedVoice(voiceName)

    fun setFontSize(sp: Int) {
        _uiState.update { it.copy(fontSizeSp = sp.coerceIn(12, 36)) }
    }

    fun setLineHeight(multiplier: Float) {
        _uiState.update { it.copy(lineHeightMultiplier = multiplier) }
    }

    fun setFontFamily(fontFamily: String) {
        _uiState.update { it.copy(fontFamily = fontFamily) }
    }

    fun setThemeMode(mode: ReaderThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setTtsViewMode(enabled: Boolean) {
        _uiState.update { it.copy(isTtsViewMode = enabled, isImmersiveMode = false) }
    }

    fun toggleTtsViewMode() {
        _uiState.update { it.copy(isTtsViewMode = !it.isTtsViewMode, isImmersiveMode = false) }
    }

    fun toggleImmersiveMode() {
        _uiState.update { it.copy(isImmersiveMode = !it.isImmersiveMode) }
    }

    fun setImmersiveMode(enabled: Boolean) {
        _uiState.update { it.copy(isImmersiveMode = enabled) }
    }

    fun jumpToBookmark(bookmark: BookmarkEntity) {
        val state = _uiState.value
        val book = state.book ?: return
        if (book.fileType.equals("PDF", ignoreCase = true)) {
            selectPdfPage(bookmark.pageIndex)
        } else {
            selectChapter(bookmark.chapterIndex)
        }
    }

    fun toggleBookmarkForCurrentLocation(defaultTitle: String, previewText: String = "") {
        val state = _uiState.value
        val book = state.book ?: return
        val existing = state.bookmarks.firstOrNull {
            it.chapterIndex == state.currentChapterIndex && it.pageIndex == state.currentPageIndex
        }

        viewModelScope.launch {
            if (existing != null) {
                repository.deleteBookmark(existing)
            } else {
                val bookmark = BookmarkEntity(
                    bookId = book.id,
                    chapterIndex = state.currentChapterIndex,
                    pageIndex = state.currentPageIndex,
                    title = defaultTitle,
                    previewText = previewText
                )
                repository.addBookmark(bookmark)
            }
        }
    }

    fun addHighlight(snippet: String, colorHex: String, note: String = "") {
        val state = _uiState.value
        val book = state.book ?: return
        viewModelScope.launch {
            val highlight = HighlightEntity(
                bookId = book.id,
                chapterIndex = state.currentChapterIndex,
                pageIndex = state.currentPageIndex,
                textSnippet = snippet,
                note = note,
                colorHex = colorHex
            )
            repository.addHighlight(highlight)
        }
    }

    fun addBookmark(title: String, previewText: String = "") {
        val state = _uiState.value
        val book = state.book ?: return
        viewModelScope.launch {
            val bookmark = BookmarkEntity(
                bookId = book.id,
                chapterIndex = state.currentChapterIndex,
                pageIndex = state.currentPageIndex,
                title = title,
                previewText = previewText
            )
            repository.addBookmark(bookmark)
        }
    }

    fun deleteHighlight(highlight: HighlightEntity) {
        viewModelScope.launch { repository.deleteHighlight(highlight) }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { repository.deleteBookmark(bookmark) }
    }

    fun toggleFavorite() {
        val currentBook = _uiState.value.book ?: return
        viewModelScope.launch {
            val newFav = !currentBook.isFavorite
            repository.toggleFavorite(currentBook.id, newFav)
            _uiState.update { it.copy(book = currentBook.copy(isFavorite = newFav)) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.release()
    }
}
