package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastReadTimestamp DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBookById(id: Long): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET lastReadChapter = :chapter, lastReadPage = :page, progressPercentage = :progress, lastReadTimestamp = :timestamp WHERE id = :bookId")
    suspend fun updateProgress(bookId: Long, chapter: Int, page: Int, progress: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun toggleFavorite(bookId: Long, isFavorite: Boolean)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)
}
