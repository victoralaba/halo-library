package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioBookmarkDao {
    @Query("SELECT * FROM audio_bookmarks WHERE trackId = :trackId ORDER BY positionMs ASC")
    fun getBookmarksForTrack(trackId: Long): Flow<List<AudioBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: AudioBookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: AudioBookmarkEntity)
}
