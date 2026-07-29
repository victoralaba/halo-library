package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_bookmarks")
data class AudioBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val positionMs: Long,
    val note: String,
    val timestamp: Long = System.currentTimeMillis()
)
