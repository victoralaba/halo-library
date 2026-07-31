package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreManager {

    private const val PREFS_SETTINGS = "lumina_app_settings_prefs"
    private const val PREFS_FOLDERS = "lumina_scan_folders_prefs"
    private const val KEY_FOLDERS_SET = "scan_folders_set"

    suspend fun exportFullBackupJson(context: Context, db: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // 1. SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        val prefsJson = JSONObject().apply {
            put("wake_lock_reading", prefs.getBoolean("wake_lock_reading", true))
            put("auto_detect_dir_startup", prefs.getBoolean("auto_detect_dir_startup", true))
            put("background_audio_playback", prefs.getBoolean("background_audio_playback", true))
            put("auto_resume_audio_position", prefs.getBoolean("auto_resume_audio_position", true))
            put("skip_interval_seconds", prefs.getInt("skip_interval_seconds", 10))
            if (prefs.contains("reader_theme_mode")) {
                put("reader_theme_mode", prefs.getString("reader_theme_mode", null))
            }
        }
        root.put("preferences", prefsJson)

        // Scan Folders
        val folderPrefs = context.getSharedPreferences(PREFS_FOLDERS, Context.MODE_PRIVATE)
        val foldersSet = folderPrefs.getStringSet(KEY_FOLDERS_SET, emptySet()) ?: emptySet()
        val foldersArray = JSONArray()
        foldersSet.forEach { foldersArray.put(it) }
        root.put("scan_folders", foldersArray)

        // 2. Books
        val books = db.bookDao().getAllBooks().first()
        val booksArray = JSONArray()
        books.forEach { book ->
            booksArray.put(JSONObject().apply {
                put("id", book.id)
                put("title", book.title)
                put("author", book.author)
                put("fileType", book.fileType)
                put("filePath", book.filePath)
                put("isAsset", book.isAsset)
                put("coverPath", book.coverPath)
                put("coverColorHex", book.coverColorHex)
                put("lastReadChapter", book.lastReadChapter)
                put("lastReadPage", book.lastReadPage)
                put("totalChapters", book.totalChapters)
                put("totalPages", book.totalPages)
                put("progressPercentage", book.progressPercentage.toDouble())
                put("lastReadTimestamp", book.lastReadTimestamp)
                put("addedTimestamp", book.addedTimestamp)
                put("isFavorite", book.isFavorite)
                put("description", book.description)
            })
        }
        root.put("books", booksArray)

        // 3. Reading Positions
        val positions = db.readingPositionDao().getAllPositions().first()
        val positionsArray = JSONArray()
        positions.forEach { pos ->
            positionsArray.put(JSONObject().apply {
                put("documentKey", pos.documentKey)
                put("bookId", pos.bookId)
                put("chapterIndex", pos.chapterIndex)
                put("pageIndex", pos.pageIndex)
                put("progressPercentage", pos.progressPercentage.toDouble())
                put("lastReadTimestamp", pos.lastReadTimestamp)
            })
        }
        root.put("readingPositions", positionsArray)

        // 4. Bookmarks
        val bookmarks = db.bookmarkDao().getAllBookmarks().first()
        val bookmarksArray = JSONArray()
        bookmarks.forEach { bm ->
            bookmarksArray.put(JSONObject().apply {
                put("id", bm.id)
                put("bookId", bm.bookId)
                put("chapterIndex", bm.chapterIndex)
                put("pageIndex", bm.pageIndex)
                put("title", bm.title)
                put("previewText", bm.previewText)
                put("timestamp", bm.timestamp)
            })
        }
        root.put("bookmarks", bookmarksArray)

        // 5. Highlights
        val highlights = db.highlightDao().getAllHighlights().first()
        val highlightsArray = JSONArray()
        highlights.forEach { hl ->
            highlightsArray.put(JSONObject().apply {
                put("id", hl.id)
                put("bookId", hl.bookId)
                put("chapterIndex", hl.chapterIndex)
                put("pageIndex", hl.pageIndex)
                put("textSnippet", hl.textSnippet)
                put("note", hl.note)
                put("colorHex", hl.colorHex)
                put("timestamp", hl.timestamp)
            })
        }
        root.put("highlights", highlightsArray)

        // 6. Audio Playlists
        val playlists = db.audioPlaylistDao().getAllPlaylists().first()
        val playlistsArray = JSONArray()
        playlists.forEach { pl ->
            playlistsArray.put(JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("description", pl.description)
                put("coverPath", pl.coverPath)
                put("createdTimestamp", pl.createdTimestamp)
                put("trackCount", pl.trackCount)
            })
        }
        root.put("audioPlaylists", playlistsArray)

        // 7. Audio Tracks
        val tracks = db.audioTrackDao().getAllTracks().first()
        val tracksArray = JSONArray()
        tracks.forEach { tr ->
            tracksArray.put(JSONObject().apply {
                put("id", tr.id)
                put("title", tr.title)
                put("artist", tr.artist)
                put("album", tr.album)
                put("durationMs", tr.durationMs)
                put("filePath", tr.filePath)
                put("coverUri", tr.coverUri)
                put("playlistId", tr.playlistId)
                put("trackNumber", tr.trackNumber)
                put("lastPositionMs", tr.lastPositionMs)
                put("lastPlayedTimestamp", tr.lastPlayedTimestamp)
                put("fileType", tr.fileType)
            })
        }
        root.put("audioTracks", tracksArray)

        // 8. Audio Bookmarks
        val audioBookmarks = db.audioBookmarkDao().getAllBookmarks().first()
        val audioBookmarksArray = JSONArray()
        audioBookmarks.forEach { ab ->
            audioBookmarksArray.put(JSONObject().apply {
                put("id", ab.id)
                put("trackId", ab.trackId)
                put("positionMs", ab.positionMs)
                put("note", ab.note)
                put("timestamp", ab.timestamp)
            })
        }
        root.put("audioBookmarks", audioBookmarksArray)

        root.toString(2)
    }

    suspend fun restoreFullBackupJson(context: Context, db: AppDatabase, jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            // Restore SharedPreferences
            if (root.has("preferences")) {
                val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
                val prefObj = root.getJSONObject("preferences")
                prefs.edit().apply {
                    if (prefObj.has("wake_lock_reading")) putBoolean("wake_lock_reading", prefObj.getBoolean("wake_lock_reading"))
                    if (prefObj.has("auto_detect_dir_startup")) putBoolean("auto_detect_dir_startup", prefObj.getBoolean("auto_detect_dir_startup"))
                    if (prefObj.has("background_audio_playback")) putBoolean("background_audio_playback", prefObj.getBoolean("background_audio_playback"))
                    if (prefObj.has("auto_resume_audio_position")) putBoolean("auto_resume_audio_position", prefObj.getBoolean("auto_resume_audio_position"))
                    if (prefObj.has("skip_interval_seconds")) putInt("skip_interval_seconds", prefObj.getInt("skip_interval_seconds"))
                    if (prefObj.has("reader_theme_mode") && !prefObj.isNull("reader_theme_mode")) {
                        putString("reader_theme_mode", prefObj.getString("reader_theme_mode"))
                    }
                    apply()
                }
            }

            // Restore Scan Folders
            if (root.has("scan_folders")) {
                val folderArray = root.getJSONArray("scan_folders")
                val set = mutableSetOf<String>()
                for (i in 0 until folderArray.length()) {
                    set.add(folderArray.getString(i))
                }
                val folderPrefs = context.getSharedPreferences(PREFS_FOLDERS, Context.MODE_PRIVATE)
                folderPrefs.edit().putStringSet(KEY_FOLDERS_SET, set).apply()
            }

            // Restore Books
            if (root.has("books")) {
                val array = root.getJSONArray("books")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val book = BookEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        author = obj.optString("author", "Unknown Author"),
                        fileType = obj.optString("fileType", "EPUB"),
                        filePath = obj.getString("filePath"),
                        isAsset = obj.optBoolean("isAsset", false),
                        coverPath = obj.optString("coverPath", null),
                        coverColorHex = obj.optString("coverColorHex", "#3F51B5"),
                        lastReadChapter = obj.optInt("lastReadChapter", 0),
                        lastReadPage = obj.optInt("lastReadPage", 0),
                        totalChapters = obj.optInt("totalChapters", 1),
                        totalPages = obj.optInt("totalPages", 1),
                        progressPercentage = obj.optDouble("progressPercentage", 0.0).toFloat(),
                        lastReadTimestamp = obj.optLong("lastReadTimestamp", System.currentTimeMillis()),
                        addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        description = obj.optString("description", "")
                    )
                    db.bookDao().insertBook(book)
                }
            }

            // Restore Reading Positions
            if (root.has("readingPositions")) {
                val array = root.getJSONArray("readingPositions")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pos = ReadingPositionEntity(
                        documentKey = obj.getString("documentKey"),
                        bookId = obj.getLong("bookId"),
                        chapterIndex = obj.optInt("chapterIndex", 0),
                        pageIndex = obj.optInt("pageIndex", 0),
                        progressPercentage = obj.optDouble("progressPercentage", 0.0).toFloat(),
                        lastReadTimestamp = obj.optLong("lastReadTimestamp", System.currentTimeMillis())
                    )
                    db.readingPositionDao().savePosition(pos)
                }
            }

            // Restore Bookmarks
            if (root.has("bookmarks")) {
                val array = root.getJSONArray("bookmarks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val bm = BookmarkEntity(
                        id = obj.optLong("id", 0L),
                        bookId = obj.getLong("bookId"),
                        chapterIndex = obj.optInt("chapterIndex", 0),
                        pageIndex = obj.optInt("pageIndex", 0),
                        title = obj.getString("title"),
                        previewText = obj.optString("previewText", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    db.bookmarkDao().insertBookmark(bm)
                }
            }

            // Restore Highlights
            if (root.has("highlights")) {
                val array = root.getJSONArray("highlights")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val hl = HighlightEntity(
                        id = obj.optLong("id", 0L),
                        bookId = obj.getLong("bookId"),
                        chapterIndex = obj.optInt("chapterIndex", 0),
                        pageIndex = obj.optInt("pageIndex", 0),
                        textSnippet = obj.optString("textSnippet", ""),
                        note = obj.optString("note", ""),
                        colorHex = obj.optString("colorHex", "#FFEB3B"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    db.highlightDao().insertHighlight(hl)
                }
            }

            // Restore Audio Playlists
            if (root.has("audioPlaylists")) {
                val array = root.getJSONArray("audioPlaylists")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val pl = AudioPlaylistEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        description = obj.optString("description", null),
                        coverPath = obj.optString("coverPath", null),
                        createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis()),
                        trackCount = obj.optInt("trackCount", 0)
                    )
                    db.audioPlaylistDao().insertPlaylist(pl)
                }
            }

            // Restore Audio Tracks
            if (root.has("audioTracks")) {
                val array = root.getJSONArray("audioTracks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tr = AudioTrackEntity(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        artist = obj.optString("artist", "Unknown Author"),
                        album = obj.optString("album", "Standalone Audio"),
                        durationMs = obj.optLong("durationMs", 0L),
                        filePath = obj.getString("filePath"),
                        coverUri = obj.optString("coverUri", null),
                        playlistId = obj.optLong("playlistId", 0L),
                        trackNumber = obj.optInt("trackNumber", 1),
                        lastPositionMs = obj.optLong("lastPositionMs", 0L),
                        lastPlayedTimestamp = obj.optLong("lastPlayedTimestamp", System.currentTimeMillis()),
                        fileType = obj.optString("fileType", "MP3")
                    )
                    db.audioTrackDao().insertTrack(tr)
                }
            }

            // Restore Audio Bookmarks
            if (root.has("audioBookmarks")) {
                val array = root.getJSONArray("audioBookmarks")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val ab = AudioBookmarkEntity(
                        id = obj.optLong("id", 0L),
                        trackId = obj.getLong("trackId"),
                        positionMs = obj.optLong("positionMs", 0L),
                        note = obj.getString("note"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    db.audioBookmarkDao().insertBookmark(ab)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getAppCacheSize(context: Context): Long {
        var size = 0L
        try {
            val cacheDir = context.cacheDir
            size += getDirectorySize(cacheDir)
            val coversDir = java.io.File(context.filesDir, "covers")
            if (coversDir.exists()) size += getDirectorySize(coversDir)
            val audioCoversDir = java.io.File(context.filesDir, "audio_covers")
            if (audioCoversDir.exists()) size += getDirectorySize(audioCoversDir)
        } catch (e: Exception) {
            // ignore
        }
        return size
    }

    fun clearAppCache(context: Context): Boolean {
        return try {
            val cacheDir = context.cacheDir
            deleteDirContents(cacheDir)
            val coversDir = java.io.File(context.filesDir, "covers")
            if (coversDir.exists()) deleteDirContents(coversDir)
            val audioCoversDir = java.io.File(context.filesDir, "audio_covers")
            if (audioCoversDir.exists()) deleteDirContents(audioCoversDir)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getDirectorySize(dir: java.io.File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirectorySize(f) else f.length()
        }
        return size
    }

    private fun deleteDirContents(dir: java.io.File) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) deleteDirContents(f)
            f.delete()
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
