package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val pageIndex: Int,
    val textSnippet: String,
    val note: String = "",
    val colorHex: String = "#FFEB3B", // Amber default
    val timestamp: Long = System.currentTimeMillis()
)
