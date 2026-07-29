package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY chapterIndex ASC, pageIndex ASC, timestamp DESC")
    fun getHighlightsForBook(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY timestamp DESC")
    fun getAllHighlights(): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: Long)
}
