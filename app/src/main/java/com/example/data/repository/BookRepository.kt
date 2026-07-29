package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.local.*
import com.example.data.parser.EpubParser
import com.example.data.parser.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class BookRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    private val readingPositionDao: ReadingPositionDao
) {
    companion object {
        private const val TAG = "BookRepository"
    }

    val allBooks: Flow<List<BookEntity>> = bookDao.getAllBooks()
    val favoriteBooks: Flow<List<BookEntity>> = bookDao.getFavoriteBooks()

    suspend fun initializePreloadedBooksIfEmpty() = withContext(Dispatchers.IO) {
        val existing = allBooks.first()
        val pkgName = context.packageName
        val artOfWarCover = "android.resource://$pkgName/${R.drawable.cover_art_of_war}"
        val prideCover = "android.resource://$pkgName/${R.drawable.cover_pride_and_prejudice}"
        val guideCover = "android.resource://$pkgName/${R.drawable.cover_lumina_guide}"

        if (existing.isEmpty()) {
            Log.d(TAG, "Initializing pre-loaded sample books into Lumina Library")

            // 1. The Art of War (EPUB Sample)
            val artOfWar = BookEntity(
                title = "The Art of War",
                author = "Sun Tzu",
                fileType = "EPUB",
                filePath = "sample_art_of_war.epub",
                isAsset = true,
                coverPath = artOfWarCover,
                coverColorHex = "#2E7D32", // Forest Green
                totalChapters = 13,
                totalPages = 13,
                description = "An ancient Chinese military treatise attributed to Sun Tzu, a high-ranking military general, strategist and tactician."
            )

            // 2. Pride and Prejudice (EPUB Sample)
            val prideAndPrejudice = BookEntity(
                title = "Pride and Prejudice",
                author = "Jane Austen",
                fileType = "EPUB",
                filePath = "sample_pride_and_prejudice.epub",
                isAsset = true,
                coverPath = prideCover,
                coverColorHex = "#C2185B", // Elegant Rose
                totalChapters = 10,
                totalPages = 10,
                description = "A romantic novel of manners written by Jane Austen in 1813, following the character development of Elizabeth Bennet."
            )

            // 3. Lumina Classic Reading Guide (PDF Sample)
            val pdfGuide = BookEntity(
                title = "Lumina Reader Guide & Classics",
                author = "Lumina Editorial",
                fileType = "PDF",
                filePath = "sample_lumina_guide.pdf",
                isAsset = true,
                coverPath = guideCover,
                coverColorHex = "#1976D2", // Deep Blue
                totalChapters = 1,
                totalPages = 5,
                description = "A comprehensive feature guide to Lumina Reader, highlighting natural voice narration, real-time sentence tracking, and custom themes."
            )

            bookDao.insertBook(artOfWar)
            bookDao.insertBook(prideAndPrejudice)
            bookDao.insertBook(pdfGuide)
        } else {
            // Update existing sample books if coverPath is missing
            for (book in existing) {
                if (book.coverPath.isNullOrBlank()) {
                    val updatedCover = when (book.title) {
                        "The Art of War" -> artOfWarCover
                        "Pride and Prejudice" -> prideCover
                        "Lumina Reader Guide & Classics" -> guideCover
                        else -> null
                    }
                    if (updatedCover != null) {
                        bookDao.insertBook(book.copy(coverPath = updatedCover))
                    }
                }
            }
            refreshBookCoversIfMissing()
        }
    }

    suspend fun importBookFromUri(uri: Uri, title: String? = null): Long = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val fileName = getFileNameFromUri(uri) ?: "Imported_Book"
        val extension = fileName.substringAfterLast(".").lowercase()

        val isPdf = extension == "pdf" || contentResolver.getType(uri)?.contains("pdf") == true
        val fileType = if (isPdf) "PDF" else "EPUB"

        val finalTitle = title ?: fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
        val coverColors = listOf("#3F51B5", "#8E24AA", "#00897B", "#D81B60", "#F57C00", "#43A047")
        val randomColor = coverColors.random()

        var pageCount = 1
        var chapterCount = 1
        var realTitle = title ?: fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
        var realAuthor = "Local Import"
        var description = "Imported $fileType document"
        var epubCoverBytes: ByteArray? = null

        if (fileType == "PDF") {
            pageCount = PdfHelper.getPageCount(context, uri, false, null).coerceAtLeast(1)
        } else {
            val parsed = EpubParser.parseEpubFromUri(context, uri)
            if (parsed != null) {
                chapterCount = parsed.chapters.size.coerceAtLeast(1)
                pageCount = chapterCount
                if (parsed.title.isNotBlank() && parsed.title != "Unknown Title") {
                    realTitle = parsed.title
                }
                if (parsed.author.isNotBlank() && parsed.author != "Unknown Author") {
                    realAuthor = parsed.author
                }
                if (!parsed.description.isNullOrBlank()) {
                    description = parsed.description
                }
                epubCoverBytes = parsed.coverBytes
            }
        }

        var coverFilePath: String? = null
        try {
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.png")
            if (fileType == "PDF") {
                val bitmap = PdfHelper.renderPageBitmap(context, uri, false, null, 0, 400)
                if (bitmap != null) {
                    FileOutputStream(coverFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    }
                    coverFilePath = coverFile.absolutePath
                }
            } else {
                val bytes = epubCoverBytes ?: EpubParser.extractCoverBytesFromUri(context, uri)
                if (bytes != null && bytes.isNotEmpty()) {
                    FileOutputStream(coverFile).use { out ->
                        out.write(bytes)
                    }
                    coverFilePath = coverFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract cover image during import", e)
        }

        val newBook = BookEntity(
            title = realTitle,
            author = realAuthor,
            fileType = fileType,
            filePath = uri.toString(),
            isAsset = false,
            coverPath = coverFilePath,
            coverColorHex = randomColor,
            totalChapters = chapterCount,
            totalPages = pageCount,
            description = description
        )

        bookDao.insertBook(newBook)
    }

    suspend fun refreshBookCoversIfMissing() = withContext(Dispatchers.IO) {
        val existing = allBooks.first()
        for (book in existing) {
            if (!book.isAsset && book.coverPath.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(book.filePath)
                    val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                    val coverFile = File(coversDir, "cover_${book.id}_${System.currentTimeMillis()}.png")

                    if (book.fileType == "EPUB") {
                        val bytes = EpubParser.extractCoverBytesFromUri(context, uri)
                        if (bytes != null && bytes.isNotEmpty()) {
                            FileOutputStream(coverFile).use { out -> out.write(bytes) }
                            bookDao.insertBook(book.copy(coverPath = coverFile.absolutePath))
                        }
                    } else if (book.fileType == "PDF") {
                        val bitmap = PdfHelper.renderPageBitmap(context, uri, false, null, 0, 400)
                        if (bitmap != null) {
                            FileOutputStream(coverFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, out) }
                            bookDao.insertBook(book.copy(coverPath = coverFile.absolutePath))
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed cover refresh for book ${book.id}", e)
                }
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = it.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    fun observeBookById(id: Long): Flow<BookEntity?> = bookDao.observeBookById(id)

    suspend fun updateReadingProgress(bookId: Long, chapterIndex: Int, pageIndex: Int, totalPages: Int) {
        val progress = if (totalPages > 0) ((pageIndex + 1).toFloat() / totalPages.toFloat()) * 100f else 0f
        bookDao.updateProgress(
            bookId = bookId,
            chapter = chapterIndex,
            page = pageIndex,
            progress = progress.coerceIn(0f, 100f)
        )
    }

    suspend fun getSavedPosition(documentKey: String, bookId: Long): ReadingPositionEntity? {
        return readingPositionDao.getPositionByKey(documentKey) ?: readingPositionDao.getPositionByBookId(bookId)
    }

    suspend fun saveReadingPosition(documentKey: String, bookId: Long, chapterIndex: Int, pageIndex: Int, totalItems: Int) {
        val progress = if (totalItems > 0) ((pageIndex + 1).toFloat() / totalItems.toFloat()) * 100f else 0f
        val pos = ReadingPositionEntity(
            documentKey = documentKey,
            bookId = bookId,
            chapterIndex = chapterIndex,
            pageIndex = pageIndex,
            progressPercentage = progress.coerceIn(0f, 100f),
            lastReadTimestamp = System.currentTimeMillis()
        )
        readingPositionDao.savePosition(pos)
        updateReadingProgress(bookId, chapterIndex, pageIndex, totalItems)
    }

    suspend fun toggleFavorite(bookId: Long, isFavorite: Boolean) {
        bookDao.toggleFavorite(bookId, isFavorite)
    }

    suspend fun deleteBook(book: BookEntity) {
        bookDao.deleteBook(book)
    }

    // Highlights
    fun getHighlightsForBook(bookId: Long): Flow<List<HighlightEntity>> = highlightDao.getHighlightsForBook(bookId)
    val allHighlights: Flow<List<HighlightEntity>> = highlightDao.getAllHighlights()

    suspend fun addHighlight(highlight: HighlightEntity): Long = highlightDao.insertHighlight(highlight)
    suspend fun deleteHighlight(highlight: HighlightEntity) = highlightDao.deleteHighlight(highlight)

    // Bookmarks
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForBook(bookId)
    suspend fun addBookmark(bookmark: BookmarkEntity): Long = bookmarkDao.insertBookmark(bookmark)
    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.deleteBookmark(bookmark)
}
