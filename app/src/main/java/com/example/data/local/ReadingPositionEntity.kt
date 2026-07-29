package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_positions")
data class ReadingPositionEntity(
    @PrimaryKey val documentKey: String, // book.filePath or assetName or unique document URI
    val bookId: Long,
    val chapterIndex: Int = 0,
    val pageIndex: Int = 0,
    val progressPercentage: Float = 0f,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)
