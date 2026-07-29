package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String = "Unknown Author",
    val album: String = "Standalone Audio",
    val durationMs: Long = 0L,
    val filePath: String,
    val coverUri: String? = null,
    val playlistId: Long = 0L,
    val trackNumber: Int = 1,
    val lastPositionMs: Long = 0L,
    val lastPlayedTimestamp: Long = 0L,
    val fileType: String = "MP3"
)
