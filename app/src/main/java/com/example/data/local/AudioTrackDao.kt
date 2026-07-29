package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioTrackDao {
    @Query("SELECT * FROM audio_tracks ORDER BY lastPlayedTimestamp DESC, title ASC")
    fun getAllTracks(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE playlistId = :playlistId ORDER BY trackNumber ASC, title ASC")
    fun getTracksByPlaylist(playlistId: Long): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE playlistId = :playlistId ORDER BY trackNumber ASC, title ASC")
    suspend fun getTracksByPlaylistSync(playlistId: Long): List<AudioTrackEntity>

    @Query("SELECT * FROM audio_tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): AudioTrackEntity?

    @Query("SELECT * FROM audio_tracks WHERE id = :id")
    fun observeTrackById(id: Long): Flow<AudioTrackEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: AudioTrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<AudioTrackEntity>)

    @Update
    suspend fun updateTrack(track: AudioTrackEntity)

    @Query("UPDATE audio_tracks SET lastPositionMs = :positionMs, lastPlayedTimestamp = :timestamp WHERE id = :trackId")
    suspend fun updatePosition(trackId: Long, positionMs: Long, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteTrack(track: AudioTrackEntity)

    @Query("DELETE FROM audio_tracks WHERE playlistId = :playlistId")
    suspend fun deleteTracksByPlaylist(playlistId: Long)
}
