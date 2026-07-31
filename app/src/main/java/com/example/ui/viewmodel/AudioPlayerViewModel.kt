package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlayerManager
import com.example.data.local.AppDatabase
import com.example.data.local.AudioBookmarkEntity
import com.example.data.local.AudioPlaylistEntity
import com.example.data.local.AudioTrackEntity
import com.example.data.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AudioPlayerUiState(
    val selectedPlaylist: AudioPlaylistEntity? = null,
    val searchQuery: String = "",
    val statusMessage: String? = null,
    val isLoading: Boolean = false,
    val isExpandedPlayerVisible: Boolean = false
)

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AudioRepository(
        context = application,
        audioTrackDao = db.audioTrackDao(),
        audioPlaylistDao = db.audioPlaylistDao(),
        audioBookmarkDao = db.audioBookmarkDao()
    )
    val playerManager = AudioPlayerManager.getInstance(application).apply {
        setRepository(repository)
    }

    private val _uiState = MutableStateFlow(AudioPlayerUiState())
    val uiState: StateFlow<AudioPlayerUiState> = _uiState.asStateFlow()

    val allTracks: StateFlow<List<AudioTrackEntity>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<AudioPlaylistEntity>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val currentPositionMs = playerManager.currentPositionMs
    val durationMs = playerManager.durationMs
    val playbackSpeed = playerManager.playbackSpeed
    val sleepTimerRemainingSec = playerManager.sleepTimerRemainingSec
    val playlist = playerManager.playlist
    val repeatMode = playerManager.repeatMode
    val isShuffle = playerManager.isShuffle

    // Selected playlist tracks
    val selectedPlaylistTracks: StateFlow<List<AudioTrackEntity>> = _uiState
        .flatMapLatest { state ->
            if (state.selectedPlaylist != null) {
                repository.getTracksForPlaylist(state.selectedPlaylist.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active track bookmarks
    val activeTrackBookmarks: StateFlow<List<AudioBookmarkEntity>> = currentTrack
        .flatMapLatest { track ->
            if (track != null) {
                repository.getBookmarksForTrack(track.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPlaylist(playlist: AudioPlaylistEntity?) {
        _uiState.update { it.copy(selectedPlaylist = playlist) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setExpandedPlayerVisible(visible: Boolean) {
        _uiState.update { it.copy(isExpandedPlayerVisible = visible) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun importAudioFiles(uris: List<Uri>, playlistId: Long = 0L) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            var count = 0
            for (uri in uris) {
                try {
                    repository.importAudioUri(uri, playlistId)
                    count++
                } catch (e: Exception) {
                    // skip failed
                }
            }
            val targetPlaylist = if (playlistId > 0) repository.allPlaylists.first().find { it.id == playlistId }?.name else null
            val msg = if (targetPlaylist != null) {
                "Imported $count audio track(s) into '$targetPlaylist'"
            } else {
                "Imported $count audio track(s)"
            }
            _uiState.update { it.copy(isLoading = false, statusMessage = msg) }
        }
    }

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createPlaylist(name, description)
            _uiState.update { it.copy(statusMessage = "Created playlist '$name'") }
        }
    }

    fun addTrackToPlaylist(track: AudioTrackEntity, playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addTrackToPlaylist(track, playlistId)
            _uiState.update { it.copy(statusMessage = "Added '${track.title}' to playlist") }
        }
    }

    fun deletePlaylist(playlist: AudioPlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlist)
            if (_uiState.value.selectedPlaylist?.id == playlist.id) {
                _uiState.update { it.copy(selectedPlaylist = null) }
            }
            _uiState.update { it.copy(statusMessage = "Deleted playlist '${playlist.name}'") }
        }
    }

    fun deleteTrack(track: AudioTrackEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTrack(track)
            if (currentTrack.value?.id == track.id) {
                playerManager.stop()
            }
            _uiState.update { it.copy(statusMessage = "Deleted '${track.title}'") }
        }
    }

    fun playTrack(track: AudioTrackEntity, playlist: List<AudioTrackEntity> = emptyList()) {
        val targetPlaylist = if (playlist.isNotEmpty()) playlist else allTracks.value
        val index = targetPlaylist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playerManager.playTrack(track, targetPlaylist, index)
    }

    fun playPlaylist(playlist: AudioPlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val tracks = repository.getTracksForPlaylistSync(playlist.id)
            if (tracks.isNotEmpty()) {
                playerManager.playTrack(tracks.first(), tracks, 0)
                _uiState.update { it.copy(isExpandedPlayerVisible = true) }
            } else {
                _uiState.update { it.copy(statusMessage = "Playlist is empty. Import audio tracks first.") }
            }
        }
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun pause() = playerManager.pause()
    fun playNext() = playerManager.playNext()
    fun playPrevious() = playerManager.playPrevious()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    private val prefs = application.getSharedPreferences("lumina_app_settings_prefs", android.content.Context.MODE_PRIVATE)

    fun skipForward(seconds: Int? = null) {
        val sec = seconds ?: prefs.getInt("skip_interval_seconds", 10)
        playerManager.skipForward(sec)
    }

    fun skipBackward(seconds: Int? = null) {
        val sec = seconds ?: prefs.getInt("skip_interval_seconds", 10)
        playerManager.skipBackward(sec)
    }
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun toggleRepeatMode() = playerManager.toggleRepeatMode()
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun setSleepTimerMinutes(minutes: Int) = playerManager.setSleepTimerMinutes(minutes)
    fun cancelSleepTimer() = playerManager.cancelSleepTimer()

    fun addBookmark(note: String) {
        val track = currentTrack.value ?: return
        val pos = currentPositionMs.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBookmark(track.id, pos, note)
            _uiState.update { it.copy(statusMessage = "Bookmark added at ${formatDuration(pos)}") }
        }
    }

    fun deleteBookmark(bookmark: AudioBookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBookmark(bookmark)
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}
