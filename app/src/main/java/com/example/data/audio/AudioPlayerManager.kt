package com.example.data.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import com.example.data.local.AudioTrackEntity
import com.example.data.repository.AudioRepository
import com.example.data.repository.ReadingStatsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode {
    NONE, ALL, ONE
}

class AudioPlayerManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AudioPlayerManager"

        @Volatile
        private var instance: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null
    private var repository: AudioRepository? = null
    private var statsRepository: ReadingStatsRepository? = null

    fun setStatsRepository(statsRepo: ReadingStatsRepository) {
        this.statsRepository = statsRepo
    }

    // State Flows
    private val _currentTrack = MutableStateFlow<AudioTrackEntity?>(null)
    val currentTrack: StateFlow<AudioTrackEntity?> = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<AudioTrackEntity>>(emptyList())
    val playlist: StateFlow<List<AudioTrackEntity>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerRemainingSec = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSec: StateFlow<Int?> = _sleepTimerRemainingSec.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private var sleepTimer: CountDownTimer? = null

    fun setRepository(repo: AudioRepository) {
        this.repository = repo
    }

    fun playTrack(track: AudioTrackEntity, newPlaylist: List<AudioTrackEntity> = emptyList(), index: Int = 0) {
        if (newPlaylist.isNotEmpty()) {
            _playlist.value = newPlaylist
            _currentIndex.value = index.coerceIn(0, newPlaylist.size - 1)
        } else if (_playlist.value.isEmpty()) {
            _playlist.value = listOf(track)
            _currentIndex.value = 0
        }

        _currentTrack.value = track

        startService()

        try {
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                if (track.filePath.startsWith("content://") || track.filePath.startsWith("file://")) {
                    setDataSource(context, Uri.parse(track.filePath))
                } else {
                    setDataSource(track.filePath)
                }

                setOnPreparedListener { mp ->
                    _durationMs.value = mp.duration.toLong()
                    applyPlaybackSpeed(_playbackSpeed.value)

                    // Resume position if saved
                    if (track.lastPositionMs > 0L && track.lastPositionMs < mp.duration - 2000) {
                        mp.seekTo(track.lastPositionMs.toInt())
                        _currentPositionMs.value = track.lastPositionMs
                    } else {
                        _currentPositionMs.value = 0L
                    }

                    mp.start()
                    _isPlaying.value = true
                    startPositionUpdater()
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    stopPositionUpdater()
                    handlePlaybackCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    _isPlaying.value = false
                    stopPositionUpdater()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio player", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            saveCurrentPosition()
            stopPositionUpdater()
        } else {
            player.start()
            _isPlaying.value = true
            startPositionUpdater()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            saveCurrentPosition()
            stopPositionUpdater()
        }
    }

    fun resume() {
        val player = mediaPlayer ?: return
        if (!player.isPlaying) {
            player.start()
            _isPlaying.value = true
            startPositionUpdater()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        try {
            val clamped = positionMs.coerceIn(0L, _durationMs.value)
            player.seekTo(clamped.toInt())
            _currentPositionMs.value = clamped
            saveCurrentPosition()
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun skipForward(seconds: Int = 10) {
        val newPos = _currentPositionMs.value + (seconds * 1000)
        seekTo(newPos)
    }

    fun skipBackward(seconds: Int = 10) {
        val newPos = _currentPositionMs.value - (seconds * 1000)
        seekTo(newPos)
    }

    fun playNext() {
        val list = _playlist.value
        if (list.isEmpty()) return

        var nextIdx = _currentIndex.value + 1
        if (_isShuffle.value) {
            nextIdx = (0 until list.size).random()
        } else if (nextIdx >= list.size) {
            if (_repeatMode.value == RepeatMode.ALL) {
                nextIdx = 0
            } else {
                return
            }
        }

        _currentIndex.value = nextIdx
        playTrack(list[nextIdx])
    }

    fun playPrevious() {
        val list = _playlist.value
        if (list.isEmpty()) return

        if (_currentPositionMs.value > 3000L) {
            seekTo(0)
            return
        }

        var prevIdx = _currentIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = if (_repeatMode.value == RepeatMode.ALL) list.size - 1 else 0
        }

        _currentIndex.value = prevIdx
        playTrack(list[prevIdx])
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed(speed)
    }

    private fun applyPlaybackSpeed(speed: Float) {
        val player = mediaPlayer ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            } catch (e: Exception) {
                Log.w(TAG, "Could not set playback speed", e)
            }
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun setSleepTimerMinutes(minutes: Int) {
        cancelSleepTimer()
        if (minutes <= 0) return

        val totalMs = minutes * 60 * 1000L
        _sleepTimerRemainingSec.value = (totalMs / 1000).toInt()

        sleepTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _sleepTimerRemainingSec.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _sleepTimerRemainingSec.value = null
                pause()
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        _sleepTimerRemainingSec.value = null
    }

    private fun handlePlaybackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentTrack.value?.let { playTrack(it) }
            }
            RepeatMode.ALL, RepeatMode.NONE -> {
                if (_currentIndex.value < _playlist.value.size - 1 || _repeatMode.value == RepeatMode.ALL) {
                    playNext()
                } else {
                    saveCurrentPosition()
                }
            }
        }
    }

    private fun startPositionUpdater() {
        stopPositionUpdater()
        var accumulatedHalfSecs = 0
        positionUpdateJob = scope.launch {
            while (coroutineContext.isActive) {
                try {
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            _currentPositionMs.value = mp.currentPosition.toLong()
                            accumulatedHalfSecs++
                            if (accumulatedHalfSecs >= 2) {
                                statsRepository?.addAudioListeningTime(1L)
                                accumulatedHalfSecs = 0
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient exceptions during state transition
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdater() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun saveCurrentPosition() {
        val track = _currentTrack.value ?: return
        val pos = _currentPositionMs.value
        scope.launch(Dispatchers.IO) {
            repository?.updateTrackPosition(track.id, pos)
        }
    }

    private fun startService() {
        try {
            val serviceIntent = Intent(context, AudioPlayerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start audio foreground service", e)
        }
    }

    private fun releaseMediaPlayer() {
        stopPositionUpdater()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaPlayer", e)
            }
        }
        mediaPlayer = null
    }

    fun stop() {
        saveCurrentPosition()
        releaseMediaPlayer()
        _isPlaying.value = false
        _currentTrack.value = null
        cancelSleepTimer()
    }
}
