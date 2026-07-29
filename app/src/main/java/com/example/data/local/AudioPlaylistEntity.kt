package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_playlists")
data class AudioPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val coverPath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val trackCount: Int = 0
)
