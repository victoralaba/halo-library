package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioPlaylistDao {
    @Query("SELECT * FROM audio_playlists ORDER BY createdTimestamp DESC")
    fun getAllPlaylists(): Flow<List<AudioPlaylistEntity>>

    @Query("SELECT * FROM audio_playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): AudioPlaylistEntity?

    @Query("SELECT * FROM audio_playlists WHERE id = :id")
    fun observePlaylistById(id: Long): Flow<AudioPlaylistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: AudioPlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: AudioPlaylistEntity)

    @Query("UPDATE audio_playlists SET trackCount = (SELECT COUNT(*) FROM audio_tracks WHERE playlistId = :playlistId) WHERE id = :playlistId")
    suspend fun updateTrackCount(playlistId: Long)

    @Delete
    suspend fun deletePlaylist(playlist: AudioPlaylistEntity)
}
