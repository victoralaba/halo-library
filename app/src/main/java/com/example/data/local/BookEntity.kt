package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val fileType: String, // "EPUB" or "PDF"
    val filePath: String, // Local URI or internal asset path
    val isAsset: Boolean = false,
    val coverPath: String? = null,
    val coverColorHex: String = "#FF6B6B",
    val lastReadChapter: Int = 0,
    val lastReadPage: Int = 0,
    val totalChapters: Int = 1,
    val totalPages: Int = 1,
    val progressPercentage: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val addedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val description: String = ""
)
