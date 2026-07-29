package com.example.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AudioRepository(
    private val context: Context,
    private val audioTrackDao: AudioTrackDao,
    private val audioPlaylistDao: AudioPlaylistDao,
    private val audioBookmarkDao: AudioBookmarkDao
) {
    companion object {
        private const val TAG = "AudioRepository"
    }

    val allTracks: Flow<List<AudioTrackEntity>> = audioTrackDao.getAllTracks()
    val allPlaylists: Flow<List<AudioPlaylistEntity>> = audioPlaylistDao.getAllPlaylists()

    fun getTracksForPlaylist(playlistId: Long): Flow<List<AudioTrackEntity>> =
        audioTrackDao.getTracksByPlaylist(playlistId)

    suspend fun getTracksForPlaylistSync(playlistId: Long): List<AudioTrackEntity> =
        audioTrackDao.getTracksByPlaylistSync(playlistId)

    fun getBookmarksForTrack(trackId: Long): Flow<List<AudioBookmarkEntity>> =
        audioBookmarkDao.getBookmarksForTrack(trackId)

    suspend fun getTrackById(id: Long): AudioTrackEntity? = audioTrackDao.getTrackById(id)

    suspend fun createPlaylist(name: String, description: String? = null): Long = withContext(Dispatchers.IO) {
        val playlist = AudioPlaylistEntity(
            name = name.ifBlank { "New Audio Collection" },
            description = description,
            createdTimestamp = System.currentTimeMillis()
        )
        audioPlaylistDao.insertPlaylist(playlist)
    }

    suspend fun deletePlaylist(playlist: AudioPlaylistEntity) = withContext(Dispatchers.IO) {
        audioTrackDao.deleteTracksByPlaylist(playlist.id)
        audioPlaylistDao.deletePlaylist(playlist)
    }

    suspend fun importAudioUri(uri: Uri, playlistId: Long = 0L): Long = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs = 0L
        var coverPath: String? = null

        val fileName = getFileNameFromUri(uri) ?: "Audio_Track"
        val extension = fileName.substringAfterLast(".").uppercase()

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!durStr.isNullOrBlank()) {
                durationMs = durStr.toLongOrNull() ?: 0L
            }

            val embeddedPicture = retriever.embeddedPicture
            if (embeddedPicture != null && embeddedPicture.isNotEmpty()) {
                val coversDir = File(context.filesDir, "audio_covers").apply { mkdirs() }
                val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
                val bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.size)
                if (bitmap != null) {
                    FileOutputStream(coverFile).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    coverPath = coverFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed metadata extraction for $uri", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }

        val finalTitle = title?.ifBlank { null } ?: fileName.substringBeforeLast(".").replace("_", " ")
        val finalArtist = artist?.ifBlank { null } ?: "Unknown Author"
        val finalAlbum = album?.ifBlank { null } ?: if (playlistId > 0) "Custom Playlist" else "Standalone Audio"

        val track = AudioTrackEntity(
            title = finalTitle,
            artist = finalArtist,
            album = finalAlbum,
            durationMs = durationMs,
            filePath = uri.toString(),
            coverUri = coverPath,
            playlistId = playlistId,
            fileType = extension.ifBlank { "AUDIO" },
            lastPlayedTimestamp = System.currentTimeMillis()
        )

        val insertedId = audioTrackDao.insertTrack(track)
        if (playlistId > 0) {
            audioPlaylistDao.updateTrackCount(playlistId)
        }
        insertedId
    }

    suspend fun updateTrackPosition(trackId: Long, positionMs: Long) = withContext(Dispatchers.IO) {
        audioTrackDao.updatePosition(trackId, positionMs, System.currentTimeMillis())
    }

    suspend fun addBookmark(trackId: Long, positionMs: Long, note: String) = withContext(Dispatchers.IO) {
        val bookmark = AudioBookmarkEntity(
            trackId = trackId,
            positionMs = positionMs,
            note = note.ifBlank { "Bookmark at ${formatDuration(positionMs)}" },
            timestamp = System.currentTimeMillis()
        )
        audioBookmarkDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: AudioBookmarkEntity) = withContext(Dispatchers.IO) {
        audioBookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun deleteTrack(track: AudioTrackEntity) = withContext(Dispatchers.IO) {
        audioTrackDao.deleteTrack(track)
        if (track.playlistId > 0) {
            audioPlaylistDao.updateTrackCount(track.playlistId)
        }
    }

    suspend fun addTrackToPlaylist(track: AudioTrackEntity, playlistId: Long) = withContext(Dispatchers.IO) {
        audioTrackDao.updateTrack(track.copy(playlistId = playlistId))
        audioPlaylistDao.updateTrackCount(playlistId)
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

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}
