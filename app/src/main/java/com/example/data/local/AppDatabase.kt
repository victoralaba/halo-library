package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        ReadingPositionEntity::class,
        AudioTrackEntity::class,
        AudioPlaylistEntity::class,
        AudioBookmarkEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingPositionDao(): ReadingPositionDao
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun audioPlaylistDao(): AudioPlaylistDao
    abstract fun audioBookmarkDao(): AudioBookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lumina_reader_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
