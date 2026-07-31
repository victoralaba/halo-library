package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.BookDao
import com.example.data.local.AudioTrackDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyReadingStat(
    val dayName: String,         // e.g. "Mon", "Tue", "Wed"
    val dateKey: String,         // e.g. "2026-07-30"
    val ebookMinutes: Float,     // Ebook reading time in minutes
    val audioMinutes: Float,     // Audio listening time in minutes
    val isToday: Boolean = false
)

data class ReadingStatsSummary(
    val totalEbooksRead: Int = 0,
    val totalEbooksInProgress: Int = 0,
    val totalEbooksInLibrary: Int = 0,
    val totalEbookTimeMinutes: Long = 0, // In minutes
    
    val totalAudioFinished: Int = 0,
    val totalAudioInProgress: Int = 0,
    val totalAudioInLibrary: Int = 0,
    val totalAudioTimeMinutes: Long = 0, // In minutes

    val weeklyStats: List<DailyReadingStat> = emptyList(),
    val currentStreakDays: Int = 1,
    val averageDailyMinutes: Int = 0
)

class ReadingStatsRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val audioTrackDao: AudioTrackDao
) {
    companion object {
        private const val PREFS_NAME = "lumina_reading_stats_prefs"
        private const val KEY_TOTAL_EBOOK_SEC = "total_ebook_seconds"
        private const val KEY_TOTAL_AUDIO_SEC = "total_audio_seconds"
        private const val PREFIX_EBOOK_DAY = "ebook_sec_" // + YYYYMMDD
        private const val PREFIX_AUDIO_DAY = "audio_sec_" // + YYYYMMDD
        
        @Volatile
        private var instance: ReadingStatsRepository? = null

        fun getInstance(context: Context, bookDao: BookDao, audioTrackDao: AudioTrackDao): ReadingStatsRepository {
            return instance ?: synchronized(this) {
                instance ?: ReadingStatsRepository(context.applicationContext, bookDao, audioTrackDao).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val dayNameFormat = SimpleDateFormat("EEE", Locale.US)
    private val dateDisplayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _statsUpdateTrigger = MutableStateFlow(System.currentTimeMillis())

    private fun getTodayKey(): String = dateFormat.format(Date())

    fun addEbookReadingTime(seconds: Long) {
        if (seconds <= 0) return
        val today = getTodayKey()
        val currentTotal = prefs.getLong(KEY_TOTAL_EBOOK_SEC, 0L)
        val currentDay = prefs.getLong(PREFIX_EBOOK_DAY + today, 0L)
        
        prefs.edit()
            .putLong(KEY_TOTAL_EBOOK_SEC, currentTotal + seconds)
            .putLong(PREFIX_EBOOK_DAY + today, currentDay + seconds)
            .apply()
        
        _statsUpdateTrigger.value = System.currentTimeMillis()
    }

    fun addAudioListeningTime(seconds: Long) {
        if (seconds <= 0) return
        val today = getTodayKey()
        val currentTotal = prefs.getLong(KEY_TOTAL_AUDIO_SEC, 0L)
        val currentDay = prefs.getLong(PREFIX_AUDIO_DAY + today, 0L)

        prefs.edit()
            .putLong(KEY_TOTAL_AUDIO_SEC, currentTotal + seconds)
            .putLong(PREFIX_AUDIO_DAY + today, currentDay + seconds)
            .apply()

        _statsUpdateTrigger.value = System.currentTimeMillis()
    }

    fun getEbookDaySeconds(dateKey: String): Long {
        return prefs.getLong(PREFIX_EBOOK_DAY + dateKey, 0L)
    }

    fun getAudioDaySeconds(dateKey: String): Long {
        return prefs.getLong(PREFIX_AUDIO_DAY + dateKey, 0L)
    }

    fun getTotalEbookSeconds(): Long {
        return prefs.getLong(KEY_TOTAL_EBOOK_SEC, 0L)
    }

    fun getTotalAudioSeconds(): Long {
        return prefs.getLong(KEY_TOTAL_AUDIO_SEC, 0L)
    }

    fun observeStats(): Flow<ReadingStatsSummary> {
        return combine(
            bookDao.getAllBooks(),
            audioTrackDao.getAllTracks(),
            _statsUpdateTrigger
        ) { books, audioTracks, _ ->
            val totalEbooks = books.size
            val ebooksRead = books.count { it.progressPercentage >= 95f }
            val ebooksInProgress = books.count { it.progressPercentage in 1f..94.9f }

            val totalAudio = audioTracks.size
            val audioFinished = audioTracks.count { track ->
                track.durationMs > 0 && (track.lastPositionMs >= track.durationMs * 0.90f)
            }
            val audioInProgress = audioTracks.count { track ->
                track.lastPositionMs > 0 && (track.durationMs <= 0 || track.lastPositionMs < track.durationMs * 0.90f)
            }

            // Calculate weekly stats for past 7 days (ending today)
            val cal = Calendar.getInstance()
            val todayStr = dateFormat.format(cal.time)
            val weeklyList = mutableListOf<DailyReadingStat>()

            // Go back 6 days to get 7 days total (Mon-Sun or past 7 days)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            for (i in 0 until 7) {
                val dKey = dateFormat.format(cal.time)
                val dayLabel = dayNameFormat.format(cal.time)
                val dateLabel = dateDisplayFormat.format(cal.time)

                val ebookSec = getEbookDaySeconds(dKey)
                val audioSec = getAudioDaySeconds(dKey)

                weeklyList.add(
                    DailyReadingStat(
                        dayName = dayLabel,
                        dateKey = dateLabel,
                        ebookMinutes = (ebookSec / 60f),
                        audioMinutes = (audioSec / 60f),
                        isToday = (dKey == todayStr)
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val totalEbookMin = getTotalEbookSeconds() / 60
            val totalAudioMin = getTotalAudioSeconds() / 60

            // Calculate streak (consecutive days with at least 1 min reading or listening)
            var streak = 0
            val streakCal = Calendar.getInstance()
            for (i in 0 until 30) {
                val sKey = dateFormat.format(streakCal.time)
                val eSec = getEbookDaySeconds(sKey)
                val aSec = getAudioDaySeconds(sKey)
                if (eSec > 0 || aSec > 0) {
                    streak++
                    streakCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    if (i == 0) {
                        // Check if yesterday had stats
                        streakCal.add(Calendar.DAY_OF_YEAR, -1)
                        continue
                    }
                    break
                }
            }

            val totalWeeklyMin = weeklyList.sumOf { (it.ebookMinutes + it.audioMinutes).toDouble() }
            val avgMin = (totalWeeklyMin / 7.0).toInt()

            ReadingStatsSummary(
                totalEbooksRead = ebooksRead,
                totalEbooksInProgress = ebooksInProgress,
                totalEbooksInLibrary = totalEbooks,
                totalEbookTimeMinutes = totalEbookMin,
                totalAudioFinished = audioFinished,
                totalAudioInProgress = audioInProgress,
                totalAudioInLibrary = totalAudio,
                totalAudioTimeMinutes = totalAudioMin,
                weeklyStats = weeklyList,
                currentStreakDays = streak.coerceAtLeast(1),
                averageDailyMinutes = avgMin
            )
        }
    }

    fun resetAllStats() {
        prefs.edit().clear().apply()
        _statsUpdateTrigger.value = System.currentTimeMillis()
    }
}
