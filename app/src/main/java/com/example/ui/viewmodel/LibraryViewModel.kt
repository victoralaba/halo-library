package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BookEntity
import com.example.data.repository.BookRepository
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: BookFilter = BookFilter.ALL,
    val isLoading: Boolean = true,
    val themeMode: ReaderThemeMode = ReaderThemeMode.DARK_OBSIDIAN,
    val statusMessage: String? = null
)

enum class BookFilter {
    ALL,
    EPUB,
    PDF,
    FAVORITES
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BookRepository(
        context = application,
        bookDao = db.bookDao(),
        highlightDao = db.highlightDao(),
        bookmarkDao = db.bookmarkDao()
    )

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(BookFilter.ALL)
    private val _themeMode = MutableStateFlow(ReaderThemeMode.DARK_OBSIDIAN)
    private val _isLoading = MutableStateFlow(true)
    private val _statusMessage = MutableStateFlow<String?>(null)

    private data class FilterState(
        val query: String = "",
        val filter: BookFilter = BookFilter.ALL,
        val theme: ReaderThemeMode = ReaderThemeMode.DARK_OBSIDIAN,
        val loading: Boolean = true,
        val message: String? = null
    )

    private val filterState = combine(
        _searchQuery,
        _selectedFilter,
        _themeMode,
        _isLoading,
        _statusMessage
    ) { query, filter, theme, loading, message ->
        FilterState(query, filter, theme, loading, message)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.allBooks,
        filterState
    ) { books, fState ->
        val filteredBooks = books.filter { book ->
            val matchesSearch = fState.query.isBlank() ||
                    book.title.contains(fState.query, ignoreCase = true) ||
                    book.author.contains(fState.query, ignoreCase = true)

            val matchesFilter = when (fState.filter) {
                BookFilter.ALL -> true
                BookFilter.EPUB -> book.fileType.equals("EPUB", ignoreCase = true)
                BookFilter.PDF -> book.fileType.equals("PDF", ignoreCase = true)
                BookFilter.FAVORITES -> book.isFavorite
            }

            matchesSearch && matchesFilter
        }

        LibraryUiState(
            books = filteredBooks,
            searchQuery = fState.query,
            selectedFilter = fState.filter,
            isLoading = fState.loading,
            themeMode = fState.theme,
            statusMessage = fState.message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    init {
        viewModelScope.launch {
            _isLoading.value = true
            repository.initializePreloadedBooksIfEmpty()
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: BookFilter) {
        _selectedFilter.value = filter
    }

    fun setThemeMode(mode: ReaderThemeMode) {
        _themeMode.value = mode
    }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Importing book..."
                repository.importBookFromUri(uri)
                _statusMessage.value = "Book imported successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to import: ${e.localizedMessage}"
            }
        }
    }

    fun importBookAndGetId(uri: Uri, onImported: (Long) -> Unit) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Opening external book..."
                val bookId = repository.importBookFromUri(uri)
                _statusMessage.value = "Book opened!"
                onImported(bookId)
            } catch (e: Exception) {
                _statusMessage.value = "Failed to open book: ${e.localizedMessage}"
            }
        }
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, !book.isFavorite)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
            _statusMessage.value = "Removed ${book.title}"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
