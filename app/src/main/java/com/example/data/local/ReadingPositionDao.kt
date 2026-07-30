package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingPositionDao {
    @Query("SELECT * FROM reading_positions WHERE documentKey = :key")
    suspend fun getPositionByKey(key: String): ReadingPositionEntity?

    @Query("SELECT * FROM reading_positions WHERE bookId = :bookId")
    suspend fun getPositionByBookId(bookId: Long): ReadingPositionEntity?

    @Query("SELECT * FROM reading_positions")
    fun getAllPositions(): Flow<List<ReadingPositionEntity>>

    @Query("SELECT * FROM reading_positions WHERE documentKey = :key")
    fun observePositionByKey(key: String): Flow<ReadingPositionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePosition(position: ReadingPositionEntity)

    @Query("DELETE FROM reading_positions WHERE documentKey = :key")
    suspend fun deletePositionByKey(key: String)
}
